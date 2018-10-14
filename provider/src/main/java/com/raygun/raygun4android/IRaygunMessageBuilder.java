package com.raygun.raygun4android;

import android.content.Context;

import com.raygun.raygun4android.messages.crashreporting.RaygunMessage;

import java.util.List;
import java.util.Map;

public interface IRaygunMessageBuilder {
    RaygunMessage build();

    IRaygunMessageBuilder setMachineName(String machineName);

    IRaygunMessageBuilder setExceptionDetails(Throwable throwable);

    IRaygunMessageBuilder setClientDetails();

    IRaygunMessageBuilder setEnvironmentDetails(Context context);

    IRaygunMessageBuilder setVersion(String version);

    IRaygunMessageBuilder setTags(List tags);

    IRaygunMessageBuilder setUserCustomData(Map userCustomData);

    IRaygunMessageBuilder setAppContext(String identifier);

    IRaygunMessageBuilder setUserInfo();

    IRaygunMessageBuilder setNetworkInfo(Context context);

    IRaygunMessageBuilder setGroupingKey(String groupingKey);
}
