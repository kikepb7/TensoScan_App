package com.tensoscan.data.scan.mlkit

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tensoscan.domain.error.DetectorError
import com.tensoscan.domain.scan.detector.DigitDetector
import com.tensoscan.domain.scan.model.DetectorInfo
import com.tensoscan.domain.scan.model.FrameImage
import com.tensoscan.domain.scan.model.SymbolDetection
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * [DigitDetector] backed by ML Kit text recognition with the Latin model bundled in the app, so it works offline from
 * the first launch. It is the first detector of the product and the fallback once the in-house model ships.
 *
 * [detect] is blocking and meant for the single analysis thread; the bitmap it reuses is confined to that thread.
 */
internal class MlKitDigitDetector @Inject constructor() : DigitDetector {

    private var recognizer: TextRecognizer? = null
    private var bitmap: Bitmap? = null

    override suspend fun prepare(): Result<DetectorInfo> = try {
        if (recognizer == null) recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        Result.success(INFO)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(DetectorError.RuntimeFailure(error))
    }

    override fun detect(frame: FrameImage): Result<List<SymbolDetection>> {
        val client = recognizer ?: return Result.failure(DetectorError.ModelMissing)
        return try {
            val image = InputImage.fromBitmap(frame.toBitmap(), 0)
            val text = Tasks.await(client.process(image), TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            val found = text.textBlocks
                .flatMap { it.lines }
                .flatMap { it.elements }
                .flatMap { it.symbols }
                .mapNotNull { symbol ->
                    val box = symbol.boundingBox ?: return@mapNotNull null
                    OcrSymbol(symbol.text, box.left, box.top, box.right, box.bottom, symbol.confidence)
                }
            Result.success(MlKitSymbolMapper.map(found, frame.width, frame.height))
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            Result.failure(DetectorError.RuntimeFailure(error))
        } catch (error: Exception) {
            // Timeouts, recogniser failures and unusable bitmaps all count as one failed frame.
            Result.failure(DetectorError.RuntimeFailure(error))
        }
    }

    override fun release() {
        recognizer?.close()
        recognizer = null
        bitmap?.recycle()
        bitmap = null
    }

    private fun FrameImage.toBitmap(): Bitmap {
        val target = bitmap?.takeIf { it.width == width && it.height == height }
            ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                bitmap?.recycle()
                bitmap = it
            }
        target.setPixels(argb, 0, width, 0, 0, width, height)
        return target
    }

    private companion object {
        const val TIMEOUT_MILLIS = 500L
        val INFO = DetectorInfo(id = "mlkit-text", version = "v2")
    }
}
