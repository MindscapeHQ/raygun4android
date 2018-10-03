# Raygun4Android

The world's best Android Crash Reporting and Real User Monitoring solution

Supports Android 4.1+ (API 16+).

## IMPORTANT

### 21/09/2018

Raygun4Android is currently actively being worked on for a release of version 4 and a lot of the documentation below is outdated and has not been updated (yet).

If you found this branch (4.0.0) - it's probably not where you want to be and things will most likely break for you. Sorry.

Please go back to master, in which you'll find the 3.x stream of the provider that we actively encourage you to use.

## Requirements

- minSdkVersion 16+
- compileSdkVersion 28

## Installation

### With Android Studio and Gradle

Ensure Maven Central is present in your **project's** build.gradle:

```
allprojects {
    repositories {
        mavenCentral()
    }
}
```

Then add the following to your **module's** build.gradle:

```
dependencies {
	...
	compile 'com.raygun:raygun4android:4.0.0'
}
```


In your app's **AndroidManifest.xml**, make sure you have granted Internet permissions. Beneath the **manifest** element add:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Inside the **<application>** element, add:

```xml
<service android:name="com.raygun.raygun4android.RaygunPostService"
         android:exported="false"
         android:permission="android.permission.BIND_JOB_SERVICE"
         android:process=":raygunpostservice"/>
<meta-data android:name="com.raygun.raygun4android.apikey"
           android:value="PASTE_YOUR_API_KEY_HERE" />
```

Replace the value in meta-data with your API key, available from your Raygun dashboard.

In a central activity, call the following:

```java
RaygunClient.init(getApplicationContext());
RaygunClient.attachExceptionHandler();
```

You might need to add the following specific imports to your class, where you wish to use RaygunClient:

```java
import com.raygun.raygun4android.RaygunClient;
import com.raygun.messages.RaygunUserInfo;
```

The above exception handler automatically catches & sends all uncaught exceptions. You can create your own or send from a catch block by calling RaygunClient.send() and passing in the Throwable.

For an actual usage example, check out the sample application in the **app** modle of this project

## Raygun and ProGuard

### General

ProGuard is a free Java tool for obfuscation, class file shrinking, optimizing and preverifying. When enabling ProGuard in a native Android application that also uses Raygun, the obfuscation feature requires a bit of attention. By default, your obfuscated class and method names will show up in the stacktraces of exception/error reports submitted to Raygun. This makes the stacktraces difficult to read when looking into the cause of the issues.

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

Instead of uploading mapping.txt manually after each deployment, you can use the **uploadProguardMapping** in the raygun group of Gradle tasks. 

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

The example shown requires curl to be on the PATH of your machine.

## Documentation (NOT UP TO DATE)

### Affected user tracking

Raygun supports tracking the unique users who encounter bugs in your apps.

By default the device UUID is transmitted. You can also add the currently logged in user's data like this:

```java
RaygunUserInfo user = new RaygunUserInfo();
user.setIdentifier("a@b.com");
user.setFirstName("User");
user.setFullName("User Name");
user.setEmail("a@b.com");
user.setUuid("a uuid");
user.setAnonymous(false);
RaygunClient.setUser(user);
```

Any of the properties are optional, for instance you can set just isAnonymous by calling setAnonymous(). There is also a constructor overload if you prefer to specify all in one statement.

`identifier` should be a unique representation of the current logged in user - we will assume that messages with the same Identifier are the same user. If you do not set it, it will be automatically set to the device UUID.

If the user context changes, for instance on log in/out, you should remember to call SetUser again to store the updated username.

### Version tracking

Set the versionName attribute on <manifest> in your AndroidManifest.xml to be of the form x.x.x.x, where x is a positive integer, and it will be sent with each message. You can then filter by version in the Raygun dashboard.

### Getting/setting/cancelling the error before it is sent

This provider has an onBeforeSend API to support accessing or mutating the candidate error payload immediately before it is sent, or cancelling the send outright. This is provided as the public method `RaygunClient.setOnBeforeSend(RaygunOnBeforeSend)`, which takes an instance of a class that implements the `RaygunOnBeforeSend` interface. Your class needs a public `onBeforeSend` method that takes a `RaygunMessage` parameter, and returns the same.

By example:

```java
class BeforeSendImplementation implements RaygunOnBeforeSend {
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

        RaygunClient.init(getApplicationContext());
        RaygunClient.attachExceptionHandler();
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

### API

The following method overloads are available for initializing RaygunClient:

* RaygunClient.init(Context context)

* RaygunClient.init(String version, Context context)

* RaygunClient.init(Context context, String apiKey)

* RaygunClient.init(Context context, String apiKey, String version)

	The first two read the API key from the application's AndroidManifest.xml. Whichever Context you pass in will have its API key read. If you want to specify your API key programmatically, use one of the latter two methods.

The following method is available for setting up a pre-made Uncaught Exception Handler, which will automatically send an exception when one reaches it (ie. just before your app crashes):

* RaygunClient.attachExceptionHandler()

	The tags and custom data will be attached to all exceptions that reaches it. This allows you to automatically send crash data when your app crashes.

	The handler will call any other pre-existing exception handlers you have set up before it sends to Raygun. After it is complete, it will call the default handler, which will crash the app and display the 'close app' user dialog. Exceptions are guaranteed to be sent if your app crashes.

The following methods are available for sending manually; pick one depending on how much extra data you'd like to send:

* RaygunClient.send(Throwable throwable)

* RaygunClient.send(Throwable throwable, List tags)

* RaygunClient.send(Throwable throwable, List tags, Map userCustomData)

These build a RaygunMessage for you then send it. If you'd like to build a message manually you can use:

* RaygunClient.post(RaygunMessage raygunMessage)

	Note that this will require certain fields to be present - documentation is available at http://raygun.io/raygun-providers/rest-json-api

The following misc methods are available:

* RaygunClient.setUser(RaygunUserInfo userInfo)

	An object containing data about the currently logged in user - see above for details. Ensure you call this again if the user context changes.

* RaygunClient.setVersion(String version)

	Stores the version of your application manually. Normally, this is automatically read from AndroidManifest (the versionName attribute on <manifest>) and is provided as a convenience.

* RaygunClient.setTags(List tags)

  Sets a list of tags which will be sent along with every exception. This will be merged with any other tags passed as the second param of Send().

* RaygunClient.setUserCustomData(Map userCustomData)

  Sets a key-value Map which, like the tags above, will be sent along with every exception. This will be merged with any other tags passed as the third param of send().

* RaygunClient.setOnBeforeSend(RaygunOnBeforeSend onBeforeSendImplementation)

  Provides an instance of a class which has an onBeforeSend method that can be used to inspect, mutate or cancel the send to the Raygun API immediately before it happens. Can be used to filter arbitrary data.

* RaygunMessageDetails.setGroupingKey(String groupingKey)

  Provides a way to override the default grouping algorithm (see above for details). Any error instances with this key will be grouped together.

### Frequently Asked Questions

* Is there an example project?

  Yup - clone this repository then run the **app** module of the project. 

* Not seeing errors in the dashboard?

  Raygun4Android outputs Logcat messages - look for the the logcat tag **Raygun4Android** in raygunpostservice. HTTP Status 403 will indicate an invalid API key, 400 a bad message, and 202 will indicate received successfully.

* Environment Data

  A selection of enironment data will be attached and available in the Environment tab in the dashboard, and more in the Raw tab. This data is gathered from android.os.Build - if you wish to see more data you can add them on the userCustomData object.

* What happens when the phone has no internet connection?

  Raygun4Android will save the exception message to disk. When the provider is later asked to send another message it will check if the internet is now available, and if it is, send the cached messages. A maximum of 64 messages will be cached, then overwritten (using a LRU strategy).

  The provider now attaches the device's network connectivity state to the payload when the exception occurs. This is visible under the Request tab in the Raygun dashboard.

* RayGunPostMessage logs an error message about a not found class: Rejecting re-init on previously-failed class java.lang.Class<android.support.v4.app.JobIntentService$JobServiceEngineImpl>
      
  The message above stems from certain versions of the Android support libraries. JobServiceEngineImpl is part of Android Oreo (8, SDK 26) and newer only. The support library catering for supporting services on earlier versions of Android runs internal checks for which implementation is available to it. As part of the checks, it outputs the message as an informational feedback only.

### Contributing

Clone this repository, then run `mvn install` to grab the dependencies and install the library to your local Maven repo. Issues and pull requests are welcome.

### Changelog

[View the changelog here](CHANGELOG.md)
