Raygun4Android
==============

The world's best Android crash reporter.

Installation
=============

Configuration
==============

1. In your **AndroidManifest.xml**, make sure you have granted Internet permissions: `<uses-permission android:name="android.permission.INTERNET" />`

Troubleshooting
================

### Developing

Run `mvn install` to grab the dependencies and install the library to your local Maven repo. Runs with Android 1.6_r2 (API v4) and JDK 1.6

`mvn source:jar` to generate source jar for debugging

Changelog
=========

Version 0.0.1: Initial release with basic functionality.