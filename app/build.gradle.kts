plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "ch.drivedeck.launcher"
    compileSdk = 35
    defaultConfig {
        applicationId = "ch.drivedeck.launcher"
        minSdk = 29
        targetSdk = 35
        versionCode = providers.environmentVariable("DRIVEDECK_VERSION_CODE").orNull?.toIntOrNull() ?: 3
        versionName = providers.environmentVariable("DRIVEDECK_VERSION_NAME").orNull ?: "0.3.0"
    }
    buildTypes { release { isMinifyEnabled = false; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true; buildConfig = true }
}
tasks.register("printVersionName") {
    doLast { println(android.defaultConfig.versionName) }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":core:model")); implementation(project(":core:preferences")); implementation(project(":core:design"))
    implementation(project(":feature:home")); implementation(project(":feature:apps")); implementation(project(":feature:settings"))
    implementation(platform(libs.compose.bom)); implementation(libs.compose.ui); implementation(libs.compose.material3)
    implementation(libs.activity.compose); implementation(libs.lifecycle.runtime.compose)
    debugImplementation(libs.compose.ui.tooling)
}
dependencies { implementation(project(":integration:media")) }
dependencies { implementation(project(":integration:gps")) }
