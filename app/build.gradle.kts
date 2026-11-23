import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.room)
  alias(libs.plugins.licensee)
  // cold-start-perf Phase F — Baseline Profile consumer side.
  // Companion :baselineprofile module produces the profile; this plugin
  // wires the merged result into the app APK + integrates the
  // `generateBaselineProfile` task into the app's task graph.
  alias(libs.plugins.androidx.baselineprofile)
}

// Capture build-time metadata for the About screen (Phase I.6).
// Via ProviderFactory.exec — Gradle 9.9 deprecates raw ProcessBuilder
// calls at configuration time.
val gitShortSha: String = runCatching {
  providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
    isIgnoreExitValue = true
  }.standardOutput.asText.get().trim().ifEmpty { "unknown" }
}.getOrDefault("unknown")

val buildDateUtc: String = DateTimeFormatter.ISO_LOCAL_DATE
  .format(LocalDate.now(ZoneOffset.UTC))

android {
    namespace = "com.eight87.shutterboy"
    compileSdk = 37
    compileSdkMinor = 2

    // R.F.27 — auto-generate `locales_config.xml` from `values-<locale>/`
    // directories so per-app `cmd locale set-app-locales` is consistent.
    // Without this the switch is flaky after `pm clear`.
    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    defaultConfig {
        applicationId = "com.eight87.shutterboy"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"

        buildConfigField("String", "GIT_SHA", "\"$gitShortSha\"")
        buildConfigField("String", "BUILD_DATE", "\"$buildDateUtc\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    testOptions {
      unitTests {
        isIncludeAndroidResources = true
      }
    }

    lint {
        // `Instantiatable` is a false positive for `androidx.activity.ComponentActivity`
        // subclasses — Lint's class-resolution doesn't see the AndroidX type via the
        // analysis classpath at lintVitalAnalyzeRelease time, so it flags MainActivity
        // even though it's perfectly instantiatable at runtime.
        disable += "Instantiatable"
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// cold-start-perf Phase F — merge the generated profile into the main
// `src/main/baseline-prof.txt` so every release APK ships with it
// (rather than living variant-side).
baselineProfile {
    mergeIntoMain = true
}

// oss-licenses Phase A — Licensee plugin generates a build-time inventory of
// every dependency that ships in the `releaseRuntimeClasspath`. Allowlist is
// report-only in v1 (no `failOnDisallowed`). EPL-1.0 junit is whitelisted at
// the artifact level because it's test-scope only and never reaches the APK.
licensee {
    allow("Apache-2.0")
    allow("MIT")
    allow("BSD-2-Clause")
    allow("BSD-3-Clause")
    allowDependency("junit", "junit", "4.13.2") {
        because("EPL-1.0; test-scope only, not shipped")
    }
    // Smart Tags pulls Google's on-device ML SDKs (Play Services ML Kit
    // image labeling + GenAI image description). These ship under Google's
    // PROPRIETARY terms, not an OSS license, so they have no SPDX id.
    // Allowed here by license URL so the build passes; `parseLicensesCatalog`
    // deliberately filters them OUT of the in-app *open-source*
    // acknowledgements list (no SPDX ⇒ not open source).
    allowUrl("https://developer.android.com/studio/terms.html")
    allowUrl("https://developers.google.com/ml-kit/terms")
}

// Copy the Licensee-generated `artifacts.json` into the app's assets so the
// LicensesScreen can read it at runtime via AssetManager. Wired as a
// dependency of both `mergeReleaseAssets` and `mergeDebugAssets` so every
// fresh build keeps the inventory in sync with the resolved classpath.
val copyLicenseeInventory = tasks.register<Copy>("copyLicenseeInventory") {
    val src = layout.buildDirectory.file("reports/licensee/androidRelease/artifacts.json")
    val dst = layout.projectDirectory.dir("src/main/assets/licenses")
    from(src)
    into(dst)
    dependsOn("licenseeAndroidRelease")
}

afterEvaluate {
    tasks.matching { it.name == "mergeReleaseAssets" || it.name == "mergeDebugAssets" }
        .configureEach { dependsOn(copyLicenseeInventory) }
    // Gradle 9 strict-mode: every Lint task that reads the merged-assets
    // directory needs an explicit dependency on the inventory copy that
    // writes into it. Match by name prefix — generateLint*ReportModel
    // + lintVitalAnalyze* + lintAnalyze*.
    tasks.matching { task ->
        val n = task.name
        n.startsWith("generate") && n.contains("Lint") ||
            n.startsWith("lintAnalyze") ||
            n.startsWith("lintVitalAnalyze") ||
            n.startsWith("lintReportModel") ||
            n.startsWith("lintVitalReportModel")
    }.configureEach { dependsOn(copyLicenseeInventory) }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        // Kotlin 2.4 made the param + property annotation use-site target
        // the default, so the former `-Xannotation-default-target` flag is gone.
    }
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)
  testImplementation(composeBom)

  // Core Android
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Lifecycle
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  debugImplementation(libs.androidx.compose.ui.tooling)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Navigation 3
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Image loading + video-frame thumbnails
  implementation(libs.coil3.compose)
  implementation(libs.coil3.video)

  // Media3 ExoPlayer + PlayerView — inline video playback in the
  // viewer when a MediaStore row's mimeType starts with "video/".
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.ui)

  // Smart Tags — on-device AI image tagging. Image labeling is
  // GMS-backed (model lives in Play Services, downloaded on demand,
  // not bundled in the APK). GenAI image description runs on AICore /
  // Gemini Nano. Both degrade gracefully to "unavailable" off-GMS /
  // off-AICore — the app keeps working without the feature.
  implementation(libs.mlkit.image.labeling)
  implementation(libs.mlkit.genai.image.description)

  // EXIF metadata
  implementation(libs.androidx.exifinterface)

  // SAF tree walking
  implementation(libs.androidx.documentfile)

  // Room
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // DataStore
  implementation(libs.androidx.datastore.preferences)

  // kotlinx.serialization
  implementation(libs.kotlinx.serialization.json)

  // SplashScreen compat
  implementation(libs.androidx.core.splashscreen)

  // ProfileInstaller — applies the baseline-prof.txt that ships in the APK
  // at first launch (cold-start-perf Phase F.3). No-op until the
  // :baselineprofile module is actually run against a device.
  implementation(libs.androidx.profileinstaller)

  // Wire the baseline profile generator module so `generateBaselineProfile`
  // knows where to source the recorded profile from.
  "baselineProfile"(project(":baselineprofile"))

  // Local tests (Robolectric on the JVM)
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.ext.junit)
  testImplementation(libs.androidx.arch.core.testing)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.compose.ui.test.manifest)

  // Instrumented tests
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
}
