package com.raygun.raygun4android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnPackageReplaced extends BroadcastReceiver {
  // TODO Kai - this seems to be used nowhere - check if a) this is needed and b) this checks for every single app-update on a system
  @Override
  public void onReceive(Context context, Intent intent) {
      RaygunClient.closePostService();
  }
}
