package com.example.tensoscan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val primaryBackgroundTrackerView = Color(0xFFEDECFD)
val secondaryBackgroundTrackerView = Color(0xFF1E1E1E)
val primaryScanDeviceTrackerButtonColor = Color(0xFF6E9BD2)
val secondaryScanDeviceTrackerButtonColor = Color(0xFFD26E6E)
val primaryWriteManuallyTrackerButtonColor = Color(0xFF6ED297)
val secondaryWriteManuallyTrackerButtonColor = Color(0xFFD29D6E)
val primarySummaryTrackerButtonColor = Color(0xFFAEC4B9)
val secondarySummaryTrackerButtonColor = Color(0xFF565656)

val ScanDeviceButtonTrackerColor
    @Composable
    get() = if (isSystemInDarkTheme()) primaryWriteManuallyTrackerButtonColor else secondaryWriteManuallyTrackerButtonColor

val WriteManuallyButtonTrackerColor
    @Composable
    get() = if (isSystemInDarkTheme()) primaryScanDeviceTrackerButtonColor else secondaryScanDeviceTrackerButtonColor

val SummaryTrackerButtonColor
    @Composable
    get() = if (isSystemInDarkTheme()) secondarySummaryTrackerButtonColor else primarySummaryTrackerButtonColor

val BackgroundTrackerViewColor
    @Composable
    get() = if (isSystemInDarkTheme()) secondaryBackgroundTrackerView else primaryBackgroundTrackerView