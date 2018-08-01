package main.java.com.mindscapehq.android.raygun4android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class RaygunPostService extends Service {
  private int tCount = 0;
  private Intent intent;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // the intent should never be null:
    // https://developer.android.com/reference/android/app/Service#START_NOT_STICKY
    // but somehow it is
    if (intent == null || intent.getExtras() == null) {
      return START_NOT_STICKY;
    }

    this.intent = intent;
    final Bundle bundle = intent.getExtras();

    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        String message = (String) bundle.get("msg");
        String apiKey = (String) bundle.get("apikey");
        boolean isPulse = ((String) bundle.get("isPulse")).equals("True");
        boolean hasInternet = hasInternetConnection();

        if (isPulse && hasInternet) {
          RaygunClient.postPulseMessage(apiKey, message);
        } else if (!isPulse && hasInternet) {
          RaygunClient.post(apiKey, message);
        } else if (!isPulse && !hasInternet) {
          synchronized (this) {
            int file = 0;
            ArrayList<File> files = new ArrayList<File>(Arrays.asList(getCacheDir().listFiles()));
            if (files != null) {
              for (File f : files) {
                String fileName = Integer.toString(file) + ".raygun";
                if (RaygunClient.getExtension(f.getName()).equals("raygun") && !f.getName().equals(fileName)) {
                  break;
                }
                else if (file < 64) {
                  file++;
                }
                else {
                  files.get(0).delete();
                }
              }
            }
            File fn = new File(getCacheDir(), Integer.toString(file) + ".raygun");
            try {
              MessageApiKey messageApiKey = new MessageApiKey(apiKey, message);
              ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fn));
              out.writeObject(messageApiKey);
              out.close();
            }
            catch (FileNotFoundException e) {
              RaygunLogger.e("Error creating file when caching message to filesystem - " + e.getMessage());
            }
            catch (IOException e) {
              RaygunLogger.e("Error writing message to filesystem - " + e.getMessage());
            }
          }
        }
        tCount--;
        if (tCount == 0) {
          stopSelf();
        }
      }
    });

    t.setDaemon(true);
    tCount++;
    t.start();

    return START_NOT_STICKY;
  }

  private boolean hasInternetConnection() {
  
    ConnectivityManager cm = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

    if (cm != null) {
      NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
      return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
    
    return false;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    stopService(intent);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
