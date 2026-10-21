import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.kotlin.test.junit)
}

tasks.test {
    // ./gradlew :core:test -PdisplayPhotos=/path/to/folder also checks the display reader against
    // photos kept out of the repository; see SevenSegmentReaderTest.readsRealPhotos.
    providers.gradleProperty("displayPhotos").orNull?.let { systemProperty("displayPhotos", it) }
}
