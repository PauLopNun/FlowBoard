// FlowBoard Root Build Configuration
// This is a composite build that includes the Android app

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

// Root project doesn't have any specific tasks
// All Android-specific tasks are delegated to the android/ subproject

tasks.register("clean") {
    description = "Clean all subprojects"
    group = "build"

    doLast {
        println("Cleaning FlowBoard workspace...")
        delete(rootProject.layout.buildDirectory)
    }
}

// Convenience tasks for common operations
tasks.register("assembleDebug") {
    description = "Build debug APK"
    group = "build"
    doLast {
        println("Use: ./gradlew -p android assembleDebug")
        println("Or open the 'android' folder directly in Android Studio")
    }
}

tasks.register("assembleRelease") {
    description = "Build release APK"
    group = "build"
    doLast {
        println("Use: ./gradlew -p android assembleRelease")
        println("Or open the 'android' folder directly in Android Studio")
    }
}
