# Contributing to Raygun4Android

## Project and library organisation

Building the project requires Android Studio Panda 4 (2025.3.4) or later, Gradle 9.5.1, and JDK 17 or later. The minimum Studio version is driven by the AGP 9.2.x requirement.

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

## How to contribute?
Please fork the main repository from https://github.com/MindscapeHQ/raygun4android into your own GitHub account.

Create a local branch off `develop` in your fork, named so that it explains the work in the branch, and submit a pull request against the main repositories' `develop` branch. Even better, get in touch with us here on Github before you undertake any work so that it can be coordinated with what we're doing.

If you're interested in contributing on a regular basis, please get in touch with the Raygun team.
