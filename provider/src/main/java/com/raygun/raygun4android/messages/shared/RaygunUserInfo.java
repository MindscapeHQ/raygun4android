package com.raygun.raygun4android.messages.shared;

import com.raygun.raygun4android.RaygunClient;
import com.raygun.raygun4android.RaygunLogger;
import com.raygun.raygun4android.network.RaygunNetworkUtils;

public class RaygunUserInfo {

    private Boolean isAnonymous;
    private String email;
    private String fullName;
    private String firstName;
    private String identifier;

    /**
     * Set the current user's info to be transmitted - any parameter can be null if the data is not available or you do not wish to send it.
     *
     * @param firstName    The user's first name
     * @param fullName     The user's full name - if setting the first name you should set this too
     * @param email        User's email address
     * @param identifier   Unique identifier for this user. Set this to the internal identifier you use to look up users,
     *                     or a correlation ID for anonymous users if you have one. It doesn't have to be unique, but we will treat
     *                     any duplicated values as the same user. If you use their email address here, pass it in as the 'emailAddress' parameter too.
     *                     If identifier is not set and/or null, a uuid will be assigned to this field.
     */
    public RaygunUserInfo(String identifier, String firstName, String fullName, String email) {
        if (isValidUser(identifier)) {
            this.firstName = firstName;
            this.fullName = fullName;
            this.email = email;
        } else {
            RaygunLogger.i("Ignored firstName, fullName and email because created user was deemed anonymous");
        }
      }

    /**
     * Convenience constructor to be used if you only want to supply an identifier string for the user.
     *
     * @param identifier   Unique identifier for this user. Set this to the internal identifier you use to look up users,
     *                     or a correlation ID for anonymous users if you have one. It doesn't have to be unique, but we will treat
     *                     any duplicated values as the same user. If you use their email address here, please use the full constructor and pass it
     *                     in as the 'emailAddress' parameter too.
     *                     If identifier is not set and/or null, a uuid will be assigned to this field.
     */
    public RaygunUserInfo(String identifier) {
        isValidUser(identifier);
    }

    public RaygunUserInfo() {
        this.identifier = RaygunNetworkUtils.getDeviceUuid(RaygunClient.getApplicationContext());
        this.isAnonymous = true;
    }

    public Boolean getIsAnonymous() {
        return this.isAnonymous;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        if (!getIsAnonymous()) {
            this.email = email;
        } else {
            RaygunLogger.i("Ignored email because current user was deemed anonymous");
        }
    }

    public String getFullName() {
        return this.fullName;
    }

    public void setFullName(String fullName) {
        if (!getIsAnonymous()) {
            this.fullName = fullName;
        } else {
            RaygunLogger.i("Ignored fullName because current user was deemed anonymous");
        }
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        if (!getIsAnonymous()) {
            this.firstName = firstName;
        } else {
            RaygunLogger.i("Ignored firstName because current user was deemed anonymous");
        }
    }

    public String getIdentifier() {
        return this.identifier;
    }

    private Boolean isValidUser(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            this.identifier = RaygunNetworkUtils.getDeviceUuid(RaygunClient.getApplicationContext());
            this.isAnonymous = true;
            RaygunLogger.i("Created anonymous user");
            return false;
        } else {
            this.identifier = identifier;
            this.isAnonymous = false;
            return true;
        }
    }
}
