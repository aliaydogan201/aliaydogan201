plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.cinebox.tv"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.cinebox.tv"
        minSdk = 23
        targetSdk = 35
        versionCode = 53
        versionName = "0.53.0"
    }
    signingConfigs.getByName("debug") {
        storeFile = rootProject.file("signing/alico-update.jks")
        storePassword = System.getenv("ALICO_KEYSTORE_PASSWORD")
        keyAlias = System.getenv("ALICO_KEY_ALIAS") ?: "alico"
        keyPassword = System.getenv("ALICO_KEY_PASSWORD")
    }
    // v12-v16 files may remain after uploading a ZIP over the repository.
    // The v11-based UI uses ImportedCatalog and WebPlayerActivity instead.
    sourceSets.getByName("main").java.exclude(
        "**/CatalogParser.java",
        "**/CatalogActivity.java"
    )
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("org.jsoup:jsoup:1.18.3")
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.6.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.6.1")
    implementation("androidx.media3:media3-ui:1.6.1")
}
