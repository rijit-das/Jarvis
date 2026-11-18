// Top-level build file. Plugin versions are declared in gradle/libs.versions.toml
// and applied per-module. No shared build logic lives here on purpose.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.hilt) apply false
}
