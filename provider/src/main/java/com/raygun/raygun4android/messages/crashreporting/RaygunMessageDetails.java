package com.raygun.raygun4android.messages.crashreporting;

import android.content.Context;

import com.raygun.raygun4android.messages.shared.RaygunUserInfo;

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
    private Map customData;
    private RaygunAppContext context;
    private RaygunUserInfo user;
    private NetworkInfo request;
    private List<RaygunBreadcrumbMessage> breadcrumbs;

    // Machine Name
    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    // Version
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    // Error
    public RaygunErrorMessage getError() {
        return error;
    }

    public void setError(RaygunErrorMessage error) {
        this.error = error;
    }

    // Environment
    public RaygunEnvironmentMessage getEnvironment() {
        return environment;
    }

    public void setEnvironment(RaygunEnvironmentMessage environment) {
        this.environment = environment;
    }

    // Client
    public RaygunClientMessage getClient() {
        return client;
    }

    public void setClient(RaygunClientMessage client) {
        this.client = client;
    }

    // Tags
    public List getTags() {
        return tags;
    }

    public void setTags(List tags) {
        this.tags = tags;
    }

    // Custom Data
    public Map getCustomData() {
        return customData;
    }

    public void setCustomData(Map customData) {
        this.customData = customData;
    }

    // App Context
    public RaygunAppContext getAppContext() {
        return context;
    }

    public void setAppContext(String identifier) {
        this.context = new RaygunAppContext(identifier);
    }

    // User
    public RaygunUserInfo getUserInfo() {
        return user;
    }

    public void setUserInfo() {
        this.user = new RaygunUserInfo();
    }

    public void setUserInfo(RaygunUserInfo userInfo) {
        this.user = userInfo;
    }

    // Network Info
    public NetworkInfo getNetworkInfo() {
        return request;
    }

    public void setNetworkInfo(Context context) {
        this.request = new NetworkInfo(context);
    }

    // Grouping Key
    public String getGroupingKey() {
        return groupingKey;
    }

    public void setGroupingKey(String groupingKey) {
        this.groupingKey = groupingKey;
    }

    public void setBreadcrumbs(List<RaygunBreadcrumbMessage> breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }

    public List<RaygunBreadcrumbMessage> getBreadcrumbs() {
        return breadcrumbs;
    }
}
