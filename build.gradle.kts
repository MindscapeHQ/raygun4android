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

val versionName = providers.gradleProperty("VERSION_NAME").get()

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

val okHttpAlignmentTestRepository =
    layout.buildDirectory.dir("okhttp-alignment-test-repository")
val prepareOkHttpAlignmentTestRepository =
    tasks.register<Sync>("prepareOkHttpAlignmentTestRepository") {
        dependsOn(
            ":provider:bundleReleaseAar",
            ":provider:generateMetadataFileForMavenPublication",
            ":provider:generatePomFileForMavenPublication",
        )

        into(
            okHttpAlignmentTestRepository.map {
                it.dir("com/raygun/raygun4android/$versionName")
            },
        )
        from("provider/build/outputs/aar/raygun4android.aar") {
            rename("raygun4android.aar", "raygun4android-$versionName.aar")
        }
        from("provider/build/publications/maven/module.json") {
            rename("module.json", "raygun4android-$versionName.module")
        }
        from("provider/build/publications/maven/pom-default.xml") {
            rename("pom-default.xml", "raygun4android-$versionName.pom")
        }
    }

tasks.register<Exec>("verifyOkHttpDependencyAlignment") {
    group = "verification"
    description = "Verifies that the published provider aligns OkHttp modules in a consumer."
    dependsOn(prepareOkHttpAlignmentTestRepository)

    commandLine(
        rootProject.file("gradlew").absolutePath,
        "-p",
        rootProject.file("integration-tests/okhttp-alignment").absolutePath,
        "verifyOkHttpAlignment",
        "-PraygunRepository=${okHttpAlignmentTestRepository.get().asFile.toURI()}",
        "-PraygunVersion=$versionName",
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
