// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.test) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  // cold-start-perf Phase F — declare so subprojects can pull in via alias.
  alias(libs.plugins.androidx.baselineprofile) apply false
}