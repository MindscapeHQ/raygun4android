# Releasing Raygun4Android

Official development or production releases of Raygun4Android are usually done by the Raygun team. This file documents the setup needed to do snapshot and production releases.

The release process uses a modified version of Chris Banes' library for pushing to Maven with Gradle: https://github.com/chrisbanes/gradle-mvn-push. Please check out Chris' documentation before reading further.

## Preparation

Create a local `gradle.properties` file in your home directory or add to an already existing one. The default is `<HOME>/.gradle/gradle.properties`. It is important that the content of this file
does not get added to the repository. The file specified in the `secretKeyRingFile` property should also never be add to and shared in a repository.

The structure to be added is:

```
NEXUS_USERNAME={to be provided}
NEXUS_PASSWORD={to be provided}
signing.keyId={to be provided}
signing.password={to be provided}
signing.secretKeyRingFile={to be provided}
```

All keys have to populated with the appropriate values and file paths to allow a successful publication on Maven Central. These values will be provided to people with the appropriate level 
of access by the Raygun team.

## Publish a build

1. Edit `gradle.properties` in the root of the project and change the version name and code according to the guidelines in the file. If you add `-SNAPSHOT` to the version name, your release will be published to the snapshot server of Maven Central. It might be appropriate to also update the fields for `POM_DEVELOPER_ID` and `POM_DEVELOPER_NAME` in certain instances. Please check with the Raygun team if you feel the need to do so.

```
VERSION_NAME=4.0.0-alpha1-SNAPSHOT
VERSION_CODE=40000000
```

2. Copy the signing key into the /releasing directory of your project and refer to it from `signing.secretKeyRingFile` in the `gradle.properties` of your home directory. If you need to create a new signing key please see below.

## Creating new signing keys

1. Go to https://gpgtools.org/ and download the tool suite.

2. Create a new key with the following details:

````
NAME: {to be provided}
EMAIL: {to be provided}
PASSWORD: {to be provided}
````

3. Click generate key and do upload the public key. To export the secring file needed to sign the archives run the following command:

````
gpg --export-secret-keys -o secring.gpg
````

4. Put the exported secring file in your local raygun4android project. To view the KeyId needed for signing, use the following command:

````
gpg --list-keys --keyid-format 0xSHORT
````

## Nexus Repository Manager OSS

We make use of the Public & Staging Repositories hosted by Nexus. We do not need to host and maintain our own copy of the Nexus Repository Manager.

Go to https://oss.sonatype.org and login with:

````
USERNAME: {to be provided}
PASSWORD: {to be provided}
````

To view our currently uploaded public artifacts go to:

````
Repositories -> User Managed Repositories -> Public Repositories -> Browse Storage, then unfold the tree to com/raygun/raygun4android/{version}
````

You can view more details by opening the right hand side menu.

The Nexus Repository (oss.sonatype.org) pulls directory information from the same LDAP source that backs the issues JIRA for Sonatype.

To view or change the user details go to https://issues.sonatype.org/secure/ViewProfile.jspa and login with:

````
USERNAME: {to be provided}
PASSWORD: {to be provided}
````

### Understanding the Build Promotion

More information can be found [here](https://help.sonatype.com/repomanager2/staging-releases/configuring-the-staging-suite)

#### Staging Profile

Details the repositories the artifacts are sent to throughout the staging process.
Our staging profile is named **com.raygun**

#### Staging Repositories

A temp staging repository will be created [here](https://oss.sonatype.org/content/groups/staging/com/raygun/raygun4android/) when we deploy.

#### Staging Ruleset

We do not have a custom staging ruleset.

#### Staging Upload

Here you can manually upload artifacts.

## Steps for releasing the provider
1. Build the provider for release and upload it to Nexus by running the following command in the terminal:

    ````
    ./gradlew clean :provider:build :provider:uploadArchives
    ````

2. Login to the [Nexus Repository Manager](https://oss.sonatype.org) and go to the **Staging Repositories**.
3. Locate the repository named 'comraygun-100*' near the bottom.
4. Mark this repository as **closed** by clicking the **Close** button.
5. The repository will sent to the Target Groups defined in the **Staging Profiles** (for us it's **Staging**).
6. Test the artifacts that are now in our staging target group (**Staging**).
7. Release the artifacts by clicking the **Release** button on our staging repository (which we marked as **closed**).
8. Artifacts are sent to the Release repository defined in the staging profile.
9. The temp staging repository will be automatically deleted.
10. Artifacts will take a few days to be made available to clients and should be listed in the [public repositories](https://oss.sonatype.org/content/repositories/public/com/raygun/raygun4android/) first and on [mvnrepository.com](https://mvnrepository.com/artifact/com.raygun/raygun4android) eventually.



