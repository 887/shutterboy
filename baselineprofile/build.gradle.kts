// cold-start-perf Phase F — Baseline Profile generator module.
//
// Records the cold-boot critical path (launch -> photos timeline visible)
// via MacrobenchmarkRule and emits an AOT profile that ships with the app
// APK. Typically shaves 25-35% off cold start on its own.
//
// Generation is NOT part of the normal build loop — it requires a connected
// non-debuggable + benchmark-signed APK on a physical device or an aosp_*
// AVD (google_apis_playstore images often block macrobench). Run via
// `./scripts/build-release-apk.sh --with-baseline-profile` when a suitable
// device is connected.

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.eight87.shutterboy.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Self-instrumenting test module: same process drives the macrobench
    // and is the system-under-test (after the plugin generates the
    // non-debuggable benchmark/baselineprofile variants of :app).
    targetProjectPath = ":app"
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}

baselineProfile {
    // Default to physically-connected devices (or a manually-launched AVD).
    // Gradle-managed device wiring would need an aosp_* image declaration
    // in the root build; deferred until cold-start-perf F.5 (CI variant).
    useConnectedDevices = true
}
