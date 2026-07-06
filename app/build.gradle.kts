plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import org.gradle.api.tasks.compile.AbstractCompile

fun String.toBuildConfigString(): String = "\"" + this.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val libreApiBaseUrl = (System.getenv("LIBRE_API_BASE_URL") ?: "https://api-eu.libreview.io").trim().ifBlank { "https://api-eu.libreview.io" }
val libreLinkUpVersion = (System.getenv("LIBRE_LINKUP_VERSION") ?: "4.17.0").trim().ifBlank { "4.17.0" }
val librePatientId = (System.getenv("LIBRE_PATIENT_ID") ?: "").trim()

android {
    namespace = "com.libredisplay"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.libredisplay"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "LIBRE_API_BASE_URL", libreApiBaseUrl.toBuildConfigString())
        buildConfigField("String", "LIBRE_LINKUP_VERSION", libreLinkUpVersion.toBuildConfigString())
        buildConfigField("String", "LIBRE_PATIENT_ID", librePatientId.toBuildConfigString())
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }


    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.splashscreen)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    debugImplementation(libs.androidx.ui.tooling)
}

tasks.withType<AbstractCompile>().configureEach {
    exclude(
        "**/data/api/v3/**",
        "**/data/api/v2/**"
    )
}
