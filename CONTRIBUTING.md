# Contributing to Raygun4Android

## Project and library organisation

Building the project requires Android Studio 2022.3.1 and Gradle 8.

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

## How to contribute?
Please fork the main repository from https://github.com/MindscapeHQ/raygun4android into your own GitHub account.

Create a local branch off `develop` in your fork, named so that it explains the work in the branch, and submit a pull request against the main repositories' `develop` branch. Even better, get in touch with us here on Github before you undertake any work so that it can be coordinated with what we're doing.

If you're interested in contributing on a regular basis, please get in touch with the Raygun team.
