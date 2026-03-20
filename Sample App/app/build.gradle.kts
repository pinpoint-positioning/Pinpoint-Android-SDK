import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val easylocateVersion = "12.2.0"

plugins {
    alias(libs.plugins.android.application) version "8.8.0"
    alias(libs.plugins.kotlin.android) version "2.3.20"
    alias(libs.plugins.kotlin.compose) version "2.3.20"
}


android {
    namespace = "de.pinpoint.android_demo_app"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.pinpoint.android.demo.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "12.2"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
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
    implementation("de.pinpoint.android:core:$easylocateVersion")
    implementation("de.pinpoint.android:sdk:$easylocateVersion")

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