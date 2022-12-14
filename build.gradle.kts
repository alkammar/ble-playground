plugins {
    id("com.android.application") version "7.1.3" apply false
    id("com.android.library") version "7.1.3" apply false
    id("org.jetbrains.kotlin.android")  version "1.6.21" apply false
    id("org.jetbrains.kotlin.jvm") version "1.6.21" apply false
}

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.42")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.5.3")
    }
}

tasks {
    register("clean", Delete::class) {
        delete(rootProject.buildDir)
    }
}