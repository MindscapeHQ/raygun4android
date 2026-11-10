# Releasing Raygun4Android

Official development or production releases of Raygun4Android are usually done by the Raygun team. This file documents the setup needed to do snapshot and production releases.

The release process is configured in `provider/build.gradle.kts` which uses the `com.vanniktech.maven.publish` plugin.

Plugin documentation can be found here: https://vanniktech.github.io/gradle-maven-publish-plugin/central/

## Preparation

Create a local `gradle.properties` file in your home directory or add to an already existing one. The default is `<HOME>/.gradle/gradle.properties`. It is important that the content of this file does not get added to the repository. The file specified in the `secretKeyRingFile` property should also never be added to and shared in a repository.

The structure to be added is:

```
mavenCentralUsername={to be provided}
mavenCentralPassword={to be provided}
signing.keyId={to be provided}
signing.password={to be provided}
signing.secretKeyRingFile={to be provided}
```

All keys have to populated with the appropriate values and file paths to allow a successful publication on Maven Central. These values will be provided to people with the appropriate level of access by the Raygun team.

`mavenCentralUsername` and `mavenCentralPassword` must come from a [Central Portal user token](https://central.sonatype.org/publish/generate-portal-token/), not your website login. The account must have publishing access to the `com.raygun` namespace. Reuse the Raygun release signing key where available; keep the private key and credentials out of version control and chat messages.

## Publish a build

1. Edit `gradle.properties` in the root of the project and change the version name and code according to the guidelines in the file. If you add `-SNAPSHOT` to the version name, your release will be published to the snapshot server of Maven Central. It might be appropriate to also update the fields for `POM_DEVELOPER_ID` and `POM_DEVELOPER_NAME` in certain instances. Please check with the Raygun team if you feel the need to do so.

```
VERSION_NAME=4.0.0-alpha1-SNAPSHOT
VERSION_CODE=40000000
```

2. Copy the signing key ring into the /releasing directory of your project and refer to it from `signing.secretKeyRingFile` in the `gradle.properties` of your home directory. If you need to create a new signing key please see below.

## Beta releases

Maven Central publishes beta versions through the same process as final releases. For the first 7.0.0 beta, use:

```properties
VERSION_NAME=7.0.0-beta1
VERSION_CODE=70000071
```

Do not append `-SNAPSHOT`: that would publish to the separate snapshot repository. A published beta is immutable. Use `7.0.0-beta2` and version code `70000072` for a subsequent beta instead of trying to replace beta 1.

Run the build and published-consumer checks against the beta version before following [Steps for releasing the provider](#steps-for-releasing-the-provider). With the current configuration, `:provider:publishToMavenCentral` uploads a deployment for validation; the deployment is not public until you select **Publish** in the Central Portal.

After publication, verify that the beta resolves from `mavenCentral()`. Consumers opt in with the exact version:

```kotlin
implementation("com.raygun:raygun4android:7.0.0-beta1")
```

Keep 6.3.1 documented as the stable release while 7.0.0 is in beta. Tag the published source commit as `v7.0.0-beta1` and mark its GitHub release as a **pre-release**, not the latest stable release.

## Creating new signing keys

1. Install GPG

2. Create a new key with your name, email address and a secure passphrase. Make sure you store the passphrase in a secure vault for future use:
```
$ gpg --gen-key
```

3. Upload the public key to a keyserver:
```
$ gpg --keyserver keyserver.ubuntu.com --send-keys <keyId>
gpg: sending key <keyId> to hkp://keyserver.ubuntu.com
```

4. To export the secring file needed to sign the artifacts, run the following command:
```
$ gpg --export-secret-keys -o secring.gpg
```

5. Put the exported secring file in your local project. To view the `keyId` required for signing, use the following command:
```
$ gpg --list-keys --keyid-format 0xSHORT
```

## Steps for releasing the provider
1. Build the provider for release and upload it to the Central Portal by running the following command in the terminal:
```
./gradlew clean :provider:build :provider:publishToMavenCentral
```

2. Login to the [Maven Central Deployments](https://central.sonatype.com/publishing/deployments).
3. Select the deployment named `com.raygun:raygun4android` in the list of deployments.
4. Release the artifacts by clicking the **Publish** button.
5. The artifact status will change to **Publishing**, it will take between 10 to 30 minutes for the artifacts to be available in Maven Central.

## Maven local testing

To release the provider to Maven local (i.e. your local maven repository), build the provider for release and publish it locally by running the following command in the terminal (notice the command name):
```
./gradlew clean :provider:build :provider:publishToMavenLocal
```

Then verify that the package exists in your Maven local folder, e.g. `/home/<user>/.m2/`.

You can now test the local package distribution locally by changing the following:

1. Add `mavenLocal()` to the root `build.gradle.kts`:

```kotlin
allprojects {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}
```

2. Change the Raygun dependency in `app/build.gradle.kts`, where `x.y.z` is the newly built version:

```kotlin
dependencies {
    implementation("com.raygun:raygun4android:x.y.z")
}
```
