package com.tensoscan.domain.scan.detector.model

import com.tensoscan.domain.scan.model.DisplaySymbol
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DetectionDecoderTest {

    private val classes = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "colon", "dash", "E", "r")

    private fun spec(candidates: Int = 4, maxDetections: Int = 32, unknown: Boolean = false) = DigitModelSpec(
        name = "digit-detector",
        version = "1.0.0",
        contractVersion = "1.0.0",
        inputSize = 320,
        numCandidates = candidates,
        classes = if (unknown) classes.dropLast(1) + "mystery" else classes,
        scoreThreshold = 0.40f,
        nmsIouThreshold = 0.45f,
        maxDetections = maxDetections,
        sha256 = "x",
    )

    /** A square frame fills the input, so the transform is the identity and boxes keep their coordinates. */
    private val identity =
        LetterboxTransform(inputSize = 320, scaledWidth = 320, scaledHeight = 320, padLeft = 0, padTop = 0)

    private fun tensor(spec: DigitModelSpec, vararg rows: FloatArray): FloatArray {
        val out = FloatArray(spec.numCandidates * spec.valuesPerCandidate)
        rows.forEachIndexed { index, row -> row.copyInto(out, index * spec.valuesPerCandidate) }
        return out
    }

    private fun row(
        spec: DigitModelSpec,
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        classIndex: Int,
        score: Float,
    ): FloatArray {
        val values = FloatArray(spec.valuesPerCandidate)
        values[0] = cx
        values[1] = cy
        values[2] = w
        values[3] = h
        values[DigitModelSpec.BOX_VALUES + classIndex] = score
        return values
    }

    @Test
    fun `When a candidate scores above the threshold then it becomes a detection of the best class`() {
        val spec = spec()
        val output = tensor(spec, row(spec, 0.5f, 0.5f, 0.2f, 0.4f, classIndex = 7, score = 0.9f))

        val actual = DetectionDecoder(spec).decode(output, identity)

        assertEquals(expected = 1, actual = actual.size)
        assertEquals(expected = DisplaySymbol.DIGIT_7, actual = actual.single().symbol)
        assertEquals(expected = 0.9f, actual = actual.single().confidence)
        val box = actual.single().box
        assertEquals(expected = 0.4f, actual = box.left, absoluteTolerance = 0.001f)
        assertEquals(expected = 0.6f, actual = box.right, absoluteTolerance = 0.001f)
        assertEquals(expected = 0.3f, actual = box.top, absoluteTolerance = 0.001f)
        assertEquals(expected = 0.7f, actual = box.bottom, absoluteTolerance = 0.001f)
    }

    @Test
    fun `When a candidate scores below the threshold then it is dropped`() {
        val spec = spec()
        val output = tensor(spec, row(spec, 0.5f, 0.5f, 0.2f, 0.4f, classIndex = 3, score = 0.39f))

        assertTrue(DetectionDecoder(spec).decode(output, identity).isEmpty())
    }

    @Test
    fun `When two boxes overlap strongly then only the more confident one survives whatever the class`() {
        val spec = spec()
        val output = tensor(
            spec,
            row(spec, 0.50f, 0.5f, 0.2f, 0.4f, classIndex = 1, score = 0.95f),
            row(spec, 0.52f, 0.5f, 0.2f, 0.4f, classIndex = 7, score = 0.80f),
        )

        val actual = DetectionDecoder(spec).decode(output, identity)

        assertEquals(expected = listOf(DisplaySymbol.DIGIT_1), actual = actual.map { it.symbol })
    }

    @Test
    fun `When two glyphs sit side by side then both are kept, most confident first`() {
        val spec = spec()
        val output = tensor(
            spec,
            row(spec, 0.30f, 0.5f, 0.1f, 0.4f, classIndex = 2, score = 0.70f),
            row(spec, 0.60f, 0.5f, 0.1f, 0.4f, classIndex = 8, score = 0.90f),
        )

        val actual = DetectionDecoder(spec).decode(output, identity)

        assertEquals(expected = listOf(DisplaySymbol.DIGIT_8, DisplaySymbol.DIGIT_2), actual = actual.map { it.symbol })
    }

    @Test
    fun `When there are more detections than the limit then the least confident are cut`() {
        val spec = spec(candidates = 3, maxDetections = 2)
        val output = tensor(
            spec,
            row(spec, 0.1f, 0.5f, 0.05f, 0.2f, classIndex = 0, score = 0.5f),
            row(spec, 0.4f, 0.5f, 0.05f, 0.2f, classIndex = 1, score = 0.9f),
            row(spec, 0.7f, 0.5f, 0.05f, 0.2f, classIndex = 2, score = 0.7f),
        )

        val actual = DetectionDecoder(spec).decode(output, identity)

        assertEquals(expected = listOf(DisplaySymbol.DIGIT_1, DisplaySymbol.DIGIT_2), actual = actual.map { it.symbol })
    }

    @Test
    fun `When the frame was letterboxed then boxes are mapped back to the frame`() {
        // A 160 x 320 frame (portrait) scaled to fit: 160 x 320 inside the 320 input with 80 px of border left and right.
        val spec = spec()
        val transform =
            LetterboxTransform(inputSize = 320, scaledWidth = 160, scaledHeight = 320, padLeft = 80, padTop = 0)
        // A glyph in the middle of the scaled image: input x 160 (80 + 80), y 160.
        val output = tensor(spec, row(spec, 0.5f, 0.5f, 0.1f, 0.2f, classIndex = 5, score = 0.9f))

        val box = DetectionDecoder(spec).decode(output, transform).single().box

        assertEquals(expected = 0.5f - 0.1f, actual = box.left, absoluteTolerance = 0.001f)
        assertEquals(expected = 0.5f + 0.1f, actual = box.right, absoluteTolerance = 0.001f)
        assertEquals(expected = 0.4f, actual = box.top, absoluteTolerance = 0.001f)
        assertEquals(expected = 0.6f, actual = box.bottom, absoluteTolerance = 0.001f)
    }

    @Test
    fun `When a box lies entirely in the grey border then it is ignored`() {
        val spec = spec()
        val transform =
            LetterboxTransform(inputSize = 320, scaledWidth = 160, scaledHeight = 320, padLeft = 80, padTop = 0)
        val output = tensor(spec, row(spec, 0.1f, 0.5f, 0.1f, 0.2f, classIndex = 5, score = 0.9f))

        assertTrue(DetectionDecoder(spec).decode(output, transform).isEmpty())
    }

    @Test
    fun `When a box crosses the edge of the frame then it is clamped to it`() {
        val spec = spec()
        val output = tensor(spec, row(spec, 0.98f, 0.5f, 0.1f, 0.2f, classIndex = 0, score = 0.9f))

        val box = DetectionDecoder(spec).decode(output, identity).single().box

        assertEquals(expected = 1f, actual = box.right)
    }

    @Test
    fun `When a class label is unknown to the app then candidates of that class are ignored`() {
        val spec = spec(unknown = true)
        val output = tensor(spec, row(spec, 0.5f, 0.5f, 0.2f, 0.4f, classIndex = 13, score = 0.9f))

        assertTrue(DetectionDecoder(spec).decode(output, identity).isEmpty())
    }

    @Test
    fun `When the tensor is shorter than declared then decoding stops without failing`() {
        val spec = spec(candidates = 10)
        val output = tensor(spec(candidates = 1), row(spec, 0.5f, 0.5f, 0.2f, 0.4f, classIndex = 4, score = 0.9f))

        assertEquals(expected = 1, actual = DetectionDecoder(spec).decode(output, identity).size)
    }

    private fun assertEquals(expected: Float, actual: Float, absoluteTolerance: Float) {
        assertTrue(kotlin.math.abs(expected - actual) <= absoluteTolerance, "Expected $expected but was $actual")
    }
}
