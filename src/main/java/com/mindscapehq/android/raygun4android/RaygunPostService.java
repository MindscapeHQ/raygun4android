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
    final Bundle bundle = intent.getExtras();
    RaygunClient.Init(this.getApplicationContext(), (String) bundle.get("apikey"));
    Thread t = new Thread(new Runnable()
    {
        @Override
        public void run()
        {
            RaygunClient.Post((String) bundle.get("msg"));
            Log.i("Raygun4Android", "Service posting done");
        }
    });
    t.start();
    this.stopSelf(startId);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}