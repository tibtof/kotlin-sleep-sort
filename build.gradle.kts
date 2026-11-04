plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

group = "com.github.tibof"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

ktlint {
    version.set(libs.versions.ktlint.cli)
    // ktlint 1.7 stopped running no-unused-imports by default; keep the check we had.
    additionalEditorconfig.set(mapOf("ktlint_standard_no-unused-imports" to "enabled"))
}
