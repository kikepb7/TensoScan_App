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
    get() = if (isSystemInDarkTheme()) Color(0xFF4B5563) else Color(0xFFCBD5E1)

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
    get() = if (isSystemInDarkTheme()) Color(0xFF4CAAC4) else Color(0xFF80CFE3)

val TextFieldBorderColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF8ED7C6) else Color(0xFF80CFE3)

val TextFieldTextColor
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFF7F7F7) else Color(0xFF1A1A1A)

val CardBackgroundColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF2D2D2D) else Color(0xFFFFFFFF)

val CardLabelTextColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFB3B3B3) else Color(0xFF6E6E6E)

val TopBarColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF1A1A1A) else Color(0xFFFFFFFF)

val TopBarTitleColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) PrimaryTextColor else PrimaryTextColor

val BottomBarContainerColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF2D2D2D) else Color(0xFFFFFFFF)

val BottomBarSelectedIconColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF8ED7C6) else Color(0xFF80CFE3)

val BottomBarUnselectedIconColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFB3B3B3) else Color(0xFF6E6E6E)

val BottomBarIndicatorColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF1A1A1A) else Color(0xFFF7F7F7)

val BackgroundUserMessage: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF29A085) else Color(0xFF8ED7C6)

val BackgroundAssistantMessage: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF1F1F1F) else Color(0xFF80CFE3)

val InputFieldBorderColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF8ED7C6) else Color(0xFF80CFE3)

val InputFieldCursorColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF8ED7C6) else Color(0xFF80CFE3)

val InputFieldTextColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color.White else Color.Black

val primaryGreenBackgroundPressureState = Color(0xFFA8D0A5)
val secondaryGreenBackgroundPressureState = Color(0xFFD0E5CD)
val primaryYellowBackgroundPressureState = Color(0xFFD8C16C)
val secondaryYellowBackgroundPressureState = Color(0xFFE7DAA1)
val primaryRedBackgroundPressureState = Color(0xFFFF9A9A)
val secondaryRedBackgroundPressureState = Color(0xFFFFC3C3)


val BackgroundGreenPressureState
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryGreenBackgroundPressureState else primaryGreenBackgroundPressureState

val BackgroundYellowPressureState
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryYellowBackgroundPressureState else primaryYellowBackgroundPressureState

val BackgroundRedPressureState
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryRedBackgroundPressureState else primaryRedBackgroundPressureState



val primaryBackgroundScreenColor = Color(0xFFFFFFFF)
val secondaryBackgroundScreenColor = Color(0xFF2A2A2A)
val primaryDefaultButtonColor = Color(0xFFFF3543)
val secondaryDefaultButtonColor = Color(0xFF666756)

val BackgroundScreenColor
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryBackgroundScreenColor else primaryBackgroundScreenColor

val TensoScanDefaultButtonColor
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryDefaultButtonColor else primaryDefaultButtonColor
