# Raygun4Android

The world's best Android Crash Reporting and Real User Monitoring solution

Supports Android 6+ (API 23+).

- If you need to support Android 4.1+, please use Raygun4Android 4.0.1.
- If you need to support Android 5+, please use Raygun4Android 5.0.1.

## Library structure

### Compatibility

Starting from version 5.0.0, the library has been migrated from Java to Kotlin.
This means that the library is now fully compatible with Kotlin Coroutines and property accessors.
We recommend using those if you are using Kotlin in your project.
As well, the library is still compatible with pure Java Android applications.

Raygun4Android 5.2.1 is currently considered to be the stable release of the provider and is tagged in the repository and supports Android 6+.

The `develop` branch reflects ongoing work on the version 5 line as tagged snapshots and only supports Android 6+.

If you want the *very old* stable version 3.0.6 please check out the change set labelled with `v3.0.6` and go from there.

### Requirements

- minSdkVersion 23+
- compileSdkVersion 36

### Internal dependencies

- Gson
- OKHttp
- Timber

## Installation

### With Android Studio and Gradle

Ensure `mavenCentral()` is present in your **project's** root `build.gradle.kts`:

```kotlin
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

Then add the following to your **module's** `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.raygun:raygun4android:5.2.1")
}
```


In your app's **AndroidManifest.xml**, make sure you have granted Internet permissions. Beneath the ```<manifest>``` element add:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Inside the ```<application>``` element, add:

```xml
<meta-data android:name="com.raygun.raygun4android.apikey"
           android:value="PASTE_YOUR_API_KEY_HERE" />
```

Replace the value in ```<meta-data>``` with your API key, available from your Raygun dashboard.

In a central activity (we suggest to use your common launch activity), call the following:

```java
RaygunClient.init(application);
// Crash Reporting
RaygunClient.enableCrashReporting();
// RUM
RaygunClient.enableRUM(activity);
```

The above exception handler automatically catches and sends all uncaught exceptions. You can create your own or send from a catch block by calling RaygunClient.send() and passing in the Throwable.

For an actual usage example, check out the sample application in the **app** module of this project

## Raygun and ProGuard/R8

### General

ProGuard and R8 are tools for obfuscation, class file shrinking, optimizing and pre-verifying. When enabling ProGuard in a native Android application that also uses Raygun, the obfuscation feature requires a bit of attention. By default, your obfuscated class and method names will show up in the stacktraces of exception/error reports submitted to Raygun. This makes the stacktraces difficult to read when looking into the cause of the issues.

ProGuard produces a mapping.txt file that can be used to restore the original class and method names. Such files can be uploaded to Raygun to automatically process all of your exception reports into readable stacktraces.

### Setup

Add the following lines to your proguard-rules.pro file so that Raygun and ProGuard to play nicely together. Each line is explained below so that you can understand what these changes to your pro file will do.

```
-keep class com.raygun.raygun4android.** { *; }
-keepattributes Exceptions, Signature, InnerClasses, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
```
**-keep** is required here in order for Raygun4Android to function correctly. This line tells ProGuard not to obfuscate any of the code in Raygun4Android. Some of the classes are used to build up a Json payload, which if obfuscated is going to create a payload that Raygun can’t read.

**-keepattributes** is recommended in order to keep certain bits of information. In particular, Signature is needed to resolve generic type names and LineNumberTable is so that your stack traces have line numbers which is generally what you want. By default, file names will not be available in the stacktraces. The SourceFile entry on the -keepattributes line will cause file names to be available in the stacktraces, but note that they are not obfuscated. Don't include SourceFile on the -keepattributes line if you don't want your file names to be included in your app package.

**-renamesourcefileattribute** is optional. This causes the file names of your code to all appear as “SourceFile” in the stacktrace. This is for added secrecy so that your actual file names can not be seen in the application package. Even with a mapping file, the original file names can not be resolved, which is not so good for debugging. If you don't mind your file names being kept, then feel free to remove this line for the extra debugging help.

### Proguard Gradle Task

Instead of uploading mapping.txt manually after each deployment, you can use the **uploadProguardMapping** task in the Raygun group of Gradle tasks.

You will find an example of how to do this in the sample app. Go to the **app** module's `build.gradle.kts` file and look for the **registerRaygunProguardTask** function.

```kotlin
fun Project.registerRaygunProguardTask(
    token: String,
    raygunAppPath: String,
    groupName: String,
    version: String,
) {
    tasks.register("uploadProguardMapping") {
        group = groupName

        doLast {
            val mappingFile =
                file("${project.rootDir}/app/build/outputs/mapping/release/mapping.txt")
            check(mappingFile.exists()) { "Mapping file not found: ${mappingFile.absolutePath}" }

            val endpointUrl =
                "https://app.raygun.com/upload/proguardsymbols/$raygunAppPath?authToken=$token"
            val url = URI(endpointUrl).toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            // ... write multipart form data (version + mapping file) and send
            // See app/build.gradle.kts for the full implementation
        }
    }
}
```

This function is called at the top level of the `build.gradle.kts` file and creates the appropriate parameterised task to push the mapping file into the Raygun backend. Depending on your project structure and module names you may have to adjust the path to the mapping file.

### Release Notification Gradle Task

Raygun4Android also comes with a second Gradle task intended to notify the Raygun backend when you deploy a new version of your app. The idea behind this functionality is that it will allow you to see changes in error rate or user behaviour by version.

Raygun offers a REST API for this notification. The sample app contains a function **registerRaygunNotifyDeploymentTask** that creates a **notifyDeployment** task.

```kotlin
fun Project.registerRaygunNotifyDeploymentTask(
    token: String,
    key: String,
    groupName: String,
    version: String,
    userName: String,
    userEmail: String,
) {
    tasks.register("notifyDeployment") {
        group = groupName

        doLast {
            val url = URI("https://app.raygun.io/deployments?authToken=$token").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val body =
                """{"apiKey":"$key","version":"$version","ownerName":"$userName","emailAddress":"$userEmail"}"""
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }

            val response =
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            println(response)
        }
    }
}
```

This function is called at the top level of the `build.gradle.kts` file and creates the appropriate parameterised task to notify the Raygun backend of your app's deployment.

## Sample application

The project contains a small sample application in the `:app` module. It demonstrates common use cases like using a global error handler, custom behaviour for catching and reporting errors and more.

[!image1](app-1.jpg)
[!image2](app-2.jpg)

## Basic Features

### Initialisation

The following method overloads are available for initializing RaygunClient:

* `RaygunClient.init(Application application)`
* `RaygunClient.init(Application application, String apiKey)`
* `RaygunClient.init(Application application, String apiKey, String version)`

The first method reads the API key from the application's AndroidManifest.xml.
If you want to specify your API key programmatically, use one of the latter two methods.

### Affected Customers

Raygun supports tracking the unique customers who encounter bugs in your apps.

By default a device-derived UUID is transmitted.

You can also add the currently logged in customer's data like this:

```java
String userIdentifier = "12345";
...
RaygunUserInfo user = RaygunUserInfo.create(userIdentifier);
user.setFirstName("User");
user.setFullName("User Name");
user.setEmail("a@b.com");
...
RaygunClient.setUser(user);
```

`identifier` should be a unique representation of the current logged in customer -
we will assume that messages with the same identifier are the same customer.
If you do not set it, it will be automatically set to the device UUID.

If the customer context changes, for instance on log in/out,
you should remember to call setUser again to store the updated customer identifier.

#### Anonymous users

If a customer logs out and you want to use the default device identifier again,
just create an anonymous `RaygunUserInfo` object. In this case `isAnonymous` will be set to true.

To create an anonymous user, call to `RaygunUserInfo.anonymous()`.

This static method creates a new `RaygunUserInfo` instance with a random UUID as the identifier.
This method is a `suspend` function, because it reads/writes to disk through `SharedPreferences`,
so you need to call it from a coroutine when using Kotlin.

For Java developers, or for situations where coroutines are not available,
the method is available as `RaygunUserInfo.anonymousSync()`,
which creates an anonymous user synchronously.
This method is not recommended for use in the main thread, as it may block the UI and cause ANR errors.

#### Customer management

* `RaygunClient.setUser(String user)`
* `RaygunClient.setUser(RaygunUserInfo userInfo)`

The first method internally builds a `RaygunUserInfo` with `user` being used at the identifier field.
Ensure you call again if the customer context changes (usually login/logout).

### Version tracking

If you want track the version of your app with a crash report or a RUM message, you can do that in different ways:

1. Set the versionName attribute on `<manifest>` in your AndroidManifest.xml to be of the form x.x.x.x, where x is a positive integer
2. Set the version in the overloaded `init` method when initialising RaygunClient: `public static voide init(Application application, String apiKey, String version)`
3. Use the `setVersion` method in RaygunClient: `public static void setVersion(String version)`

The app's version will then be sent with each message and you can then filter by version in the Raygun dashboard.

## Crash Reporting

### Enable crash reporting

To enable crash reporting, you need to call one of the following methods in your code:

* `RaygunClient.enableCrashReporting()`
* `RaygunClient.enableCrashReporting(boolean attachDefaultHandler)`

Both methods will enable crash reporting. By default, a pre-made Uncaught Exception Handler, which will automatically send an exception when one reaches it (ie. just before your app crashes), will be setup. If you want to have control over this behaviour, use the second method.

Tags and custom data will be attached to all exceptions that reaches it. This allows you to automatically send crash data when your app crashes. The handler will call any other pre-existing exception handlers you have set up before it sends to Raygun. After it is complete, it will call the default handler, which will crash the app and display the 'close app' user dialog. Exceptions are guaranteed to be sent if your app crashes.

### Tags and custom data

* `RaygunClient.setCustomData(Map customData)`

Sets a key-value Map which will be sent along with every exception. This will be merged with any other custom data passed as the third param of send().

* `RaygunClient.setTags(List tags)`

Sets a list of tags which will be sent along with every exception. This will be merged with any other tags passed as the second param of send().

### Sending crash reports manually

The following methods are available for sending manually; pick one depending on how much extra data you'd like to send:

* `RaygunClient.send(Throwable throwable)`
* `RaygunClient.send(Throwable throwable, List tags)`
* `RaygunClient.send(Throwable throwable, List tags, Map customData)`
* `RaygunClient.send(String exceptionName, String reason, List tags, Map customData)`

The `send` function builds a RaygunMessage for you and then sends it.

### Getting/setting/cancelling the error before it is sent

This provider has an onBeforeSend API to support accessing or mutating the candidate error payload immediately before it is sent, or cancelling the send outright. This is provided as the public method `RaygunClient.setOnBeforeSend(RaygunOnBeforeSend)`, which takes an instance of a class that implements the `CrashReportingOnBeforeSend` interface. Your class needs a public `onBeforeSend` method that takes a `RaygunMessage` parameter, and returns the same.

By example:

```java
class BeforeSendImplementation implements CrashReportingOnBeforeSend {
    @Override
    public RaygunMessage onBeforeSend(RaygunMessage message) {
        Log.i("onBeforeSend", "About to post to Raygun, returning the payload as is...");
        return message;
    }
}
...

public class SomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize the activity as normal
        ...
        // Initialize Raygun
        ...
        RaygunClient.setOnBeforeSend(new BeforeSendImplementation());
    }
}
```

In the example above, the overridden `onBeforeSend` method will log an info message every time an error is sent.

To mutate the error payload, for instance to change the message:

```java
@Override
public RaygunMessage onBeforeSend(RaygunMessage message) {
    Log.i("onBeforeSend", "Changing the message...");

    RaygunMessageDetails details = message.getDetails();
    RaygunErrorMessage error = details.getError();
    error.setMessage("Mutated message");

    return message;
}
```

To cancel the send (prevent the error from reaching the Raygun dashboard) by returning null:

```java
@Override
public RaygunMessage onBeforeSend(RaygunMessage message) {
    Log.i("onBeforeSend", "Cancelling sending message to Raygun...");

    return null;
}
```

### Custom error grouping

You can override Raygun's default grouping logic for Android exceptions by setting the grouping key manually in onBeforeSend (see above):

```java
@Override
public RaygunMessage onBeforeSend(RaygunMessage message) {
    RaygunMessageDetails details = message.getDetails();
    details.setGroupingKey("foo");

    return message;
}
```

Any error instances with a certain key will be grouped together. The example above will place all errors within one group (as the key is hardcoded to 'foo'). The grouping key is a String and must be between 1 and 100 characters long. You should send all data you care about (for instance, parts of the exception message, stacktrace frames, class names etc) to a hash function (for instance MD5), then pass that to `setGroupingKey`.

## Real User Monitoring (RUM)

### Enable RUM

To enable RUM, you need to call one of the following methods in your code:

* `RaygunClient.enableRUM(Activity activity)`
* `RaygunClient.enableRUM(Activity activity, boolean networkLogging)`

Both methods enable RUM. By default, network activity details are being logged.
If you want to change this behaviour, please use the second method.

## Advanced Features

### Custom endpoints

Raygun supports sending data from Crash Reporting and Real User Monitoring to your own endpoints. If you want to set custom endpoints, could can do so by setting them after you've initialised RaygunClient:

```java
// Crash Reporting
RaygunClient.setCustomCrashReportingEndpoint("http://...");
// RUM
RaygunClient.setCustomRUMEndpoint("http://...");
```

Please note that setting a custom endpoint will stop Crash Report or Real User Monitoring data from being sent to the Raygun backend.

### Storing crash reports on the device

If the device can't connect because it is offline, Raygun4Android will save the crash report to the device storage. At the next start of the application, (along with the provider) it will check if the internet is now available. If it is, send the cached messages. A maximum of 64 messages will be cached. Once the storage limit is reached, no further crash reports are stored locally until the storage has been cleared. You can change the amount by calling:

```java
RaygunClient.setMaxReportsStoredOnDevice(amount)
```

You cannot increase the amount beyond the maximum of 64. If you decrease the amount, any currently stored cached reports will be deleted.

### Mobile network type information

Raygun attaches information about the current network capabilities in every crash report.

In order to include details about the mobile network type (e.g. "UMTS"), you need to explicitly request the `READ_PHONE_STATE` permission.

This functionality is optional. If this permission is not granted, the mobile network type will appear as "Unknown". The library does not request permissions automatically.

## Frequently Asked Questions

* Is there an example app?

  Yup - clone this repository, then run the **app** module of the project.

* I'm not seeing errors in Raygun Crash Reporting.

  Raygun4Android outputs Logcat messages - look for the the logcat tag **Raygun4Android**. HTTP Status 403 will indicate an invalid API key, 400 a bad message, and 202 will indicate received successfully.

* My build fails with `Default interface methods are only supported starting with Android N (--min-api 24)`. Why is that?

  Raygun4Android uses Timber for internal logging. This requires some language features that are only available with Java 8 or higher. Make sure that your project, using the library, has set the compilation compatibility to at least Java 17 (the version currently used by the library).

  Google's documentation has more information on the reasons and implications of this requirement: https://developer.android.com/studio/write/java8-support

* There's something weird going on - I checked the logs and the Raygun servers can't be reached!

  We found that certain apps in the category of ad- and tracking blockers on Android devices can stop Raygun messages from going through to our servers. One example of this is Blockada, a well-known and root-less Adblocker on Android. Unfortunately there is nothing we can directly do to stop problems arising from this to occur.

  One possible workaround would be to implement a check in your app to see if api.raygun.io is reachable and if not, post this customer's crash reports or RUM events to your own backend via custom endpoints.

* Environment Data

  A selection of environment data will be attached and available in the Environment tab in the dashboard, and more in the Raw tab. This data is gathered from android.os.Build - if you wish to see more data you can add them on the userCustomData JSON object by setting custom data through the API..

* The library logs an error message about a not found class: Rejecting re-init on previously-failed class java.lang.Class<android.support.v4.app.JobIntentService$JobServiceEngineImpl>

  The message above stems from certain versions of the Android support libraries. JobServiceEngineImpl is part of Android Oreo (8, SDK 26) and newer only. The support library catering for supporting services on earlier versions of Android runs internal checks for which implementation is available to it. As part of the checks, it outputs the message as an informational feedback only.

* Timber Lint warnings get exposed to my app even though I don't (want to) use Timber.

  The solution for the time being is to disable linting for those specific lint warning in your app:

```kotlin
android {
    lint {
        disable += setOf(
            "LogNotTimber", "StringFormatInTimber", "ThrowableNotAtBeginning",
            "BinaryOperationInTimber", "TimberArgCount", "TimberArgTypes", "TimberTagLength"
        )
    }
}
```
