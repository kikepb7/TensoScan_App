plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlinxSerialization)
    kotlin("kapt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

kapt {
    correctErrorTypes = true
    generateStubs = true
}


dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.koin.core)
    testImplementation(libs.junit)
}
