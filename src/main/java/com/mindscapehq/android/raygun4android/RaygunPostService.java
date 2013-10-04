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

  private int tCount = 0;
  private Intent _intent;

  @Override
  public synchronized int onStartCommand(Intent intent, int flags, int startId)
  {
    _intent = intent;
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
        tCount--;
        if (tCount == 0)
        {
          stopSelf();
        }
      }
    });
    t.setDaemon(true);
    tCount++;
    t.start();
    return START_NOT_STICKY;
  }

  private boolean hasInternetConnection()
  {
    ConnectivityManager cm = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    stopService(_intent);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}