// FlowBoard Root Project Configuration
// This file allows opening the entire monorepo in Android Studio
// while keeping android and backend as independent modules

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "FlowBoard"

// Include Android app module
includeBuild("android")

// Note: Backend is a separate Ktor project and should be opened independently
// To work on backend, open the 'backend' folder in IntelliJ IDEA
