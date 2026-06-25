import java.util.Properties
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.library") version "9.1.1"
        id("org.jetbrains.kotlin.android") version "2.3.20"
    }
}


val localProperties = Properties().apply {
    val localPropertiesFile = File(rootDir, "local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val repoUser = localProperties.getProperty("PINPOINT_USER")
val repoPassword = localProperties.getProperty("PINPOINT_PASSWORD")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()

        maven {
            url = uri("https://posie.pinpoint.de:8073/repository/android_sdk_relase/")
            credentials {
                username = repoUser
                password = repoPassword
            }
        }
    }
}

rootProject.name = "Pinpoint Android SDK"
include(":app")

