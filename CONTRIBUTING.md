# Contributing to Raygun4Android

## Project and library organisation

Building the project requires Android Studio Panda 4 (2025.3.4) or later, Gradle 9.6.1, and JDK 17 or later. The minimum Studio version is driven by the AGP 9.2.x requirement.

The project consists of two modules:

`:app`: A sample app demonstrating the use of Raygun4Android with a dependency to the `:provider` module of the project.

`:provider`: Android library project that contains the actual Raygun4Android provider.

## Building

### Building from Android Studio

Setup the `:app` or `:provider` module in Run - Edit Configurations. Select the module in the UI and build by pressing the green triangle button next to it.

Building the `:app` module will also resolve the dependency to the `:provider` module.

`build.gradle` in the `"app` module defines which version of the library is being used (replace `<version>` with the library version you'd like to use):

````
// Build from local provider library src
implementation project(':provider')

// Build from external repo
//implementation 'com.raygun:raygun4android:<version>'
````

### Building from command-line with Gradle

When building through the terminal inside of Android Studio and you receive the error message:

````
bash: ./gradlew: Permission denied
````

run the following command in the terminal to enable permissions (Mac/Linux):

````
chmod u+x gradlew
````

To generate the library locally run in the terminal:

````
./gradlew clean :provider:build
````

## Updating Gradle dependencies

The build uses Gradle dependency verification and dependency locking. When changing Gradle, plugin, or library dependency versions, update the checked-in verification metadata and lockfiles as part of the same change.

The root `resolveAndLockAll` task is the deliberately maintained lock and verification surface for the build. Both dependency lockfiles (`--write-locks`) and verification metadata (`--write-verification-metadata sha256`) only cover configurations resolved by this task. When adding a new module, variant, or configuration that should be locked and verified, add the relevant task to `resolveAndLockAll` in the root `build.gradle.kts`.

To refresh dependency lockfiles, run:

````
./gradlew resolveAndLockAll --write-locks
./gradlew buildEnvironment --write-locks
````

To refresh verification checksums after dependency changes, run the verification tasks with metadata writing enabled:

````
./gradlew --write-verification-metadata sha256 resolveAndLockAll
````

Then validate the normal build path before opening a pull request:

````
./gradlew --no-daemon spotlessCheck provider:test app:assembleDebug provider:assembleDebug
````

## Published Kotlin compatibility

The Kotlin Gradle plugin used to build Raygun4Android is independent of the Kotlin language, API, metadata, and standard library versions exposed to consumers. The build toolchain can move forward without requiring applications that use the SDK to upgrade their Kotlin compiler.

Raygun4Android 6.2.3 exposed why that separation matters. Updating the build to Kotlin Gradle plugin 2.4.10 also made the published artifact declare `kotlin-stdlib` 2.4.10 and emit Kotlin 2.4 metadata. Gradle selected that standard library in React Native 0.81 applications, but their Kotlin 2.1.20 compiler can only read metadata through 2.2. The applications then failed to compile.

The `:provider` module therefore targets Kotlin language and API version 2.2 and publishes a `kotlin-stdlib` 2.2.21 dependency, while the project can use a newer Kotlin Gradle plugin. Kotlin/JVM explicitly allows a compiler from the previous language version, so Kotlin 2.1 consumers can read Kotlin 2.2 library metadata.

Follow these rules when changing Kotlin, AGP, the publishing plugin, or provider dependencies:

- Do not automatically raise `coreLibrariesVersion`, `apiVersion`, or `languageVersion` in `provider/build.gradle.kts` with the build toolchain.
- Treat raising the published versions as a consumer compatibility and release decision. Verify the oldest supported Kotlin compiler before changing them.
- Refresh dependency locks and verification metadata, then inspect the generated POM and Gradle module metadata rather than relying only on an in-project build.
- Keep the `Kotlin 2.1 consumer` CI job green. It compiles a separate Android application with Kotlin 2.1.20 against the generated Maven publication, exercising both dependency and class metadata.
- Do not hide compatibility failures with `-Xskip-metadata-version-check`, forced standard library versions, or consumer-side dependency exclusions.

The consumer fixture is in `integration-tests/kotlin-2.1-consumer`. Its Gradle and Kotlin versions intentionally represent the oldest supported React Native toolchain and should only be raised as an explicit compatibility decision.

## How to contribute?
Please fork the main repository from https://github.com/MindscapeHQ/raygun4android into your own GitHub account.

Create a local branch off `develop` in your fork, named so that it explains the work in the branch, and submit a pull request against the main repositories' `develop` branch. Even better, get in touch with us here on Github before you undertake any work so that it can be coordinated with what we're doing.

If you're interested in contributing on a regular basis, please get in touch with the Raygun team.
