package com.example.tensoscan.ui.model

import com.example.tensoscan.R.raw as RRaw
import com.example.tensoscan.R.string as RString

sealed class OnboardingModel(
    val animation: Int,
    val title: Int,
    val description: Int
) {
    data object FirstPage : OnboardingModel(
        animation = RRaw.onboarding1,
        title = RString.tenso_scan_welcome_onboarding_first_title,
        description = RString.tenso_scan_onboarding_first_description
    )

    data object SecondPage : OnboardingModel(
        animation = RRaw.onboarding2,
        title = RString.tenso_scan_ia_monitoring_onboarding_second_title,
        description = RString.tenso_scan_onboarding_second_description
    )

    data object ThirdPage : OnboardingModel(
        animation = RRaw.onboarding3,
        title = RString.tenso_scan_manage_historical_onboarding_third_title,
        description = RString.tenso_scan_onboarding_third_description
    )

    companion object {
        val pages = listOf(FirstPage, SecondPage, ThirdPage)
    }
}