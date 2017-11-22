package main.java.com.mindscapehq.android.raygun4android.messages;

import android.content.Context;

import java.util.List;
import java.util.Map;

public class RaygunMessageDetails {
  private String groupingKey;
  private String machineName;
  private String version = "Not supplied";
  private RaygunErrorMessage error;
  private RaygunEnvironmentMessage environment;
  private RaygunClientMessage client;
  private List tags;
  private Map userCustomData;
  private RaygunAppContext context;
  private RaygunUserContext user;
  private NetworkInfo request;

  // Machine Name
  public String getMachineName() { return machineName; }
  public void setMachineName(String machineName) { this.machineName = machineName; }

  // Version
  public String getVersion() { return version; }
  public void setVersion(String version) { this.version = version; }

  // Error
  public RaygunErrorMessage getError() { return error; }
  public void setError(RaygunErrorMessage error) { this.error = error; }

  // Environment
  public RaygunEnvironmentMessage getEnvironment() { return environment; }
  public void setEnvironment(RaygunEnvironmentMessage environment) { this.environment = environment; }

  // Client
  public RaygunClientMessage getClient() { return client; }
  public void setClient(RaygunClientMessage client) { this.client = client; }

  // Tags
  public List getTags() { return tags; }
  public void setTags(List tags) { this.tags = tags; }

  // Custom Data
  public Map getUserCustomData() { return userCustomData; }
  public void setUserCustomData(Map userCustomData) { this.userCustomData = userCustomData; }

  // App Context
  public RaygunAppContext getAppContext() { return context; }
  public void setAppContext(String identifier) { this.context = new RaygunAppContext(identifier); }

  // User
  public RaygunUserContext getUserContext() { return user; }
  public void setUserContext(Context context) { this.user = new RaygunUserContext(context); }
  public void setUserContext(String user) { this.user = new RaygunUserContext(user); }
  public void setUserContext(RaygunUserInfo userInfo, Context context) { this.user = new RaygunUserContext(userInfo, context); }

  // Network Info
  public NetworkInfo getNetworkInfo(Context context) { return request; }
  public void setNetworkInfo(Context context) { this.request = new NetworkInfo(context); }

  // Grouping Key
  public String getGroupingKey() { return groupingKey; }
  public void setGroupingKey(String groupingKey) { this.groupingKey = groupingKey; }
}
