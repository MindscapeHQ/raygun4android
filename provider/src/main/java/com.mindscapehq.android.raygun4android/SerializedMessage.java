package main.java.com.mindscapehq.android.raygun4android;

import java.io.Serializable;

/**
 * SerializedMessage stores and serialises a crash reporting message.
 *
 * The message is wrapped into this class to support future extensibility of what data is to be stored.
 */
public class SerializedMessage implements Serializable {
  public String message;

  public SerializedMessage() {
  }

  public SerializedMessage(String message) {
    this.message = message;
  }
}
