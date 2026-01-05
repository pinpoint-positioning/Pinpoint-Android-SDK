import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        //Add this repo for Pinpoint dependencies

        maven {
            url = uri("https://maven.pkg.github.com/pinpoint-positioning/Android-Demo-App")
            name = "GitHub"
            credentials {
                username = "grait"
                password = "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"
            }
        }
    }
}

rootProject.name = "Pinpoint Android Demo App"
include(":app")