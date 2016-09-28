package main.java.com.mindscapehq.android.raygun4android;

import main.java.com.mindscapehq.android.raygun4android.messages.RaygunMessage;

public interface RaygunOnBeforeSend {
  RaygunMessage onBeforeSend(RaygunMessage message);

  @Deprecated
  RaygunMessage OnBeforeSend(RaygunMessage message);
}