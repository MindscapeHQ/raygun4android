package main.java.com.mindscapehq.android.raygun4android;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class RaygunPostService extends Service
{
  @Override
  public void onStart(Intent intent, int startId) {
    Bundle bundle = intent.getExtras();
    RaygunClient.Init(this.getApplicationContext(), (String) bundle.get("apikey"));
    RaygunClient.Post((String) bundle.get("msg"));
    Log.i("Raygun4Android", "Service posting done");
    this.stopSelf(startId);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}