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
            url = uri("https://gitlab.com/api/v4/projects/26571989/packages/maven")
        }
    }
}

rootProject.name = "Pinpoint Android Demo App"
include(":app")