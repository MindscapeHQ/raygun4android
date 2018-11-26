# Contributing to Raygun4Android

## Project and library organisation

Building the project at this stage requires Android Studio 3.2+ and Java 8 or newer.

The project consists of two modules:

/app: A sample app demonstrating the use of Raygun4Android with a dependency to the "provider" module of the project.

/provider: Android library project that contains the actual Raygun4Android provider.

## Building

### Building from Android Studio

Setup the app or provider module in Run - Edit Configurations. Select the module in the UI and build by pressing the green triangle button next to it.

Building the app module will also resolve the dependency to the provider module.

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
./gradlew clean :provider:assembleDebug
./gradlew clean :provider:assembleRelease
````

## How to contribute?
Please fork the main repository from https://github.com/MindscapeHQ/raygun4android into your own GitHub account.

Create a local dev branch (named so that it explains the work in the branch), and submit a pull request against the main repository. Even better, get in touch with us here on Github before you undertake any work so that it can be coordinated with what we're doing.

If you're interested in contributing on a regular basis, please get in touch with the Raygun team.
