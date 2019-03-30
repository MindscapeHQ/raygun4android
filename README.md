# Raygun4Android

The world's best Android Crash Reporting and Real User Monitoring solution

Supports Android 4.1+ (API 16+).

## IMPORTANT

### 26 March 2019

Raygun4Android is currently actively being worked on for a release of version 4.

If you found this branch (4.0.0), you found the area that we're working on at the moment. The code is quite stable and we encourage you to use the 4.0.0-alpha2 release.

If you want the fully stable version go back to master, in which you'll find the 3.x stream of the provider.

## Requirements

- minSdkVersion 16+
- compileSdkVersion 28

## Installation

### With Android Studio and Gradle

Ensure jcenter() or mavenCentral() are present in your **project's** build.gradle:

```gradle
allprojects {
    repositories {
        jcenter()
        mavenCentral()
    }
}
```

Then add the following to your **module's** build.gradle:

```gradle
dependencies {
    ...
    compile 'com.raygun:raygun4android:4.0.0-alpha2'
}
```


In your app's **AndroidManifest.xml**, make sure you have granted Internet permissions. Beneath the ```<manifest>``` element add:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Inside the ```<application>``` element, add:

```xml
<service android:name="com.raygun.raygun4android.services.CrashReportingPostService"
         android:exported="false"
         android:permission="android.permission.BIND_JOB_SERVICE"
         android:process=":crashreportingpostservice"/>
<service android:name="com.raygun.raygun4android.services.RUMPostService"
         android:exported="false"
         android:permission="android.permission.BIND_JOB_SERVICE"
         android:process=":rumpostservice"/>
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

## Raygun and ProGuard

### General

ProGuard is a free Java tool for obfuscation, class file shrinking, optimizing and pre-verifying. When enabling ProGuard in a native Android application that also uses Raygun, the obfuscation feature requires a bit of attention. By default, your obfuscated class and method names will show up in the stacktraces of exception/error reports submitted to Raygun. This makes the stacktraces difficult to read when looking into the cause of the issues.

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

### Gradle Task

Instead of uploading mapping.txt manually after each deployment, you can use the **uploadProguardMapping** task in the yaygun group of Gradle tasks.

You will find an example of how to do this in the sample app. Go to the **app** module's build.gradle file and look for the **createRaygunProguardTask** function.

```groovy
def createRaygunProguardTask(token,raygunAppPath,groupName,version) {

    task "uploadProguardMapping" {
        group "${groupName}"

        doLast {
            def proguardMappingFileParam = "file=@${project.rootDir}/app/build/outputs/mapping/release/mapping.txt"
            def versionIdentifierParam = "version=${version}"
            def raygunProguardEndpointUrlParam = "https://app.raygun.com/upload/proguardsymbols/${raygunAppPath}?authToken=${token}"

            def p = ['curl', '-F', proguardMappingFileParam, '-F', versionIdentifierParam, raygunProguardEndpointUrlParam].execute()
            p.waitFor()

            def result = p.text
            println result
            assert result == "true"
        }
    }
}
```

This function gets called from within the ```android {...}``` block of the Gradle file at each build in Android Studio and creates the appropriate parameterised task to push the file into the Raygun backend.

The example shown requires curl to be on the PATH of your machine. Depending on your project structure and module names you also might have to adjust the path used in **proguardMappingFileParam**.

## Advanced features

### Affected user tracking

Raygun supports tracking the unique users who encounter bugs in your apps.

By default a device-derived UUID is transmitted. You can also add the currently logged in user's data like this:

```java
RaygunUserInfo user = new RaygunUserInfo();
user.setIdentifier("a@b.com");
user.setFirstName("User");
user.setFullName("User Name");
user.setEmail("a@b.com");
user.setAnonymous(false);
RaygunClient.setUser(user);
```

Any of the properties are optional, for instance you could set only isAnonymous by calling the setAnonymous() method. There is also a constructor overload if you prefer to specify all in one statement and a convenience constructor to only set an identifier.

`identifier` should be a unique representation of the current logged in user - we will assume that messages with the same identifier are the same user. If you do not set it, it will be automatically set to the device UUID.

If the user context changes, for instance on log in/out, you should remember to call setUser again to store the updated user identifier. If a user logs out and you want to use the default device identifier again, just create an empty `RaygunUserInfo` object.

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

If the device can't connect, Raygun4Android will save the crash report to disk. At the next start of the application (and therefore the provider) it will check if the internet is now available, and if it is, send the cached messages. A maximum of 64 messages will be cached and you can change the amount by calling:

```java
RaygunClient.setMaxReportsStoredOnDevice(amount)
```

You cannot increase the amount beyond the maximum of 64. If you decrease the amount, any currently stored cached reports will be deleted.

### Version tracking

If you want track the version of your app with a crash report, you can do that in different ways:

1. Set the versionName attribute on `<manifest>` in your AndroidManifest.xml to be of the form x.x.x.x, where x is a positive integer
2. Set the version in the overloaded `init` method when initialising RaygunClient: `public static voide init(Application application, String apiKey, String version)`
3. Use the `setVersion` method in RaygunClient: `public static void setVersion(String version)`

The app's version will then be sent with each message and you can then filter by version in the Raygun dashboard.

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


public class FullscreenActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize the activity as normal

        RaygunClient.init((Application)getApplicationContext());

        RaygunClient.enableCrashReporting();
        RaygunClient.enableRUM(this);

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

## API Overview

#### Initialisation

The following method overloads are available for initializing RaygunClient:

* `RaygunClient.init(Application application)`
* `RaygunClient.init(Application application, String apiKey)`
* `RaygunClient.init(Application application, String apiKey, String version)`

The first method reads the API key from the application's AndroidManifest.xml. If you want to specify your API key programmatically, use one of the latter two methods.

#### Enabling features

Crash Reporting:

* `RaygunClient.enableCrashReporting()`
* `RaygunClient.enableCrashReporting(boolean attachDefaultHandler)`

Both methods will enable crash reporting. By default, a pre-made Uncaught Exception Handler, which will automatically send an exception when one reaches it (ie. just before your app crashes), will be setup. If you want to have control over this behaviour, use the second method.

Tags and custom data will be attached to all exceptions that reaches it. This allows you to automatically send crash data when your app crashes. The handler will call any other pre-existing exception handlers you have set up before it sends to Raygun. After it is complete, it will call the default handler, which will crash the app and display the 'close app' user dialog. Exceptions are guaranteed to be sent if your app crashes.

RUM:

* `RaygunClient.enableRUM(Activity activity)`
* `RaygunClient.enableRUM(Activity activity, boolean networkLogging)`

Both methods enable RUM. By default, network activity details are being logged. If you want to change this behaviour, please use the second method.

#### Sending crash reports manually

The following methods are available for sending manually; pick one depending on how much extra data you'd like to send:

* `RaygunClient.send(Throwable throwable)`
* `RaygunClient.send(Throwable throwable, List tags)`
* `RaygunClient.send(Throwable throwable, List tags, Map userCustomData)`

The `send` function builds a RaygunMessage for you and then sends it.

#### User management

* `RaygunClient.setUser(String user)`
* `RaygunClient.setUser(RaygunUserInfo userInfo)`

The first method internally builds a `RaygunUserInfo` with `user` being used at the identifier field. Ensure you call again if the user context changes (usually login/logout).

* `RaygunClient.setUserCustomData(Map userCustomData)`

Sets a key-value Map which will be sent along with every exception. This will be merged with any other custom data passed as the third param of send().

#### Misc

* `RaygunClient.setVersion(String version)`

Stores the version of your application manually. Normally, this is automatically read from AndroidManifest (the versionName attribute on <manifest>) and is provided as a convenience.

* `RaygunClient.setTags(List tags)`

Sets a list of tags which will be sent along with every exception. This will be merged with any other tags passed as the second param of send().

* `RaygunClient.setOnBeforeSend(CrashReportingOnBeforeSend onBeforeSendImplementation)`

Provides an instance of a class which has an onBeforeSend method that can be used to inspect, mutate or cancel the send to the Raygun API immediately before it happens. Can be used to filter arbitrary data.

## Frequently Asked Questions

* Is there an example app?

  Yup - clone this repository then run the **app** module of the project.

* Not seeing errors in the dashboard?

  Raygun4Android outputs Logcat messages - look for the the logcat tag **Raygun4Android**. HTTP Status 403 will indicate an invalid API key, 400 a bad message, and 202 will indicate received successfully.

* Environment Data

  A selection of environment data will be attached and available in the Environment tab in the dashboard, and more in the Raw tab. This data is gathered from android.os.Build - if you wish to see more data you can add them on the userCustomData object.

* The library logs an error message about a not found class: Rejecting re-init on previously-failed class java.lang.Class<android.support.v4.app.JobIntentService$JobServiceEngineImpl>

  The message above stems from certain versions of the Android support libraries. JobServiceEngineImpl is part of Android Oreo (8, SDK 26) and newer only. The support library catering for supporting services on earlier versions of Android runs internal checks for which implementation is available to it. As part of the checks, it outputs the message as an informational feedback only.
