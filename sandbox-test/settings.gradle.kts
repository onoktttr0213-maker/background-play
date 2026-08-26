// Minimal, Android-SDK-free build root used to compile and unit-test the pure-Kotlin
// game engine that lives in android/core. This exists because the sandbox environment
// used to develop this project has no Android SDK available (network to dl.google.com
// is blocked), so the full android/ Gradle project (which needs the SDK for :app)
// cannot be built here. This root reuses the exact same source files via projectDir,
// so there is no duplicated engine code to keep in sync.
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "logicpunch-sandbox-test"

include(":core")
project(":core").projectDir = file("../android/core")
