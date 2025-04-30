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


val BackgroundAppColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF1A1A1A) else Color(0xFFF7F7F7)

val CardColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF2D2D2D) else Color(0xFFFFFFFF)

val PrimaryTextColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF8ED7C6) else Color(0xFF80CFE3)

val SecondaryTextColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFB3B3B3) else Color(0xFF6E6E6E)

val ButtonLoginColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF29A085) else Color(0xFF8ED7C6)

val ButtonLogoutColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFB12B24) else Color(0xFFF2878B)

val ButtonBackColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF7949A7) else Color(0xFFC2A4E3)

val ButtonUploadPhotoColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFFC8F13) else Color(0xFFFFD9A3)

val ButtonGetReportsColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF156758) else Color(0xFF80CFE3)

val ButtonTextColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFFFFFFF) else Color(0xFF1A1A1A)

val IconColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFC2A4E3) else Color(0xFF80CFE3)

val TextFieldBorderColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF8ED7C6) else Color(0xFF80CFE3)

val TextFieldTextColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFF7F7F7) else Color(0xFF1A1A1A)


val backgroundBottomBarIcon = Color(0xFFFCE7E8)
val bottomBarIcon = Color(0xFF440D1A)
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




val primaryTopBarColor = Color(0xFFFFFFFF)
val secondaryTopBarColor = Color(0xFF3A3A3A)
val primaryBackgroundScreenColor = Color(0xFFFFFFFF)
val secondaryBackgroundScreenColor = Color(0xFF2A2A2A)
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

val TensoScanDefaultButtonColor
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryDefaultButtonColor else primaryDefaultButtonColor

val BackgroundSummarySetManuallyButtonSelectorColor
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryBackgroundSummarySetManuallyButtonSelector else primaryBackgroundSummarySetManuallyButtonSelector

val BackgroundSummaryUploadButtonSelectorColor
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryBackgroundSummaryUploadButtonSelector else primaryBackgroundSummaryUploadButtonSelector