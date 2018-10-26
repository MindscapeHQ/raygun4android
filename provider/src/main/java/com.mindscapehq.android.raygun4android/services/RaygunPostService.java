package main.java.com.mindscapehq.android.raygun4android.services;

import android.support.v4.app.JobIntentService;

import main.java.com.mindscapehq.android.raygun4android.RaygunLogger;

abstract class RaygunPostService extends JobIntentService {

  protected static Boolean validateApiKey(String apiKey) {
    if (apiKey.length() == 0) {
      RaygunLogger.e("API key is empty, nothing will be logged or reported");
      return false;
    } else {
      return true;
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }
}
