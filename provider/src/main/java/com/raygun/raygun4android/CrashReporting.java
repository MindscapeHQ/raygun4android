package com.raygun.raygun4android;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;

import com.google.gson.Gson;
import com.raygun.raygun4android.messages.crashreporting.RaygunBreadcrumbMessage;
import com.raygun.raygun4android.messages.crashreporting.RaygunMessage;
import com.raygun.raygun4android.network.RaygunNetworkUtils;
import com.raygun.raygun4android.services.CrashReportingPostService;
import com.raygun.raygun4android.utils.RaygunFileFilter;
import com.raygun.raygun4android.utils.RaygunFileUtils;
import com.raygun.raygun4android.utils.RaygunUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CrashReporting {

    private static RaygunUncaughtExceptionHandler handler;
    private static CrashReportingOnBeforeSend onBeforeSend;
    private static List tags;
    private static Map userCustomData;
    private static List<RaygunBreadcrumbMessage> breadcrumps;
    private static boolean shouldProcessBreadcrumbLocation = false;

    static RaygunUncaughtExceptionHandler getExceptionHandler() {
        return CrashReporting.handler;
    }

    static void setOnBeforeSend(CrashReportingOnBeforeSend onBeforeSend) {
        CrashReporting.onBeforeSend = onBeforeSend;
    }

    static List getTags() {
        return CrashReporting.tags;
    }

    static void setTags(List tags) {
        CrashReporting.tags = tags;
    }

    static Map getUserCustomData() {
        return CrashReporting.userCustomData;
    }

    static void setUserCustomData(Map userCustomData) {
        CrashReporting.userCustomData = userCustomData;
    }

    static void recordBreadcrump(String message) {
        RaygunBreadcrumbMessage breadcrump = new RaygunBreadcrumbMessage.Builder(message).build();
        recordBreadcrump(breadcrump);
    }

    static void recordBreadcrump(RaygunBreadcrumbMessage breadcrump) {
        breadcrumps.add(processBreadcrumpLocation(breadcrump, shouldProcessBreadcrumbLocation(),3));
    }

    private static RaygunBreadcrumbMessage processBreadcrumpLocation(RaygunBreadcrumbMessage breadcrump, boolean shouldProcessBreadcrumbLocation, int stackFrame) {

        if(shouldProcessBreadcrumbLocation && breadcrump.getClassName() == null) {
            StackTraceElement frame = Thread.currentThread().getStackTrace()[stackFrame];

            return new RaygunBreadcrumbMessage.Builder(breadcrump.getMessage())
                .category(breadcrump.getCategory())
                .customData(breadcrump.getCustomData())
                .level(breadcrump.getLevel())
                .className(frame.getClassName())
                .methodName(frame.getMethodName())
                .lineNumber(frame.getLineNumber())
                .build();
        }

        return breadcrump;
    }

    private static boolean shouldProcessBreadcrumbLocation() {
        return CrashReporting.shouldProcessBreadcrumbLocation;
    }

    static void shouldProcessBreadcrumbLocation(boolean shouldProcessBreadcrumbLocation) {
        CrashReporting.shouldProcessBreadcrumbLocation = shouldProcessBreadcrumbLocation;
    }

    static void send(Throwable throwable, List tags) {
        send(throwable, tags, null);
    }

    static void send(Throwable throwable, List tags, Map userCustomData) {
        if (RaygunClient.isCrashReportingEnabled()) {

            RaygunMessage msg = buildMessage(throwable);

            if (msg == null) {
                RaygunLogger.e("Failed to send RaygunMessage - due to invalid message being built");
                return;
            }

            msg.getDetails().setTags(RaygunUtils.mergeLists(getTags(), tags));
            msg.getDetails().setUserCustomData(RaygunUtils.mergeMaps(getUserCustomData(), userCustomData));

            if (onBeforeSend != null) {
                msg = onBeforeSend.onBeforeSend(msg);
                if (msg == null) {
                    return;
                }
            }

            enqueueWorkForCrashReportingService(RaygunClient.getApiKey(), new Gson().toJson(msg));
            postCachedMessages();
        } else {
            RaygunLogger.w("Crash Reporting is not enabled, please enable to use the send() function");
        }
    }

    private static RaygunMessage buildMessage(Throwable throwable) {
        try {
            RaygunMessage msg = RaygunMessageBuilder.instance()
                .setEnvironmentDetails(RaygunClient.getApplicationContext())
                .setMachineName(Build.MODEL)
                .setExceptionDetails(throwable)
                .setClientDetails()
                .setAppContext(RaygunClient.getAppContextIdentifier())
                .setVersion(RaygunClient.getVersion())
                .setNetworkInfo(RaygunClient.getApplicationContext())
                .setBreadcrumbs(breadcrumps)
                .build();

            if (RaygunClient.getVersion() != null) {
                msg.getDetails().setVersion(RaygunClient.getVersion());
            }

            if (RaygunClient.getUser() != null) {
                msg.getDetails().setUserInfo(RaygunClient.getUser());
            } else {
                msg.getDetails().setUserInfo();
            }

            return msg;
        } catch (Exception e) {
            RaygunLogger.e("Failed to build RaygunMessage - " + e);
        }
        return null;
    }

    static void attachExceptionHandler() {
        Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (!(oldHandler instanceof RaygunUncaughtExceptionHandler)) {
            CrashReporting.handler = new RaygunUncaughtExceptionHandler(oldHandler);
            Thread.setDefaultUncaughtExceptionHandler(CrashReporting.handler);
        }
    }

    static void postCachedMessages() {
        if (RaygunNetworkUtils.hasInternetConnection(RaygunClient.getApplicationContext())) {
            File[] fileList = RaygunClient.getApplicationContext().getCacheDir().listFiles(new RaygunFileFilter());
            for (File f : fileList) {
                try {
                    if (RaygunFileUtils.getExtension(f.getName()).equalsIgnoreCase(RaygunSettings.DEFAULT_FILE_EXTENSION)) {
                        ObjectInputStream ois = null;
                        try {
                            ois = new ObjectInputStream(new FileInputStream(f));
                            SerializedMessage serializedMessage = (SerializedMessage) ois.readObject();
                            enqueueWorkForCrashReportingService(RaygunClient.getApiKey(), serializedMessage.message);
                            f.delete();
                        } finally {
                            if (ois != null) {
                                ois.close();
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    RaygunLogger.e("Error loading cached message from filesystem - " + e.getMessage());
                } catch (IOException e) {
                    RaygunLogger.e("Error reading cached message from filesystem - " + e.getMessage());
                } catch (ClassNotFoundException e) {
                    RaygunLogger.e("Error in cached message from filesystem - " + e.getMessage());
                }
            }
        }
    }

    private static void enqueueWorkForCrashReportingService(String apiKey, String jsonPayload) {
        Intent intent = new Intent(RaygunClient.getApplicationContext(), CrashReportingPostService.class);
        intent.setAction("com.raygun.raygun4android.intent.action.LAUNCH_CRASHREPORTING_POST_SERVICE");
        intent.setPackage("com.raygun.raygun4android");
        intent.setComponent(new ComponentName(RaygunClient.getApplicationContext(), CrashReportingPostService.class));

        intent.putExtra("msg", jsonPayload);
        intent.putExtra("apikey", apiKey);

        CrashReportingPostService.enqueueWork(RaygunClient.getApplicationContext(), intent);
    }

    public static class RaygunUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        private Thread.UncaughtExceptionHandler defaultHandler;

        public RaygunUncaughtExceptionHandler(Thread.UncaughtExceptionHandler defaultHandler) {
            this.defaultHandler = defaultHandler;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {

            List tags = new ArrayList();
            tags.add(RaygunSettings.CRASH_REPORTING_UNHANDLED_EXCEPTION_TAG);

            CrashReporting.send(throwable, tags);

            RUM.sendRemainingActivity();

            defaultHandler.uncaughtException(thread, throwable);
        }
    }
}