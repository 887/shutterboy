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
val gitShortSha: String = runCatching {
  val proc = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
    .redirectErrorStream(true)
    .start()
  proc.waitFor()
  proc.inputStream.bufferedReader().readText().trim().ifEmpty { "unknown" }
}.getOrDefault("unknown")

val buildDateUtc: String = DateTimeFormatter.ISO_LOCAL_DATE
  .format(LocalDate.now(ZoneOffset.UTC))

android {
    namespace = "com.eight87.shutterboy"
    compileSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
}

// Copy the Licensee-generated `artifacts.json` into the app's assets so the
// LicensesScreen can read it at runtime via AssetManager. Wired as a
// dependency of both `mergeReleaseAssets` and `mergeDebugAssets` so every
// fresh build keeps the inventory in sync with the resolved classpath.
val copyLicenseeInventory by tasks.registering(Copy::class) {
    val src = layout.buildDirectory.file("reports/licensee/androidRelease/artifacts.json")
    val dst = layout.projectDirectory.dir("src/main/assets/licenses")
    from(src)
    into(dst)
    dependsOn("licenseeAndroidRelease")
}

afterEvaluate {
    tasks.matching { it.name == "mergeReleaseAssets" || it.name == "mergeDebugAssets" }
        .configureEach { dependsOn(copyLicenseeInventory) }
}

kotlin {
    jvmToolchain(17)
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

  // Image loading
  implementation(libs.coil3.compose)

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
