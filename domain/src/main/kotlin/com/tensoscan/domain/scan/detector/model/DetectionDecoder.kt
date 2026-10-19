package com.tensoscan.domain.scan.detector.model

import com.tensoscan.domain.scan.model.DisplaySymbol
import com.tensoscan.domain.scan.model.NormalizedBox
import com.tensoscan.domain.scan.model.SymbolDetection
import kotlin.math.max
import kotlin.math.min

/**
 * Turns the raw model output into glyph detections (`digit-detector-model.md` §6): threshold, class-agnostic
 * non-maximum suppression, limit, and undo the letterbox so the boxes are normalised to the ROI.
 *
 * The output tensor is `[N, 4 + C]` flattened row by row: `cx, cy, w, h` normalised to the model input, then one
 * sigmoid score per class.
 */
class DetectionDecoder(private val spec: DigitModelSpec) {

    fun decode(output: FloatArray, transform: LetterboxTransform): List<SymbolDetection> {
        val stride = spec.valuesPerCandidate
        val candidates = ArrayList<Candidate>()
        for (index in 0 until spec.numCandidates) {
            val offset = index * stride
            if (offset + stride > output.size) break
            var bestClass = -1
            var bestScore = spec.scoreThreshold
            for (classIndex in spec.classes.indices) {
                val score = output[offset + DigitModelSpec.BOX_VALUES + classIndex]
                if (score >= bestScore) {
                    bestScore = score
                    bestClass = classIndex
                }
            }
            if (bestClass < 0) continue
            val symbol = spec.symbolOf(bestClass) ?: continue
            val box = toRoi(output, offset, transform) ?: continue
            candidates += Candidate(symbol, box, bestScore)
        }
        return suppress(candidates.sortedByDescending { it.score })
            .take(spec.maxDetections)
            .map { SymbolDetection(it.symbol, it.box, it.score) }
    }

    /** Box in the letterboxed input -> box in the ROI, clamped to it; null when it lies entirely in the border. */
    private fun toRoi(output: FloatArray, offset: Int, transform: LetterboxTransform): NormalizedBox? {
        val size = transform.inputSize.toFloat()
        val cx = output[offset] * size
        val cy = output[offset + 1] * size
        val halfWidth = output[offset + 2] * size / 2f
        val halfHeight = output[offset + 3] * size / 2f

        val left = ((cx - halfWidth - transform.padLeft) / transform.scaledWidth).coerceIn(0f, 1f)
        val right = ((cx + halfWidth - transform.padLeft) / transform.scaledWidth).coerceIn(0f, 1f)
        val top = ((cy - halfHeight - transform.padTop) / transform.scaledHeight).coerceIn(0f, 1f)
        val bottom = ((cy + halfHeight - transform.padTop) / transform.scaledHeight).coerceIn(0f, 1f)
        return if (right > left && bottom > top) NormalizedBox(left, top, right, bottom) else null
    }

    /** Glyphs of a display never overlap, so two overlapping boxes are the same glyph whatever their classes. */
    private fun suppress(sorted: List<Candidate>): List<Candidate> {
        val kept = ArrayList<Candidate>()
        for (candidate in sorted) {
            if (kept.none { iou(it.box, candidate.box) > spec.nmsIouThreshold }) kept += candidate
        }
        return kept
    }

    private fun iou(a: NormalizedBox, b: NormalizedBox): Float {
        val width = min(a.right, b.right) - max(a.left, b.left)
        val height = min(a.bottom, b.bottom) - max(a.top, b.top)
        if (width <= 0f || height <= 0f) return 0f
        val intersection = width * height
        val union = a.width * a.height + b.width * b.height - intersection
        return if (union <= 0f) 0f else intersection / union
    }

    private class Candidate(val symbol: DisplaySymbol, val box: NormalizedBox, val score: Float)
}
