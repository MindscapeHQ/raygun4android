buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9 otherwise silently upgrades the compiler to its bundled Kotlin version.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin") {
            version { strictly("2.1.20") }
        }
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin-api") {
            version { strictly("2.1.20") }
        }
    }
}

plugins {
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
}
