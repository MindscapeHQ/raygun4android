package main.java.com.mindscapehq.raygun4android;

import main.java.com.mindscapehq.raygun4android.messages.RaygunMessage;

import java.util.AbstractList;
import java.util.Map;

public interface IRaygunMessageBuilder {

  RaygunMessage Build();

  IRaygunMessageBuilder SetDeviceName(String deviceName);

  IRaygunMessageBuilder SetExceptionDetails(Throwable throwable);

  IRaygunMessageBuilder SetClientDetails();

  IRaygunMessageBuilder SetEnvironmentDetails();

  IRaygunMessageBuilder SetVersion();

  IRaygunMessageBuilder SetTags(AbstractList tags);

  IRaygunMessageBuilder SetUserCustomData(Map userCustomData);
}
