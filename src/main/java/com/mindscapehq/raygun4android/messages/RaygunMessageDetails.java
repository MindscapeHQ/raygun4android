package main.java.com.mindscapehq.raygun4android.messages;

import java.util.AbstractList;
import java.util.Map;

public class RaygunMessageDetails {

  private String machineName;
  private String version;
  private RaygunErrorMessage error;
  private RaygunEnvironmentMessage environment;
  private RaygunClientMessage client;
  private AbstractList tags;
  private Map userCustomData;

  public void setMachineName(String machineName) {
    this.machineName = machineName;
  }

  public String getMachineName() {
    return machineName;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public RaygunErrorMessage getError() {
    return error;
  }

  public void setError(RaygunErrorMessage error) {
    this.error = error;
  }

  public RaygunEnvironmentMessage getEnvironment() {
    return environment;
  }

  public void setEnvironment(RaygunEnvironmentMessage environment) {
    this.environment = environment;
  }

  public RaygunClientMessage getClient() {
    return client;
  }

  public void setClient(RaygunClientMessage client) {
    this.client = client;
  }

  public AbstractList getTags() {
    return tags;
  }

  public void setTags(AbstractList tags) {
    this.tags = tags;
  }

  public Map getUserCustomData() {
    return userCustomData;
  }

  public void setUserCustomData(Map userCustomData) {
    this.userCustomData = userCustomData;
  }
}
