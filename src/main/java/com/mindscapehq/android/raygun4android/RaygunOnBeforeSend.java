package main.java.com.mindscapehq.android.raygun4android;

import main.java.com.mindscapehq.android.raygun4android.messages.RaygunMessage;

public interface RaygunOnBeforeSend {
  RaygunMessage onBeforeSend(RaygunMessage message);

  /**
   * @deprecated As of release 3.0.0, replaced by {@link #onBeforeSend(RaygunMessage)}
   */
  @Deprecated RaygunMessage OnBeforeSend(RaygunMessage message);
}