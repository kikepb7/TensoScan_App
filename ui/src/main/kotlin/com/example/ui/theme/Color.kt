package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)


val menta_50 = Color(0xFFEFFAF6)
val menta_100 = Color(0xFFD7F4E8)
val menta_200 = Color(0xFF9DE1C9)
val menta_300 = Color(0xFF80D5BC)
val menta_400 = Color(0xFF4CBB9F)
val menta_500 = Color(0xFF29A085)
val menta_600 = Color(0xFF1B806C)
val menta_700 = Color(0xFF156758)
val menta_800 = Color(0xFF135247)
val menta_900 = Color(0xFF11433B)
val menta_950 = Color(0xFF082622)
val lavanda_50 = Color(0xFFFAF7FD)
val lavanda_100 = Color(0xFFF3EDFA)
val lavanda_200 = Color(0xFFE8DFF5)
val lavanda_300 = Color(0xFFD7C5ED)
val lavanda_400 = Color(0xFFB28DDB)
val lavanda_500 = Color(0xFFA57AD2)
val lavanda_600 = Color(0xFF8E5DC0)
val lavanda_700 = Color(0xFF7949A7)
val lavanda_800 = Color(0xFF664089)
val lavanda_900 = Color(0xFF53346F)
val lavanda_950 = Color(0xFF381C4F)
val melocoton_50 = Color(0xFFFFF9ED)
val melocoton_100 = Color(0xFFFFF2D5)
val melocoton_200 = Color(0xFFFFE1A8)
val melocoton_300 = Color(0xFFFFCB72)
val melocoton_400 = Color(0xFFFDA93A)
val melocoton_500 = Color(0xFFFC8F13)
val melocoton_600 = Color(0xFFED7209)
val melocoton_700 = Color(0xFFC4560A)
val melocoton_800 = Color(0xFF9C4410)
val melocoton_900 = Color(0xFF7D3911)
val melocoton_950 = Color(0xFF441B06)
val rosa_50 = Color(0xFFFDF3F3)
val rosa_100 = Color(0xFFFDE4E3)
val rosa_200 = Color(0xFFFBCFCD)
val rosa_300 = Color(0xFFF7A6A2)
val rosa_400 = Color(0xFFF17E78)
val rosa_500 = Color(0xFFE6544D)
val rosa_600 = Color(0xFFD3372F)
val rosa_700 = Color(0xFFB12B24)
val rosa_800 = Color(0xFF932621)
val rosa_900 = Color(0xFF7A2622)
val rosa_950 = Color(0xFF420F0D)
val blanco_50 = Color(0xFFFFFFFF)
val blanco_100 = Color(0xFFEFEFEF)
val blanco_200 = Color(0xFFDCDCDC)
val blanco_300 = Color(0xFFBDBDBD)
val blanco_400 = Color(0xFF989898)
val blanco_500 = Color(0xFF7C7C7C)
val blanco_600 = Color(0xFF656565)
val blanco_700 = Color(0xFF525252)
val blanco_800 = Color(0xFF464646)
val blanco_900 = Color(0xFF3D3D3D)
val blanco_950 = Color(0xFF292929)





val backgroundBottomBarIcon = Color(0xFFFCE7E8)
val bottomBarIcon = Color(0xFF440D1A)
val primaryBackgroundMeasureCard = Color(0xFFF3AEB4)
val secondaryBackgroundMeasureCard = Color(0xFFF8D3D5)
val primaryLabelMeasureCard = Color(0xFF8D223B)
val secondaryLabelMeasureCard = Color(0xFFAB2640)
val primaryGreenBackgroundPressureState = Color(0xFFA8D0A5)
val secondaryGreenBackgroundPressureState = Color(0xFFD0E5CD)
val primaryYellowBackgroundPressureState = Color(0xFFD8C16C)
val secondaryYellowBackgroundPressureState = Color(0xFFE7DAA1)
val primaryRedBackgroundPressureState = Color(0xFFFF9A9A)
val secondaryRedBackgroundPressureState = Color(0xFFFFC3C3)
val primaryBackgroundAssistantMessage = Color(0xFFB9C4E8)
val secondaryBackgroundAssistantMessage = Color(0xFFD4DCF1)
val primaryBackgroundUserMessage = Color(0xFF8FB9A4)
val secondaryBackgroundUserMessage = Color(0xFFBAD5C7)

val CardBackgroundLight = Color(0xFFF5F5FA)
val CardBackgroundDark = Color(0xFF2C2C3A)
val PrimaryLabelLight = Color(0xFF3D3D5C)
val PrimaryLabelDark = Color(0xFFCACCE5)
val SecondaryText = Color(0xFF7A7A9D)
val PulseIconColor = Color(0xFFE57373)
val DeleteIconColor = Color(0xFFBA1A1A)

val BackgroundMeasureCard
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryBackgroundMeasureCard else primaryBackgroundMeasureCard

val LabelMeasureCard
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryLabelMeasureCard else primaryLabelMeasureCard

val BackgroundGreenPressureState
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryGreenBackgroundPressureState else primaryGreenBackgroundPressureState

val BackgroundYellowPressureState
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryYellowBackgroundPressureState else primaryYellowBackgroundPressureState

val BackgroundRedPressureState
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryRedBackgroundPressureState else primaryRedBackgroundPressureState

val BackgroundAssistantMessage
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryBackgroundAssistantMessage else primaryBackgroundAssistantMessage

val BackgroundUserMessage
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryBackgroundUserMessage else primaryBackgroundUserMessage

val CardBackgroundColor
    @Composable
    get() = if (isSystemInDarkTheme()) CardBackgroundDark else CardBackgroundLight

val LabelTextColor
    @Composable
    get() = if (isSystemInDarkTheme()) PrimaryLabelDark else PrimaryLabelLight





val primaryTopBarColor = Color(0xFFFFFFFF)
val secondaryTopBarColor = Color(0xFF3A3A3A)
val primaryBackgroundScreenColor = Color(0xFFFFFFFF)
val secondaryBackgroundScreenColor = Color(0xFF2A2A2A)
val primaryBackgroundTrackerView = Color(0xFFEDECFD)
val secondaryBackgroundTrackerView = Color(0xFF1E1E1E)
val primaryScanDeviceTrackerButtonColor = Color(0xFF6E9BD2)
val secondaryScanDeviceTrackerButtonColor = Color(0xFFD26E6E)
val primaryWriteManuallyTrackerButtonColor = Color(0xFF6ED297)
val secondaryWriteManuallyTrackerButtonColor = Color(0xFFD29D6E)
val primarySummaryTrackerButtonColor = Color(0xFFAEC4B9)
val secondarySummaryTrackerButtonColor = Color(0xFF565656)
val primaryBackgroundSummaryUploadButtonSelector = Color(0xFFEC9D9D)
val secondaryBackgroundSummaryUploadButtonSelector = Color(0xFF666756)
val primaryBackgroundSummarySetManuallyButtonSelector = Color(0xFF9DECB7)
val secondaryBackgroundSummarySetManuallyButtonSelector = Color(0xFF666756)
val primaryDefaultButtonColor = Color(0xFFFF3543)
val secondaryDefaultButtonColor = Color(0xFF666756)

val TopBarColor
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryTopBarColor else primaryTopBarColor

val BackgroundScreenColor
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryBackgroundScreenColor else primaryBackgroundScreenColor

val ScanDeviceButtonTrackerColor
    @Composable
    get() = if (isSystemInDarkTheme()) primaryWriteManuallyTrackerButtonColor else secondaryWriteManuallyTrackerButtonColor

val WriteManuallyButtonTrackerColor
    @Composable
    get() = if (isSystemInDarkTheme()) primaryScanDeviceTrackerButtonColor else secondaryScanDeviceTrackerButtonColor

val SummaryTrackerButtonColor
    @Composable
    get() = if (isSystemInDarkTheme()) secondarySummaryTrackerButtonColor else primarySummaryTrackerButtonColor

val TensoScanDefaultButtonColor
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryDefaultButtonColor else primaryDefaultButtonColor

val BackgroundTrackerViewColor
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryBackgroundTrackerView else primaryBackgroundTrackerView


val BackgroundSummarySetManuallyButtonSelectorColor
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryBackgroundSummarySetManuallyButtonSelector else primaryBackgroundSummarySetManuallyButtonSelector

val BackgroundSummaryUploadButtonSelectorColor
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryBackgroundSummaryUploadButtonSelector else primaryBackgroundSummaryUploadButtonSelector