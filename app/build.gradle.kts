import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Android only installs an update over the old app, keeping its readings, when both are signed
// with the same key and the new versionCode isn't lower. CI decodes the shared key from the
// repository secrets into VITALS_KEYSTORE and numbers each build with VITALS_BUILD_NUMBER; local
// builds and forks without the secrets fall back to the default debug key and versionCode 1.
val sharedKeystore = providers.environmentVariable("VITALS_KEYSTORE").orNull?.let(::file)?.takeIf { it.isFile }
val buildNumber = providers.environmentVariable("VITALS_BUILD_NUMBER").orNull?.toIntOrNull()

android {
    namespace = "com.cyberkryption.vitals"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cyberkryption.vitals"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber ?: 1
        versionName = if (buildNumber != null) "1.0.$buildNumber" else "1.0"
    }

    signingConfigs {
        if (sharedKeystore != null) {
            create("shared") {
                val password = providers.environmentVariable("VITALS_KEYSTORE_PASSWORD").orNull
                    ?: error("VITALS_KEYSTORE is set but VITALS_KEYSTORE_PASSWORD isn't")
                storeFile = sharedKeystore
                storePassword = password
                keyAlias = "vitals"
                keyPassword = password
            }
        }
    }

    buildTypes {
        val shared = signingConfigs.findByName("shared")
        debug {
            if (shared != null) signingConfig = shared
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = shared
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// Screenshot tests render the app through Robolectric into docs/screenshots. They're slow and
// write files, so they run only when asked: ./gradlew :app:testDebugUnitTest -Pscreenshots
tasks.withType<Test>().configureEach {
    if (providers.gradleProperty("screenshots").isPresent) {
        filter.includeTestsMatching("*.ScreenshotTest")
        systemProperty("vitals.screenshots.dir", rootProject.file("docs/screenshots").absolutePath)
    } else {
        filter.excludeTestsMatching("*.ScreenshotTest")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.text.recognition)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
