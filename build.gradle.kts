buildscript {
    repositories {
        google()
        mavenCentral()
    }
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
    dependencies {
        classpath(libs.kotlin.gradle) // pins KGP 2.3.0 (higher than AGP 9's bundled 2.2.10)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.maven.central.publish) apply false
    alias(libs.plugins.spotless)
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    dependencyLocking {
        lockAllConfigurations()
    }
}

tasks.register("resolveAndLockAll") {
    group = "verification"
    description = "Resolves build, test, lint, and formatting dependencies used to generate lockfiles."

    dependsOn(
        "spotlessCheck",
        "app:lint",
        "provider:lint",
        "app:assembleDebug",
        "provider:assembleDebug",
        "app:assembleDebugAndroidTest",
        "provider:test",
    )
}

spotless {
    java {
        target("**/*.java")
        googleJavaFormat().aosp().reflowLongStrings(true)
        removeUnusedImports()
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }
    kotlin {
        target("**/*.kt")
        ktfmt().googleStyle().configure {
            it.setBlockIndent(4)
            it.setContinuationIndent(4)
        }
        ktlint()
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint().editorConfigOverride(
            mapOf("ktlint_standard_property-naming" to "disabled"),
        )
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }
    format("misc") {
        target("**/*.md", "**/.gitignore")
        leadingTabsToSpaces()
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("xml") {
        target("**/*.xml")
        targetExclude(".idea/**/*.xml")
        leadingTabsToSpaces()
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("yml") {
        target("**/*.yml")
        leadingTabsToSpaces()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
