package main.java.com.mindscapehq.android.raygun4android;

import android.content.Context;
import main.java.com.mindscapehq.android.raygun4android.messages.RaygunMessage;

import java.util.AbstractList;
import java.util.Map;

public interface IRaygunMessageBuilder {

  RaygunMessage Build();

  IRaygunMessageBuilder SetMachineName(String machineName);

  IRaygunMessageBuilder SetExceptionDetails(Throwable throwable);

  IRaygunMessageBuilder SetClientDetails();

  IRaygunMessageBuilder SetEnvironmentDetails(Context context);

  IRaygunMessageBuilder SetVersion(String version);

  IRaygunMessageBuilder SetTags(AbstractList tags);

  IRaygunMessageBuilder SetUserCustomData(Map userCustomData);
}
