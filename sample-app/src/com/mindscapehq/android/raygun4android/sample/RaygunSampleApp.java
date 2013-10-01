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

    /*Map m = new HashMap();
    m.put("One", 1);
    List l = new LinkedList();
    l.add(3);*/

    RaygunClient.Init(getApplicationContext());
    RaygunClient.AttachExceptionHandler();

    final Button button = (Button) findViewById(R.id.button);
    button.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        int i = 3 / 0;
        //RaygunClient.Send(new Exception("Clicked the button"), new ArrayList(), new HashMap());
      }
    });
  }
}
