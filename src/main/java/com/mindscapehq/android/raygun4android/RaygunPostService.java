package main.java.com.mindscapehq.android.raygun4android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class RaygunPostService extends Service
{

  private int tCount = 0;
  private Intent _intent;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    _intent = intent;
    final Bundle bundle = intent.getExtras();

    Thread t = new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        String message = (String) bundle.get("msg");
        String apiKey = (String) bundle.get("apikey");
        if (hasInternetConnection())
        {
          RaygunClient.Post(apiKey, message);
        }
        else
        {
          synchronized (this)
          {
            int file = 0;
            ArrayList<File> files = new ArrayList<File>(Arrays.asList(getCacheDir().listFiles()));
            if (files != null)
            {
              for (File f : files)
              {
                String fileName = Integer.toString(file) + ".raygun";
                if (RaygunClient.getExtension(f.getName()) == "raygun" && !f.getName().equals(fileName))
                {
                  break;
                }
                else if (file < 64)
                {
                  file++;
                }
                else
                {
                  files.get(0).delete();
                }
              }
            }
            File fn = new File(getCacheDir(), Integer.toString(file) + ".raygun");
            try
            {
              MessageApiKey messageApiKey = new MessageApiKey(apiKey, message);
              ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fn));
              out.writeObject(messageApiKey);
              out.close();
              Log.i("Raygun4Android", "Wrote file " + file + " to disk");
            } catch (FileNotFoundException e)
            {
              Log.e("Raygun4Android", "Error creating file when caching message to filesystem - " + e.getMessage());
            } catch (IOException e)
            {
              Log.e("Raygun4Android", "Error writing message to filesystem - " + e.getMessage());
            }
          }
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