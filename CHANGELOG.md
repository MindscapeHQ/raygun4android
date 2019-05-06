## Changelog

- Towards v4.0.0-beta3:
  - API changes for setting and working with custom data: the field userCustomData is now being referred to as customData. This is reflected in API method changes.
  - Documentation for both Raygun4Android Gradle tasks
  - Added Timber 4.7.1 dependency for debug/prod logging

- v4.0.0-beta2: This is a major release and has a variety of breaking changes, depending on your use cases. This is an overview of the most important and visible changes:

  - The Maven groupID for loading RG4A has changed to com.raygun.
  - The build process now uses Gradle, the Maven build process has been removed without replacement.
  - Project setup optimised for Android Studio 3.1+:
    - app module build sample app
    - provider module is an Android Library project and creates .aar artifacts
  - Various improvements to sample app
  - The background services are now a JobIntentService to better deal with background limitations in Android 8+. This will behave as a Service on Android 7 and earlier devices.
  - Sending POST data to the Raygun backend has been refactored to using Okhttp3. RG4A therefore has a new implementation dependency on this library.
  - Build requirements have been updated to compileSDK 28 and minSDK 16+.
  - There is a new convenience constructor on RaygunUserInfo to create a user object that only has an identifier.
  - All deprecated functionality from 3.x has been removed.
  - Custom endpoints are supported now.
  - You can change the default of 64 reports being stored to a lower value now.
  - General API cleanup and changes to Pulse -> all renamed RUM now
  - Removed uuid field from RaygunUserInfo
  - You have to enable individual products now before being able to use them.
  - Changes to RUM and Pulse APIs
  - The post() functions in RaygunClient have been removed.
  
- v3.0.6: Fixed null reference exception when building crash report messages. We now send messages using a JobIntentService to improve compatibility with the latest Android versions. IMPORTANT: Support for API versions 9 to 15 has been discontinued from this release due to that. 

- v3.0.5: Fixing timing issue from ConnectivityManager in RaygunPostService (#41), adding null checks around intent extras being null that should never be null (XRAY-1898)

- v3.0.4: Fixing NPE in RaygunPostService (#34, PR #37); RaygunErrorMessage can now accept any Throwable (PR #33); Pulse now checks for connectivity (PR #38)

- v3.0.3: Bug fix: removing println from code (PR #36)

- v3.0.2: Allowing all properties of the RaygunMessageDetails to be modifiable during the onBeforeSend callback.

- v3.0.1: Update sample app in repository to remove usage of deprecated methods on RaygunClient

- v3.0.0: Add support for Pulse for Mobile (automatic network call tracking etc)

- v2.1.1: Added MY_PACKAGE_REPLACED intent receiver and guard against spurious null Context seen on some devices when checking net connection

- v2.1.0: Add OnBeforeSend implementation; expose setGroupingKey

- v2.0.0: Replace deprecated Apache HTTP library with HttpUrlConnection; change packaging format to AAR for Android Studio/Gradle compatibility

- v1.3.0: Provide device network connectivity state under Request section; aAdded RaygunClient.SetTags() and SetUserCustomData() to provide objects that will be attached to all exceptions

- v1.2.1: Fix: only distinct device IPs are transmitted

- v1.2.0: Added device IP data (shown in the 'Request' tab in the Raygun dashboard)

- v1.1.0: Add new user info properties, bump Gson to 2.2.4

- v1.0.5: Guard against a spurious NullPointerException caused by the posting service

- v1.0.4: JSON payload now encoded in UTF-8, fixes issues with post-ASCII chars (e.g Cyrillic) were displayed as '?' in Raygun dashboard

- v1.0.3: Improved version tracking support

- v1.0.2: Added SetUser method for unique user tracking.

- v1.0.1: Added caching of messages to disk when network unavailable & post them when it becomes available again; several bug fixes relating to the posting service. This version is recommended; do not use 1.0.0.

- v1.0.0: Completed initial version with background service for performing posting, API key read from AndroidManifest and more.

- v0.0.1: Initial release with basic functionality.
