import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use(::load)
    }

// Debug builds talk to the Android emulator's host loopback unless local.properties overrides it
// (physical device: tensoscan.apiBaseUrl=http://<computer-ip>:8000/).
// Which digit detector the scanner tries first: "mlkit" (default) or "litert" (local.properties: tensoscan.detector).
val debugDetector = localProperties.getProperty("tensoscan.detector", "mlkit")

val debugApiBaseUrl = localProperties.getProperty("tensoscan.apiBaseUrl", "http://10.0.2.2:8000/")

// Production endpoint; placeholder until the deployment is decided (spec A-001, Q-A001-02).
val releaseApiBaseUrl =
    providers
        .gradleProperty("tensoscan.releaseApiBaseUrl")
        .orElse("https://api.tensoscan.example/")
        .get()

android {
    namespace = "com.tensoscan"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.tensoscan"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
            // The server does not issue refresh tokens until the v1 auth endpoints are deployed (spec B-001).
            buildConfigField("boolean", "REFRESH_TOKEN_ENABLED", "true")
            buildConfigField("String", "DIGIT_DETECTOR_PRIMARY", "\"$debugDetector\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            buildConfigField("boolean", "REFRESH_TOKEN_ENABLED", "true")
            // The in-house model becomes the primary detector only after the field validation (A-002 T-A002-35).
            buildConfigField("String", "DIGIT_DETECTOR_PRIMARY", "\"mlkit\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":ui"))
    implementation(project(":presentation"))
    implementation(project(":data"))
    implementation(project(":domain"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
