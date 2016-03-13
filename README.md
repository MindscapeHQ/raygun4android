# Raygun4Android

The world's best Android crash and exception reporter.

Supports Android 2.3.1 (API 9 Gingerbread) through Android 6+

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
	compile 'com.google.code.gson:gson:2.1'
	compile 'com.mindscapehq.android:raygun4android:2.1.0'
}
```

You may need to add the following specific imports to your class, where you wish to use RaygunClient:

```java
import main.java.com.mindscapehq.android.raygun4android.RaygunClient;
import main.java.com.mindscapehq.android.raygun4android.messages.RaygunUserInfo;
```

Then see the configuration section below.

### With Maven

To your pom.xml, add:

```xml
<dependency>
    <groupId>com.mindscapehq.android</groupId>
    <artifactId>raygun4android</artifactId>
    <version>2.1.0</version>
</dependency>
```

In your IDE, build your project (or run `mvn compile`), then see the configuration section below.

### With Ant, other build tools, or manually

[Download the JAR for the latest version](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22raygun4android%22), as well as [the Gson library](http://search.maven.org/remotecontent?filepath=com/google/code/gson/gson/2.1/gson-2.1.jar) (if you do not already use it). Place both of these in a /lib folder in your project, add them to your project's classpath, then see below.

## Configuration & Usage

1. In your **AndroidManifest.xml**, make sure you have granted Internet permissions. Beneath the **manifest** element add:

	```xml
	<uses-permission android:name="android.permission.INTERNET" />
	<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
	```

2. Inside the **<application>** element, add:

	```xml
	<service   android:name="main.java.com.mindscapehq.android.raygun4android.RaygunPostService"
	           android:exported="false"
	           android:process=":raygunpostservice"/>
    <receiver  android:name="main.java.com.mindscapehq.android.raygun4android.OnPackageReplaced"
               android:exported="false">
        <intent-filter>
            <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
        </intent-filter>
    </receiver>
	<meta-data android:name="com.mindscapehq.android.raygun4android.apikey"
	           android:value="PASTE_YOUR_API_KEY_HERE" />
	```

	And replace the value in meta-data with your API key, available from your Raygun dashboard.

3. In a central activity, call the following:

	```java
	RaygunClient.Init(getApplicationContext());
	RaygunClient.AttachExceptionHandler();
	```

The above exception handler automatically catches & sends all uncaught exceptions. You can create your own or send from a catch block by calling RaygunClient.Send() and passing in the Throwable.

For a usage example, check out the application in /sample-app.

## Documentation

### Affected user tracking

Raygun supports tracking the unique users who encounter bugs in your apps.

By default the device UUID is transmitted. You can also add the currently logged in user's data like this:

```java
RaygunUserInfo user = new RaygunUserInfo();
user.Identifier = "a@b.com";
user.FullName = "User Name";
user.FirstName = "User";
user.Email = "a@b.com";
user.Uuid = "a uuid";
user.IsAnonymous = false;

RaygunClient.SetUser(user);
```

Any of these properties are optional, for instance you can set just IsAnonymous. There is also a constructor overload if you prefer to specify all in one statement.

`Identifier` should be a unique representation of the current logged in user - we will assume that messages with the same Identifier are the same user. If you do not set it, it will be automatically set to the device UUID.

If the user context changes, for instance on log in/out, you should remember to call SetUser again to store the updated username.

This was upgraded in 1.1, previously a SetUser(string) method was available - this has now been deprecated and will be removed in a future version.

### Version tracking

Set the versionName attribute on <manifest> in your AndroidManifest.xml to be of the form x.x.x.x, where x is a positive integer, and it will be sent with each message. You can then filter by version in the Raygun dashboard.

### Getting/setting/cancelling the error before it is sent

This provider has an OnBeforeSend API to support accessing or mutating the candidate error payload immediately before it is sent, or cancelling the send outright. This is provided as the public method `RaygunClient.SetOnBeforeSend(RaygunOnBeforeSend)`, which takes an instance of a class that implements the `RaygunOnBeforeSend` interface. Your class needs a public `OnBeforeSend` method that takes a `RaygunMessage` parameter, and returns the same.

By example:

```java
class BeforeSendImplementation implements RaygunOnBeforeSend {
    @Override
    public RaygunMessage OnBeforeSend(RaygunMessage message) {
        Log.i("OnBeforeSend", "About to post to Raygun, returning the payload as is...");
        return message;
    }
}


public class FullscreenActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize the activity as normal

        RaygunClient.Init(getApplicationContext());
        RaygunClient.AttachExceptionHandler();
        RaygunClient.SetOnBeforeSend(new BeforeSendImplementation());
    }
}
```

In the example above, the overridden `OnBeforeSend` method will log an info message every time an error is sent.

To mutate the error payload, for instance to change the message:

```java
@Override
public RaygunMessage OnBeforeSend(RaygunMessage message) {
    Log.i("OnBeforeSend", "Changing the message...");

    RaygunMessageDetails details = message.getDetails();
    RaygunErrorMessage error = details.getError();
    error.setMessage("Mutated message");

    return message;
}
```

To cancel the send (prevent the error from reaching the Raygun dashboard) by returning null:

```java
@Override
public RaygunMessage OnBeforeSend(RaygunMessage message) {
    Log.i("OnBeforeSend", "Cancelling sending message to Raygun...");

    return null;
}
```

### Custom error grouping

You can override Raygun's default grouping logic for Android exceptions by setting the grouping key manually in OnBeforeSend (see above):

```java
@Override
public RaygunMessage OnBeforeSend(RaygunMessage message) {
    RaygunMessageDetails details = message.getDetails();
    details.setGroupingKey("foo");

    return message;
}
```

Any error instances with a certain key will be grouped together. The example above will place all errors within one group (as the key is hardcoded to 'foo'). The grouping key is a String and must be between 1 and 100 characters long. You should send all data you care about (for instance, parts of the exception message, stacktrace frames, class names etc) to a hash function (for instance MD5), then pass that to `setGroupingKey`.

### API

The following method overloads are available for initializing RaygunClient:

* RaygunClient.Init(Context context)

* RaygunClient.Init(String version, Context context)

* RaygunClient.Init (Context context, String apiKey)

* RaygunClient.Init (Context context, String apiKey, String version)

	The first two read the API key from the application's AndroidManifest.xml. Whichever Context you pass in will have its API key read. If you want to specify your API key programmatically, use one of the latter two methods.

The following method is available for setting up a pre-made Uncaught Exception Handler, which will automatically send an exception when one reaches it (ie. just before your app crashes):

* RaygunClient.AttachExceptionHandler()

	The tags and custom data will be attached to all exceptions that reaches it. This allows you to automatically send crash data when your app crashes.

	The handler will call any other pre-existing exception handlers you have set up before it sends to Raygun. After it is complete, it will call the default handler, which will crash the app and display the 'close app' user dialog. Exceptions are guaranteed to be sent if your app crashes.

The following methods are available for sending manually; pick one depending on how much extra data you'd like to send:

* RaygunClient.Send(Throwable throwable)

* RaygunClient.Send(Throwable throwable, List tags)

* RaygunClient.Send(Throwable throwable, List tags, Map userCustomData)

These build a RaygunMessage for you then send it. If you'd like to build a message manually you can use:

* RaygunClient.Post(RaygunMessage raygunMessage)

	Note that this will require certain fields to be present - documentation is available at http://raygun.io/raygun-providers/rest-json-api

The following misc methods are available:

* RaygunClient.SetUser(RaygunUserInfo userInfo)

	An object containing data about the currently logged in user - see above for details. Ensure you call this again if the user context changes.

* RaygunClient.SetVersion(String version)

	Stores the version of your application manually. Normally, this is automatically read from AndroidManifest (the versionName attribute on <manifest>) and is provided as a convenience.

* RaygunClient.SetTags(List tags)

  Sets a list of tags which will be sent along with every exception. This will be merged with any other tags passed as the second param of Send().

* RaygunClient.SetUserCustomData(Map userCustomData)

  Sets a key-value Map which, like the tags above, will be sent along with every exception. This will be merged with any other tags passed as the third param of Send().

* RaygunClient.SetOnBeforeSend(RaygunOnBeforeSend onBeforeSendImplementation)

  Provides an instance of a class which has an OnBeforeSend method that can be used to inspect, mutate or cancel the send to the Raygun API immediately before it happens. Can be used to filter arbitrary data.

* RaygunMessageDetails.setGroupingKey(String groupingKey)

  Provides a way to override the default grouping algorithm (see above for details). Any error instances with this key will be grouped together.

### Frequently Asked Questions

* Is there an example project?

	Yup - clone this repository then load the sample-app project. It has been confirmed to run on the emulator for SDK >= 9, and physical devices (4.1.2).

* Not seeing errors in the dashboard?

	Raygun4Android outputs Logcat messages - look for the 'Exception Message HTTP POST result' message - 403 will indicate an invalid API key, 400 a bad message, and 202 will indicate received successfully.

* Environment Data

	A selection of enironment data will be attached and available in the Environment tab in the dashboard, and more in the Raw tab. This data is gathered from android.os.Build - if you wish to see more data you can add them on the userCustomData object.

* What happens when the phone has no internet connection?

	Raygun4Android will save the exception message to disk. When the provider is later asked to send another message it will check if the internet is now available, and if it is, send the cached messages. A maximum of 64 messages will be cached, then overwritten (using a LRU strategy).

  The provider now attaches the device's network connectivity state to the payload when the exception occurs. This is visible under the Request tab in the Raygun dashboard.

### Contributing

Clone this repository, then run `mvn install` to grab the dependencies and install the library to your local Maven repo. Issues and pull requests are welcome.

## Changelog

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
