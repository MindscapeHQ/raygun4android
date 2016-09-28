package main.java.com.mindscapehq.android.raygun4android.messages;

import java.util.UUID;

public class RaygunAppContext {
  public String identifier;

  public RaygunAppContext(String uuid) {
    identifier = uuid;
  }
}
