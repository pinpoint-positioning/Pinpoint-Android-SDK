import java.io.FileInputStream
import java.util.Properties

val pinpointSdkVersion = "15.0.0"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {

    namespace = "de.pinpoint.android_example_app"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.pinpoint.android.example.app"
        minSdk = 29
        targetSdk = 37
        versionCode = 2
        versionName = "15.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        // Adding custom git info for version display
        val gitBranch = "git rev-parse --abbrev-ref HEAD".runCommand() ?: "unknown"
        val gitCommit = "git rev-parse --short HEAD".runCommand() ?: "unknown"

        buildConfigField("String", "GIT_BRANCH", "\"$gitBranch\"")
        buildConfigField("String", "GIT_COMMIT", "\"$gitCommit\"")
        buildConfigField("String", "PINPOINT_SDK_VERSION", "\"$pinpointSdkVersion\"")
        buildConfigField(
            "String",
            "PINPOINT_API_KEY",
            "\"${localProperties.getProperty("PINPOINT_API_KEY")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileSdkMinor = 0
}

kotlin {
    jvmToolchain(17)
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
    implementation("de.pinpoint.android:sdk:$pinpointSdkVersion")
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
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