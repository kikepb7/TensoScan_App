import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// The golden cases of the shared contract (reading rules) are the single source of truth for the parser and the
// validator tests; they are copied from specs/contracts so the repository never holds two versions of them.
val syncContractFixtures by tasks.registering(Copy::class) {
    from(layout.projectDirectory.file("../specs/contracts/reading-golden-cases.json"))
    into(layout.buildDirectory.dir("generated/contract-fixtures/contracts"))
}

sourceSets.test {
    resources.srcDir(layout.buildDirectory.dir("generated/contract-fixtures"))
}

tasks.processTestResources {
    dependsOn(syncContractFixtures)
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    // Flow and CoroutineDispatcher are part of the public API of the domain.
    api(libs.kotlinx.coroutines.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":core:testing"))
}
