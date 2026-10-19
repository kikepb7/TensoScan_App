package com.tensoscan.data.scan.litert

import com.tensoscan.domain.error.DetectorError
import com.tensoscan.domain.scan.detector.DigitDetector
import com.tensoscan.domain.scan.detector.model.DetectionDecoder
import com.tensoscan.domain.scan.detector.model.DigitModelSpec
import com.tensoscan.domain.scan.detector.model.LetterboxPreprocessor
import com.tensoscan.domain.scan.model.DetectorInfo
import com.tensoscan.domain.scan.model.FrameImage
import com.tensoscan.domain.scan.model.SymbolDetection
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * [DigitDetector] backed by the in-house LiteRT model (`digit-detector-model.md`). The model's CPU path (XNNPACK, four
 * threads) fits the latency budget, so no GPU delegate is shipped: it would add size and failure modes for no gain.
 *
 * [detect] is blocking and confined to the analysis thread, which is the only user of the interpreter and the buffers.
 */
internal class LiteRtDigitDetector @Inject constructor(private val loader: ModelAssetLoader) : DigitDetector {

    private var runtime: Runtime? = null

    override suspend fun prepare(): Result<DetectorInfo> {
        runtime?.let { return Result.success(it.info) }
        val model = loader.load().getOrElse { return Result.failure(it) }
        return try {
            val created = Runtime.create(model)
            created.warmUp()
            runtime = created
            Result.success(created.info)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.failure(DetectorError.RuntimeFailure(error))
        }
    }

    override fun detect(frame: FrameImage): Result<List<SymbolDetection>> {
        val current = runtime ?: return Result.failure(DetectorError.ModelMissing)
        return try {
            Result.success(current.detect(frame))
        } catch (error: RuntimeException) {
            Result.failure(DetectorError.RuntimeFailure(error))
        }
    }

    override fun release() {
        runtime?.close()
        runtime = null
    }

    private class Runtime(
        private val interpreter: Interpreter,
        private val spec: DigitModelSpec,
    ) {
        val info = DetectorInfo(id = "digits", version = spec.version)

        private val preprocessor = LetterboxPreprocessor(spec.inputSize)
        private val decoder = DetectionDecoder(spec)
        private val floats = FloatArray(preprocessor.outputSize)
        private val input: ByteBuffer = ByteBuffer.allocateDirect(
            floats.size * Float.SIZE_BYTES,
        ).order(ByteOrder.nativeOrder())
        private val output = Array(1) { Array(spec.numCandidates) { FloatArray(spec.valuesPerCandidate) } }
        private val flat = FloatArray(spec.numCandidates * spec.valuesPerCandidate)

        fun detect(frame: FrameImage): List<SymbolDetection> {
            val transform = preprocessor.process(frame, floats)
            input.rewind()
            input.asFloatBuffer().put(floats, 0, preprocessor.outputSize)
            interpreter.run(input, output)
            var offset = 0
            for (row in output[0]) {
                row.copyInto(flat, offset)
                offset += row.size
            }
            return decoder.decode(flat, transform)
        }

        /** The first inference is slow (allocations); doing it now keeps it out of the first live frame. */
        fun warmUp() {
            val blank = FrameImage(WARM_UP_SIZE, WARM_UP_SIZE, IntArray(WARM_UP_SIZE * WARM_UP_SIZE), 0)
            detect(blank)
        }

        fun close() = interpreter.close()

        companion object {
            private const val THREADS = 4
            private const val WARM_UP_SIZE = 64

            fun create(model: LoadedModel): Runtime {
                val buffer = ByteBuffer.allocateDirect(model.modelBytes.size).order(ByteOrder.nativeOrder())
                buffer.put(model.modelBytes).rewind()
                val options = Interpreter.Options().setNumThreads(THREADS)
                return Runtime(Interpreter(buffer, options), model.spec)
            }
        }
    }
}
