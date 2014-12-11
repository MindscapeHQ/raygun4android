package main.java.com.mindscapehq.android.raygun4android;

import android.content.Context;
import main.java.com.mindscapehq.android.raygun4android.messages.RaygunMessage;

import java.util.List;
import java.util.Map;

public interface IRaygunMessageBuilder {

  RaygunMessage Build();

  IRaygunMessageBuilder SetMachineName(String machineName);

  IRaygunMessageBuilder SetExceptionDetails(Throwable throwable);

  IRaygunMessageBuilder SetClientDetails();

  IRaygunMessageBuilder SetEnvironmentDetails(Context context);

  IRaygunMessageBuilder SetVersion(String version);

  IRaygunMessageBuilder SetTags(List tags);

  IRaygunMessageBuilder SetUserCustomData(Map userCustomData);

  IRaygunMessageBuilder SetAppContext(String identifier);

  IRaygunMessageBuilder SetUserContext(Context context);

  IRaygunMessageBuilder SetNetworkInfo(Context context);
}
