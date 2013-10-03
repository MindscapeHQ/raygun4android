package main.java.com.mindscapehq.android.raygun4android.messages;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class RaygunUserContext
{
  protected static final String PREFS_FILE = "device_id.xml";
  protected static final String PREFS_DEVICE_ID = "device_id";

  private UUID identifier;

  public RaygunUserContext(Context context)
  {
    getDeviceUuid(context);
  }

  private void getDeviceUuid(Context context) {
      synchronized (RaygunAppContext.class) {
        if( identifier == null) {
          final SharedPreferences prefs = context.getSharedPreferences( PREFS_FILE, 0);
          final String id = prefs.getString(PREFS_DEVICE_ID, null );

          if (id != null) {
            identifier = UUID.fromString(id);
          } else {

            final String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

            try {
              if (!"9774d56d682e549c".equals(androidId)) {
                identifier = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
              } else {
                identifier = UUID.randomUUID();
              }
            } catch (UnsupportedEncodingException e) {
              throw new RuntimeException(e);
            }

            prefs.edit().putString(PREFS_DEVICE_ID, identifier.toString() ).commit();
          }
        }
      }
  }

  public String getIdentifier()
  {
    return identifier.toString();
  }
}
