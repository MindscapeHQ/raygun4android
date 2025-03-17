## Changelog

### 4.2.0

- feat(provider): #70 custom OkHttpClient (#150) (2025-03-11)
- fix(provider): Support message payloads larger than 10KB (#151) (2025-03-18)
- fix(provider): missing archives and bump version to 4.1.1 (#149) (2025-03-11)
- docs: Update README.md (2025-03-12)
- docs: Update CHANGELOG.md (2025-03-11)

### v4.1.1

- feat: RUM support for Fragments (#131) (2025-01-17)
- feat: Migrating from IntentService to WorkManager (#133) (2025-01-16)
- feat: new public method to allow sending simple strings (#122) (2024-10-14)
- Quick fix for #94. (2024-09-06)
- chore(deps): bump com.android.tools.build:gradle from 8.7.3 to 8.8.2 (#146) (2025-03-05)
- chore(deps): bump org.jetbrains.kotlin.android from 1.8.22 to 2.1.10 (#144) (2025-03-05)
- chore(deps): bump com.diffplug.spotless from 6.25.0 to 7.0.2 (#143) (2025-03-05)
- chore(deps): bump androidx.activity:activity from 1.10.0 to 1.10.1 (#145) (2025-03-02)
- chore(deps): bump androidx.constraintlayout:constraintlayout (#142) (2025-03-02)
- chore(deps): bump androidx.work:work-runtime from 2.9.1 to 2.10.0 (#138) (2025-02-02)
- chore(deps): bump androidx.activity:activity from 1.8.0 to 1.10.0 (#139) (2025-02-02)
- chore(deps): bump com.google.code.gson:gson from 2.11.0 to 2.12.1 (#141) (2025-02-02)
- chore(deps): bump org.jetbrains.kotlin:kotlin-gradle-plugin (#137) (2025-02-02)
- chore(deps): bump mockito from 3.11.2 to 5.2.0 (#126) (2025-01-31)
- chore(deps): bump org.jetbrains.kotlin:kotlin-gradle-plugin (#128) (2025-01-31)
- chore: Code formatting in provider sourcecode (#130) (2024-12-12)
- chore: Project upgrades to run on Android Studio 2024.2.1 Patch 3 (#129) (2024-12-12)
- chore(deps): bump androidx.constraintlayout:constraintlayout (#124) (2024-12-12)
- chore(deps): bump androidx.core:core from 1.13.1 to 1.15.0 (#123) (2024-12-12)
- OKHttp to 4.12.0 (#121) (2024-10-14)
- chore: Collection of dependabot changes (#120) (2024-10-10)
- chore: Testing infrastructure (#119) (2024-10-10)
- chore(deps): bump androidx.test.espresso:espresso-core (#108) (2024-09-06)
- chore(deps): bump androidx.appcompat:appcompat from 1.6.1 to 1.7.0 (#111) (2024-09-06)
- chore(deps): bumps Spotless to 6.25.0 and associated fixes. (2024-09-06)
- chore(deps): bump com.squareup.leakcanary:leakcanary-android (#110) (2024-09-05)
- chore(deps): bump com.google.code.gson:gson from 2.8.9 to 2.11.0 (#112) (2024-09-05)
- Ran spotless checks from root for the first time and it found more to correct. (2024-09-05)
- Changed build management for resources after investigating the new Gradle settings. (2024-09-05)
- Merge pull request #107 from MindscapeHQ/feature/#102-dependencies (2024-09-05)
- Formatting result (2024-09-03)
- Tweaking formatting rules (2024-09-03)
- Dependabot integration using the Gradle version library setup. (2024-09-03)
- Merge pull request #106 from MindscapeHQ/feature/#100-templates (2024-09-03)
- Transition to using a Gradle library version management file and re-wiring, so that we can use dependabot and generate BOMs etc. (2024-09-02)
- Merge pull request #105 from MindscapeHQ/feature/#103-linting-formatting (2024-09-02)
- Added issue and PR templates. (2024-08-31)
- Spotless run to cleanup and normalize formatting. (2024-08-31)
- Added Spotless for code project-wide code-formatting. (2024-08-31)
- Merge pull request #99 from MindscapeHQ/feature/#98-feedbackonbuttons (2024-08-31)
- Added some screenshots of the sample app and mentioned in documentation. (2024-08-30)
- Changing the way 2nd activity is called so that a snackbar can be shown on return properly. (2024-08-30)
- Enabling multi-dexing for the sample app. (2024-08-30)
- Added some UI Snackbars as action indicators. (2024-02-21)
- Added material to sample app for Snackbar component (2024-02-21)
- Fixed wrong tool context (2024-02-21)
- ci: Add connected test to GH CI (#136) (2025-01-31)
- ci: #90 add CI workflow (#132) (2025-01-14)

### v4.1.0

The content of v4.1.0 is identical to v4.1.1.

Unfortunately we made a mistake when releasing v4.1.0 to Maven Central and it got published in an incomplete state.

Please use v4.1.1 instead.

### v4.1.0-alpha2

- Gradle 8, AGP 8.1.1
- Fix: Sample app displayed wrong provider version
- Internal: Introduced gitflow process

### v4.1.0-alpha1

- Upgrade repo and all dependencies to SDK 34, Android X (#83) and others.
- Documented Timber linting behaviour and solution (#69).
- Upgraded all builds to Gradle 7.x to work correctly with Android Studio Giraffe (2022.3.1)

### v4.0.2-beta1

- Added additional RaygunClient.init() entry point to make usage from within cross-platform libraries more accessible (#72)
- Debug build of the sample app now includes LeakCanary

### v4.0.1

- Fixes for an issue with breadcrumb processing (#64) and improved general parsing of the stack trace
- Fixes inability to use CrashReporingOnBeforeSend interface due to wrong visibility (#63)
- Safeguard against slightly obscure "out of files" scenario that can cause File.listFiles() to return null (#62)
- General improvements to the sample app:
  - Refactoring of sample app into its own namespace
  - Samples for breadcrumb handling
  - Samples for intercepting the crash reporting data

### v4.0.0

- Minor internal changes to memory handling
- Linting cleanup

### v4.0.0-beta3

- API changes for setting and working with custom data: the field userCustomData is now being referred to as customData. This is reflected in API method changes.
- Documentation for both Raygun4Android Gradle tasks
- Added Timber 4.7.1 dependency for debug/prod logging

### v4.0.0-beta2

This is a major release and has a variety of breaking changes, depending on your use cases. This is an overview of the most important and visible changes:

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

### v3.0.6

- Fixed null reference exception when building crash report messages. We now send messages using a JobIntentService to improve compatibility with the latest Android versions. IMPORTANT: Support for API versions 9 to 15 has been discontinued from this release due to that.

### v3.0.5

- Fixing timing issue from ConnectivityManager in RaygunPostService (#41), adding null checks around intent extras being null that should never be null (XRAY-1898)

### v3.0.4

- Fixing NPE in RaygunPostService (#34, PR #37); RaygunErrorMessage can now accept any Throwable (PR #33); Pulse now checks for connectivity (PR #38)

### v3.0.3

- Bug fix: removing println from code (PR #36)

### v3.0.2

- Allowing all properties of the RaygunMessageDetails to be modifiable during the onBeforeSend callback.

### v3.0.1

- Update sample app in repository to remove usage of deprecated methods on RaygunClient

### v3.0.0

- Add support for Pulse for Mobile (automatic network call tracking etc)

### v2.1.1

- Added MY_PACKAGE_REPLACED intent receiver and guard against spurious null Context seen on some devices when checking net connection

### v2.1.0

- Add OnBeforeSend implementation; expose setGroupingKey

### v2.0.0

- Replace deprecated Apache HTTP library with HttpUrlConnection; change packaging format to AAR for Android Studio/Gradle compatibility

### v1.3.0

- Provide device network connectivity state under Request section; aAdded RaygunClient.SetTags() and SetUserCustomData() to provide objects that will be attached to all exceptions

### v1.2.1

- Fix: only distinct device IPs are transmitted

### v1.2.0

- Added device IP data (shown in the 'Request' tab in the Raygun dashboard)

### v1.1.0

- Add new user info properties, bump Gson to 2.2.4

### v1.0.5

- Guard against a spurious NullPointerException caused by the posting service

### v1.0.4

- JSON payload now encoded in UTF-8, fixes issues with post-ASCII chars (e.g Cyrillic) were displayed as '?' in Raygun dashboard

### v1.0.3

- Improved version tracking support

### v1.0.2

- Added SetUser method for unique user tracking/customers.

### v1.0.1

- Added caching of messages to disk when network unavailable & post them when it becomes available again; several bug fixes relating to the posting service. This version is recommended; do not use 1.0.0.

### v1.0.0

- Completed initial version with background service for performing posting, API key read from AndroidManifest and more.

### v0.0.1

- Initial release with basic functionality.
