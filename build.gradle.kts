import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage

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
        "app:assembleRelease",
        "app:bundleRelease",
        "provider:assembleRelease",
        "provider:publishToMavenLocal",
        "provider:test",
    )
}

val okHttpAlignmentTest =
    configurations.create("okHttpAlignmentTest") {
        isCanBeConsumed = false
        isCanBeResolved = true
        resolutionStrategy.deactivateDependencyLocking()
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        }
    }

dependencies {
    okHttpAlignmentTest(
        project(mapOf("path" to ":provider", "configuration" to "debugRuntimeElements")),
    )
    okHttpAlignmentTest("com.squareup.okhttp3:okhttp-urlconnection:4.9.2") {
        isTransitive = false
    }
}

tasks.register("verifyOkHttpDependencyAlignment") {
    group = "verification"
    description = "Verifies that the provider aligns OkHttp modules in a consumer."
    val resolutionResult = okHttpAlignmentTest.incoming.resolutionResult

    doLast {
        val okHttpVersions =
            resolutionResult.allComponents
                .mapNotNull { it.moduleVersion }
                .filter { it.group == "com.squareup.okhttp3" }
                .associate { it.name to it.version }

        val coreVersion = checkNotNull(okHttpVersions["okhttp"]) { "okhttp was not resolved" }
        check(okHttpVersions["okhttp-urlconnection"] == coreVersion) {
            "Expected okhttp-urlconnection $coreVersion, resolved ${okHttpVersions["okhttp-urlconnection"]}"
        }
    }
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
