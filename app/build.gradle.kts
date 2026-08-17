plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
}

import org.gradle.api.tasks.compile.AbstractCompile
import java.util.Properties

fun String.toBuildConfigString(): String = "\"" + this.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val libreApiBaseUrl = (System.getenv("LIBRE_API_BASE_URL") ?: "https://api-eu.libreview.io").trim().ifBlank { "https://api-eu.libreview.io" }
val libreLinkUpVersion = (System.getenv("LIBRE_LINKUP_VERSION") ?: "4.17.0").trim().ifBlank { "4.17.0" }
val librePatientId = (System.getenv("LIBRE_PATIENT_ID") ?: "").trim()
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun propertyOrEnv(name: String): String? {
    return providers.gradleProperty(name).orNull
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: localProperties.getProperty(name)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        ?: System.getenv(name)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
}

val releaseStoreFile = propertyOrEnv("RELEASE_STORE_FILE")
val releaseStorePassword = propertyOrEnv("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = propertyOrEnv("RELEASE_KEY_ALIAS")
val releaseKeyPassword = propertyOrEnv("RELEASE_KEY_PASSWORD")
val releaseSigningProps = mapOf(
    "RELEASE_STORE_FILE" to releaseStoreFile,
    "RELEASE_STORE_PASSWORD" to releaseStorePassword,
    "RELEASE_KEY_ALIAS" to releaseKeyAlias,
    "RELEASE_KEY_PASSWORD" to releaseKeyPassword
)
val missingReleaseSigningProps = releaseSigningProps.filterValues { it.isNullOrBlank() }.keys.sorted()
val releaseStoreFilePath = releaseStoreFile?.let { rootProject.file(it) }
val hasReleaseSigningConfig = missingReleaseSigningProps.isEmpty() && releaseStoreFilePath?.exists() == true

android {
    namespace = "com.libredisplay"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.libredisplay"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "LIBRE_API_BASE_URL", libreApiBaseUrl.toBuildConfigString())
        buildConfigField("String", "LIBRE_LINKUP_VERSION", libreLinkUpVersion.toBuildConfigString())
        buildConfigField("String", "LIBRE_PATIENT_ID", librePatientId.toBuildConfigString())

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigningConfig) {
                storeFile = releaseStoreFilePath
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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
        unitTests.isIncludeAndroidResources = true
        animationsDisabled = true
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
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.room.runtime)
    androidTestImplementation(libs.androidx.room.ktx)
    debugImplementation(libs.androidx.ui.tooling)
}

tasks.configureEach {
    if (name in listOf("assembleRelease", "bundleRelease", "packageRelease", "signReleaseBundle", "signReleaseBundle", "packageReleaseBundle")) {
        doFirst {
            if (missingReleaseSigningProps.isNotEmpty()) {
                throw GradleException(
                    "Release signing is not configured. Missing properties: ${missingReleaseSigningProps.joinToString(", ")}. " +
                        "Provide them via gradle.properties, local.properties, or environment variables."
                )
            }
            if (releaseStoreFilePath?.exists() != true) {
                throw GradleException(
                    "Release signing is not configured. Keystore file not found: ${releaseStoreFilePath?.absolutePath ?: "(RELEASE_STORE_FILE not set)"}."
                )
            }
        }
    }
}

tasks.withType<AbstractCompile>().configureEach {
    exclude(
        "**/data/api/v3/**",
        "**/data/api/v2/**"
    )
}
