# Raygun4Android

The world's best Android crash reporter.

Supports Android 2.3.3 (API 9) and above.

## Installation

### With Maven

To your pom.xml, add:

```xml
<dependency>
    <groupId>com.mindscapehq.android</groupId>
    <artifactId>raygun4android</artifactId>
    <version>1.0.0</version>
</dependency>
```

In your IDE, build your project (or run `mvn compile`), then see the configuration section below.

### With Ant, other build tools, or manually

Download the JAR and place it in a /lib folder in your project. Add it to your project's classpath, then see below.

## Configuration & Usage

1. In your **AndroidManifest.xml**, make sure you have granted Internet permissions. Beneath the **manifest** element add:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

2. Inside the **application** element, add:

```xml
<meta-data android:name="com.mindscapehq.android.raygun4android.apikey"
                   android:value="PASTE_YOUR_API_KEY_HERE" />
```

3. In a central location, call `RaygunClient.Init()`, passing in your activity's context.

4. When an exception occurs, such as in a catch block, call RaygunClient.Send(), passing in the Throwable. This will send it to the Raygun service, where it will be viewable in the dashboard.

For a usage example, check out the application in /sample-app.

## Documentation

### API

The following method overloads are available for initializing RaygunClient:

* RaygunClient.Init(Context context)

* RaygunClient.Init(String version, Context context)

* RaygunClient.Init (Context context, String apiKey)

* RaygunClient.Init (Context context, String apiKey, String version)

The first two read the API key from the application's AndroidManifest.xml. Whichever Context you pass in will have its API key read. If you want to specify your API key programmatically, use one of the latter two methods.

The following methods are available for setting up a pre-made Uncaught Exception Handler, which will automatically send an exception when one reaches it (ie. just before your app crashes):

* RaygunClient.AttachExceptionHandler()

* RaygunClient.AttachExceptionHandler(List tags)

* RaygunClient.AttachExceptionHandler(List tags, Map userCustomData)

The tags and custom data will be attached to all exceptions that reaches it. This allows you to automatically send crash data when your app crashes.

The handler will call any other pre-existing exception handlers you have set up before it sends to Raygun. After it is complete, it will call the default handler, which will crash the app and display the 'close app' user dialog. Exceptions are guaranteed to be sent if your app crashes.

The following methods are available for sending manually; pick one depending on how much extra data you'd like to send:

* RaygunClient.Send(Throwable throwable)

* RaygunClient.Send(Throwable throwable, List tags)

* RaygunClient.Send(Throwable throwable, List tags, Map userCustomData)

These build a RaygunMessage for you then send it. If you'd like to build a message manually you can use:

* RaygunClient.Post(RaygunMessage raygunMessage)

Note that this will require certain fields to be present - documentation is available at http://raygun.io/raygun-providers/rest-json-api

### Frequently Asked Questions

* Not seeing errors in the dashboard?
	
	Raygun4Android outputs Logcat messages - look for the 'Exception Message HTTP POST result' message - 403 will indicate an invalid API key, 400 a bad message, and 202 will indicate received successfully.

* Environment Data

	A selection of enironment data will be attached and available in the Environment tab in the dashboard, and more in the Raw tab. This data is gathered from android.os.Build - if you wish to see more data you can add them on the userCustomData object.

### Contributing

Clone this repository, then run `mvn install` to grab the dependencies and install the library to your local Maven repo. Runs with Android 2.2 (API level 8) and JDK 1.6


## Changelog

Version 0.0.1: Initial release with basic functionality.