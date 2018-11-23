# Releasing Raygun4Android

Official development or production releases of Raygun4Android are usually done by the Raygun team. This file documents the setup needed to do snapshot and production releases.

The release process uses Chris Banes' library for pushing to Maven with Gradle: https://github.com/chrisbanes/gradle-mvn-push. Please check out Chris' documentation before reading further.

## Preparation

Create a local `gradle.properties` file in your home directory or add to an existing one. The default is `<HOME>/.gradle/gradle.properties. It is important that the content of this file
does not get added to any repository. The file specified in the secretKeyRingFile property should also never be add to and shared in a repository.

The structure to be added is:

```
NEXUS_USERNAME=
NEXUS_PASSWORD=
signing.keyId=
signing.password=
signing.secretKeyRingFile=
```

All keys have to populated with the appropriate values and file paths to allow a successful publication on Maven Central. These values will be provided to people with the appropriate level 
of access by the Raygun team.

## Publish a build

1. Edit `gradle.properties` in the root of the project and change the version name and code. If you add -SNAPSHOT to the version name, your release will be published to the snapshot server of Maven Central.

```
VERSION_NAME=4.0.0-ALPHA1-SNAPSHOT
VERSION_CODE=40000000
```

It might be appropriate to also update the fields for POM_DEVELOPER_ID and POM_DEVELOPER_NAME in certain instances. Please check with the Raygun team if you feel the need to do so.

2. Run `gradle clean build uploadArchives`.

