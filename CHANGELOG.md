## Changelog

- v3.0.5: Fixing timing issue from ConnectivityManager in RaygunPostService (#41), adding null checks around intent extras being null that should never be null (XRAY-1898)

- v3.0.4: Fixing NPE in RaygunPostService (#34, PR #37); RaygunErrorMessage can now accept any Throwable (PR #33); Pulse now checks for connectivity (PR #38)

- v3.0.3: Bugfix: removing println from code (PR #36)

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

- v1.0.1: Added caching of messages to disk when network unavailable & post them when it becomes available again; several bugfixes relating to the posting service. This version is recommended; do not use 1.0.0.

- v1.0.0: Completed initial version with background service for performing posting, API key read from AndroidManifest and more.

- v0.0.1: Initial release with basic functionality.
