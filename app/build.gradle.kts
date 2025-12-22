val easylocateVersion = "12.2.0-gamma"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}


android {
    namespace = "de.pinpoint.android_demo_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.pinpoint.android.demo.app"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "12.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        // Adding custom git info for version display
        val gitBranch = "git rev-parse --abbrev-ref HEAD".runCommand() ?: "unknown"
        val gitCommit = "git rev-parse --short HEAD".runCommand() ?: "unknown"

        buildConfigField("String", "GIT_BRANCH", "\"$gitBranch\"")
        buildConfigField("String", "GIT_COMMIT", "\"$gitCommit\"")
        // Adding Easylocate Version to BuildConfig
        buildConfigField("String", "EASYLOCATE_VERSION", "\"$easylocateVersion\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Helper function to get git info
fun String.runCommand(): String? = try {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
        .directory(File("."))
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
    proc.inputStream.bufferedReader().readText().trim().takeIf { it.isNotEmpty() }
} catch (e: Exception) {
    null
}


dependencies {
    implementation("de.easylocate:core:$easylocateVersion")
    implementation("de.easylocate:android-sdk:$easylocateVersion")

    implementation(platform(libs.androidx.compose.bom.v20240600))
    // Activity Compose
    implementation(libs.androidx.activity.compose.v182)

    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation("androidx.core:core-ktx:1.3.2")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}