package main.java.com.mindscapehq.android.raygun4android.network.http;

import main.java.com.mindscapehq.android.raygun4android.network.RaygunNetworkLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

public final class RaygunHttpsUrlConnection
    extends HttpsURLConnection
{
  private URLConnection original;

  public RaygunHttpsUrlConnection(URLConnection original)
  {
    super(original.getURL());
    this.original = original;
    RaygunNetworkLogger.getInstance().startNetworkCall(this.original.getURL().toExternalForm(), this.original.getURL().toExternalForm(), System.currentTimeMillis(), "HTTPS");
  }

  public void connect()
      throws IOException
  {
    try
    {
      this.original.connect();
    }
    catch (IOException e)
    {
      RaygunNetworkLogger.getInstance().cancelNetworkCall(this.url.toExternalForm(), System.currentTimeMillis(), e.getMessage());
      throw e;
    }
  }

  public void disconnect()
  {
    int statusCode = RaygunNetworkLogger.getStatusCodeFromUrlConnection(this.original);
    RaygunNetworkLogger.getInstance().endNetworkCall(this.url.toExternalForm(), System.currentTimeMillis(), statusCode);

    if ((this.original instanceof HttpURLConnection)) {
      ((HttpURLConnection)this.original).disconnect();
    }
  }

  public InputStream getInputStream()
      throws IOException
  {
    try
    {
      return this.original.getInputStream();
    }
    catch (IOException e)
    {
      RaygunNetworkLogger.getInstance().cancelNetworkCall(this.url.toExternalForm(), System.currentTimeMillis(), e.getMessage());
      throw e;
    }
  }

  public OutputStream getOutputStream()
      throws IOException
  {
    try
    {
      return this.original.getOutputStream();
    }
    catch (IOException e)
    {
      RaygunNetworkLogger.getInstance().cancelNetworkCall(this.url.toExternalForm(), System.currentTimeMillis(), e.getMessage());
      throw e;
    }
  }

  public boolean getAllowUserInteraction()
  {
    return this.original.getAllowUserInteraction();
  }

  public void addRequestProperty(String field, String newValue)
  {
    this.original.addRequestProperty(field, newValue);
  }

  public int getConnectTimeout()
  {
    return this.original.getConnectTimeout();
  }

  public Object getContent()
      throws IOException
  {
    try
    {
      return this.original.getContent();
    }
    catch (IOException e)
    {
      RaygunNetworkLogger.getInstance().cancelNetworkCall(this.url.toExternalForm(), System.currentTimeMillis(), e.getMessage());
      throw e;
    }
  }

  public Object getContent(Class[] types)
      throws IOException
  {
    try
    {
      return this.original.getContent(types);
    }
    catch (IOException e)
    {
      RaygunNetworkLogger.getInstance().cancelNetworkCall(this.url.toExternalForm(), System.currentTimeMillis(), e.getMessage());
      throw e;
    }
  }

  public String getContentEncoding()
  {
    return this.original.getContentEncoding();
  }

  public int getContentLength()
  {
    return this.original.getContentLength();
  }

  public String getContentType()
  {
    return this.original.getContentType();
  }

  public long getDate()
  {
    return this.original.getDate();
  }

  public boolean getDefaultUseCaches()
  {
    return this.original.getDefaultUseCaches();
  }

  public boolean getDoInput()
  {
    return this.original.getDoInput();
  }

  public boolean getDoOutput()
  {
    return this.original.getDoOutput();
  }

  public long getExpiration()
  {
    return this.original.getExpiration();
  }

  public String getHeaderField(int pos)
  {
    return this.original.getHeaderField(pos);
  }

  public String getHeaderField(String key)
  {
    return this.original.getHeaderField(key);
  }

  public long getHeaderFieldDate(String field, long defaultValue)
  {
    return this.original.getHeaderFieldDate(field, defaultValue);
  }

  public int getHeaderFieldInt(String field, int defaultValue)
  {
    return this.original.getHeaderFieldInt(field, defaultValue);
  }

  public String getHeaderFieldKey(int posn)
  {
    return this.original.getHeaderFieldKey(posn);
  }

  public Map<String, List<String>> getHeaderFields()
  {
    return this.original.getHeaderFields();
  }

  public long getIfModifiedSince()
  {
    return this.original.getIfModifiedSince();
  }

  public long getLastModified()
  {
    return this.original.getLastModified();
  }

  public Permission getPermission()
      throws IOException
  {
    try
    {
      return this.original.getPermission();
    }
    catch (IOException e)
    {
      RaygunNetworkLogger.getInstance().cancelNetworkCall(this.url.toExternalForm(), System.currentTimeMillis(), e.getMessage());
      throw e;
    }
  }

  public int getReadTimeout()
  {
    return this.original.getReadTimeout();
  }

  public Map<String, List<String>> getRequestProperties()
  {
    return this.original.getRequestProperties();
  }

  public String getRequestProperty(String field)
  {
    return this.original.getRequestProperty(field);
  }

  public URL getURL()
  {
    return this.original.getURL();
  }

  public boolean getUseCaches()
  {
    return this.original.getUseCaches();
  }

  public void setAllowUserInteraction(boolean newValue)
  {
    this.original.setAllowUserInteraction(newValue);
  }

  public void setConnectTimeout(int timeoutMillis)
  {
    this.original.setConnectTimeout(timeoutMillis);
  }

  public void setDefaultUseCaches(boolean newValue)
  {
    this.original.setDefaultUseCaches(newValue);
  }

  public void setDoInput(boolean newValue)
  {
    this.original.setDoInput(newValue);
  }

  public void setDoOutput(boolean newValue)
  {
    this.original.setDoOutput(newValue);
  }

  public void setIfModifiedSince(long newValue)
  {
    this.original.setIfModifiedSince(newValue);
  }

  public void setReadTimeout(int timeoutMillis)
  {
    this.original.setReadTimeout(timeoutMillis);
  }

  public void setRequestProperty(String field, String newValue)
  {
    this.original.setRequestProperty(field, newValue);
  }

  public void setUseCaches(boolean newValue)
  {
    this.original.setUseCaches(newValue);
  }

  public boolean usingProxy()
  {
    if ((this.original instanceof HttpsURLConnection)) {
      return ((HttpsURLConnection)this.original).usingProxy();
    }
    return false;
  }

  public InputStream getErrorStream()
  {
    if ((this.original instanceof HttpsURLConnection)) {
      return ((HttpsURLConnection)this.original).getErrorStream();
    }
    return null;
  }

  public boolean getInstanceFollowRedirects()
  {
    if ((this.original instanceof HttpsURLConnection)) {
      return ((HttpsURLConnection)this.original).getInstanceFollowRedirects();
    }
    return true;
  }

  public String getRequestMethod()
  {
    if ((this.original instanceof HttpsURLConnection)) {
      return ((HttpsURLConnection)this.original).getRequestMethod();
    }
    return "GET";
  }

  public int getResponseCode()
      throws IOException
  {
    if ((this.original instanceof HttpsURLConnection)) {
      return ((HttpsURLConnection)this.original).getResponseCode();
    }
    return -1;
  }

  public String getResponseMessage()
      throws IOException
  {
    if ((this.original instanceof HttpsURLConnection)) {
      return ((HttpsURLConnection)this.original).getResponseMessage();
    }
    return "";
  }

  public void setChunkedStreamingMode(int chunkLength)
  {
    if ((this.original instanceof HttpsURLConnection)) {
      ((HttpsURLConnection)this.original).setChunkedStreamingMode(chunkLength);
    }
  }

  public void setFixedLengthStreamingMode(int contentLength)
  {
    if ((this.original instanceof HttpsURLConnection)) {
      ((HttpsURLConnection)this.original).setFixedLengthStreamingMode(contentLength);
    }
  }

  public void setInstanceFollowRedirects(boolean followRedirects)
  {
    if ((this.original instanceof HttpsURLConnection)) {
      ((HttpsURLConnection)this.original).setInstanceFollowRedirects(followRedirects);
    }
  }

  public void setRequestMethod(String method)
      throws ProtocolException
  {
    if ((this.original instanceof HttpsURLConnection)) {
      ((HttpsURLConnection)this.original).setRequestMethod(method);
    }
  }

  public String getCipherSuite()
  {
    if ((this.original instanceof HttpsURLConnection)) {
      return ((HttpsURLConnection)this.original).getCipherSuite();
    }
    return "";
  }

  public Certificate[] getLocalCertificates()
  {
    if ((this.original instanceof HttpsURLConnection)) {
      return ((HttpsURLConnection)this.original).getLocalCertificates();
    }
    return null;
  }

  public Certificate[] getServerCertificates()
      throws SSLPeerUnverifiedException
  {
    if ((this.original instanceof HttpsURLConnection)) {
      return ((HttpsURLConnection)this.original).getServerCertificates();
    }
    return null;
  }
}
