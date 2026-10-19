package com.tensoscan.ui.feature.summary.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tensoscan.domain.measurement.classification.BloodPressureCategory
import com.tensoscan.ui.theme.BackgroundGreenPressureState
import com.tensoscan.ui.theme.BackgroundRedPressureState
import com.tensoscan.ui.theme.BackgroundYellowPressureState
import com.tensoscan.ui.theme.TensoScanTypography
import com.tensoscan.ui.R.string as RString

/** Informative label of a reading. The colour never carries the meaning alone: the name is always written. */
@Composable
internal fun BloodPressureCategoryChip(category: BloodPressureCategory, modifier: Modifier = Modifier) {
    val label = stringResource(category.labelRes())
    val color = when (category) {
        BloodPressureCategory.NON_ELEVATED -> BackgroundGreenPressureState
        BloodPressureCategory.ELEVATED -> BackgroundYellowPressureState
        BloodPressureCategory.HYPERTENSION_RANGE -> BackgroundRedPressureState
    }
    val description = stringResource(RString.category_content_description, label)
    AssistChip(
        onClick = {},
        label = { Text(text = label, style = TensoScanTypography.bodySmall) },
        colors = AssistChipDefaults.assistChipColors(containerColor = color.copy(alpha = 0.1f), labelColor = color),
        modifier = modifier.semantics { contentDescription = description },
    )
}

internal fun BloodPressureCategory.labelRes(): Int = when (this) {
    BloodPressureCategory.NON_ELEVATED -> RString.category_non_elevated
    BloodPressureCategory.ELEVATED -> RString.category_elevated
    BloodPressureCategory.HYPERTENSION_RANGE -> RString.category_hypertension_range
}
