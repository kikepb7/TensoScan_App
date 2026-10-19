package com.tensoscan.domain.support

import com.tensoscan.domain.scan.model.DisplaySymbol
import com.tensoscan.domain.scan.model.NormalizedBox
import com.tensoscan.domain.scan.model.SymbolDetection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Reader of `reading-golden-cases.json`, the cases shared with the backend (spec contract `reading-rules.md`). */
object GoldenCases {

    data class ParserCase(
        val name: String,
        val detections: List<SymbolDetection>,
        val status: String,
        val systolic: Int?,
        val diastolic: Int?,
        val pulse: Int?,
        /** Width / height of the ROI the boxes are normalised to (contract 1.1.0); 0.8 when the case does not say. */
        val roiAspect: Double = 0.8,
    )

    data class ValidationCase(
        val name: String,
        val systolic: Int?,
        val diastolic: Int?,
        val pulse: Int?,
        val errors: List<Pair<String, String>>,
        val warnings: List<String>,
    )

    private val root: JsonObject by lazy {
        val stream = checkNotNull(GoldenCases::class.java.getResourceAsStream("/contracts/reading-golden-cases.json")) {
            "reading-golden-cases.json is missing: run the syncContractFixtures task"
        }
        Json.parseToJsonElement(stream.reader().readText()).jsonObject
    }

    val parserCases: List<ParserCase> by lazy {
        root.getValue("parser_cases").jsonArray.map { element ->
            val case = element.jsonObject
            val expected = case.getValue("expected").jsonObject
            ParserCase(
                name = case.getValue("name").jsonPrimitive.content,
                detections = case.getValue("detections").jsonArray.map(::detection),
                status = expected.getValue("status").jsonPrimitive.content,
                systolic = expected.intOrNull("systolic"),
                diastolic = expected.intOrNull("diastolic"),
                pulse = expected.intOrNull("pulse"),
                roiAspect = (case["roi_aspect"] as? JsonPrimitive)?.doubleOrNull ?: 0.8,
            )
        }
    }

    val validationCases: List<ValidationCase> by lazy {
        root.getValue("validation_cases").jsonArray.map { element ->
            val case = element.jsonObject
            val input = case.getValue("input").jsonObject
            ValidationCase(
                name = case.getValue("name").jsonPrimitive.content,
                systolic = input.intOrNull("systolic"),
                diastolic = input.intOrNull("diastolic"),
                pulse = input.intOrNull("pulse"),
                errors = case.getValue("errors").jsonArray.map {
                    it.jsonObject.getValue("field").jsonPrimitive.content to
                        it.jsonObject.getValue("code").jsonPrimitive.content
                },
                warnings = case.getValue("warnings").jsonArray.map { it.jsonPrimitive.content },
            )
        }
    }

    private fun JsonObject.intOrNull(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

    private fun detection(element: JsonElement): SymbolDetection {
        val json = element.jsonObject
        val box = (json.getValue("box") as JsonArray).map { it.jsonPrimitive.float }
        return SymbolDetection(
            symbol = symbolOf(json.getValue("symbol").jsonPrimitive.content),
            box = NormalizedBox(left = box[0], top = box[1], right = box[2], bottom = box[3]),
            confidence = json.getValue("confidence").jsonPrimitive.float,
        )
    }

    private fun symbolOf(label: String): DisplaySymbol = when (label) {
        "colon" -> DisplaySymbol.COLON
        "dash" -> DisplaySymbol.DASH
        "E" -> DisplaySymbol.LETTER_E
        "r" -> DisplaySymbol.LETTER_R
        else -> DisplaySymbol.entries[checkNotNull(label.toIntOrNull()) { "Unknown symbol label: $label" }]
    }
}
