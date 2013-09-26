package main.java.com.mindscapehq.raygun4android;

import android.content.Context;
import android.util.Log;
import com.mindscapehq.raygun4android.R;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.Security;
import java.util.zip.ZipFile;

public class RaygunHttpClient extends DefaultHttpClient {

  final Context context;

  public RaygunHttpClient(Context context) {
    this.context = context;
  }

  @Override
  protected ClientConnectionManager createClientConnectionManager() {
    SchemeRegistry registry = new SchemeRegistry();
    registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    // Register for port 443 our SSLSocketFactory with our keystore
    // to the ConnectionManager
    registry.register(new Scheme("https", newSslSocketFactory(), 443));
    return new SingleClientConnManager(getParams(), registry);
  }

  private SSLSocketFactory newSslSocketFactory() {
    try {
      Log.d("SSL", "Starting");
      // Get an instance of the Bouncy Castle KeyStore format
      KeyStore trusted = KeyStore.getInstance("BKS");
      // Get the raw resource, which contains the keystore with
      // your trusted certificates (root and any intermediate certs)
      InputStream in = context.getResources().openRawResource(R.raw.rob3);
      //InputStream in = this.getClass().getClassLoader().getResourceAsStream("raw/rob3.mp3");

      Log.i("SSL", in.toString());
      Log.d("SSL", "Loading");
      try {
        // Initialize the keystore with the provided trusted certificates
        // Also provide the password of the keystore
        trusted.load(in, "robbie".toCharArray());
      } finally {
        in.close();
      }
      Log.d("SSL", "Loaded, creating factory");
      // Pass the keystore to the SSLSocketFactory. The factory is responsible
      // for the verification of the server certificate.
      SSLSocketFactory sf = new SSLSocketFactory(trusted);
      Log.d("SSL", "Factory created, verifying");
      // Hostname verification from certificate
      // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d4e506
      sf.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
      Log.d("SSL", "Verified");
      return sf;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
