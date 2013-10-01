package com.mindscapehq.android.raygun4android.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import main.java.com.mindscapehq.android.raygun4android.RaygunClient;

import java.util.*;

/**
 * This class shows you how you can use Raygun4Android to send an exception to Raygun when one occurs.
 * Create an application in the Raygun dashboard, copy its API key, paste it into this app's AndroidManifest.xml,
 * then run it and hit the button.
 */
public class RaygunSampleApp extends Activity {

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.main);

    RaygunClient.Init(getApplicationContext()); // This sets up the client with the API key as provided in your AndroidManifest.xml
    RaygunClient.AttachExceptionHandler();      // This attaches a pre-made exception handler to catch all uncaught exceptions, and send them to Raygun

    final Button button = (Button) findViewById(R.id.button);
    button.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        RaygunClient.Send(new Exception("Congratulations, you have sent errors with Raygun4Android")); // Example 1: Manual exception creation & sending

        int i = 3 / 0;                          // Example 2: A real exception will be thrown here, which will be caught & sent by RaygunClient
      }
    });
  }
}
