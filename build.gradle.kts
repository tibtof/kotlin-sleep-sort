plugins {
    kotlin("jvm") version "2.0.0"
}

group = "com.github.tibof"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(libs.coroutines.core)
    testImplementation(libs.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}