package com.tensoscan.data.scan.litert

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tensoscan.domain.scan.model.FrameImage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures the latency of one `detect` call on a real device (RNF-A002-03: p95 <= 60 ms for detector plus parser; the
 * parser costs microseconds, so the detector alone is held to the same budget).
 *
 * It runs against the model bundled in the debug build. When there is none (the release channel ships ML Kit only) the
 * test is skipped instead of failing. Results are logged under the tag `LiteRtBenchmark` so they can be pasted into
 * `validation-report.md`.
 */
@RunWith(AndroidJUnit4::class)
class LiteRtDigitDetectorBenchmark {

    private val assets = object : ModelAssets {
        private val manager = InstrumentationRegistry.getInstrumentation().targetContext.assets

        override fun list(directory: String): List<String> = manager.list(directory)?.toList().orEmpty()

        override fun read(path: String): ByteArray = manager.open(path).use { it.readBytes() }
    }

    @Test
    fun detectLatencyStaysInsideTheBudget() {
        val sut = LiteRtDigitDetector(ModelAssetLoader(assets, Json { ignoreUnknownKeys = true }))
        val prepared = runBlocking { sut.prepare() }
        assumeTrue("No digit model bundled in this build", prepared.isSuccess)

        val frame = syntheticFrame()
        repeat(WARM_UP_RUNS) { sut.detect(frame) }
        val nanos = LongArray(MEASURED_RUNS) {
            val start = System.nanoTime()
            sut.detect(frame).getOrThrow()
            System.nanoTime() - start
        }.sorted()
        sut.release()

        val median = nanos[nanos.size / 2] / NANOS_PER_MILLI
        val p95 = nanos[(nanos.size * P95_RANK).toInt()] / NANOS_PER_MILLI
        Log.i(TAG, "model=${prepared.getOrThrow().version} median=${median}ms p95=${p95}ms runs=$MEASURED_RUNS")
        assertTrue("p95 latency ${p95}ms is over the ${BUDGET_MILLIS}ms budget", p95 <= BUDGET_MILLIS)
    }

    /** A gradient is enough: the cost of the network does not depend on what the pixels show. */
    private fun syntheticFrame(): FrameImage {
        val pixels = IntArray(WIDTH * HEIGHT) { index ->
            val shade = (index % WIDTH) * 255 / WIDTH
            0xFF000000.toInt() or (shade shl 16) or (shade shl 8) or shade
        }
        return FrameImage(WIDTH, HEIGHT, pixels, timestampMillis = 0)
    }

    private companion object {
        const val TAG = "LiteRtBenchmark"
        const val WIDTH = 640
        const val HEIGHT = 288
        const val WARM_UP_RUNS = 20
        const val MEASURED_RUNS = 200
        const val P95_RANK = 0.95
        const val NANOS_PER_MILLI = 1_000_000L
        const val BUDGET_MILLIS = 60L
    }
}
