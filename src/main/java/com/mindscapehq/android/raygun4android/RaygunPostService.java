package main.java.com.mindscapehq.android.raygun4android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class RaygunPostService extends Service
{
  @Override
  public void onStart(Intent intent, int startId) {

  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
      final Bundle bundle = intent.getExtras();

      Thread t = new Thread(new Runnable()
      {
          @Override
          public void run()
          {
              if (hasInternetConnection())
              {
                  RaygunClient.Post((String) bundle.get("apikey"), (String) bundle.get("msg"));
              }
              else
              {
                  Log.i("Raygun4Android", "No internet connection available; saving exception to disk");
              }
          }
      });
      t.start();
      this.stopSelf(startId);
      return START_NOT_STICKY;
  }

  private boolean hasInternetConnection()
  {
    ConnectivityManager cm = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
  }

    @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}