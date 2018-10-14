package com.raygun.raygun4android.messages.shared;

import com.raygun.raygun4android.RaygunClient;
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
     * @param emailAddress User's email address
     * @param isAnonymous  Whether this user data represents an anonymous user
     * @param identifier   Unique identifier for this user. Set this to the internal identifier you use to look up users,
     *                     or a correlation ID for anonymous users if you have one. It doesn't have to be unique, but we will treat
     *                     any duplicated values as the same user. If you use their email address here, pass it in as the 'emailAddress' parameter too.
     *                     If identifier is not set and/or null, a uuid will be assigned to this field.
     */
    public RaygunUserInfo(String identifier, String firstName, String fullName, String emailAddress, Boolean isAnonymous) {
        validateIdentifier(identifier);
        this.firstName = firstName;
        this.fullName = fullName;
        this.email = emailAddress;
        this.isAnonymous = isAnonymous;
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
        validateIdentifier(identifier);
    }

    public RaygunUserInfo() {
        this.identifier = RaygunNetworkUtils.getDeviceUuid(RaygunClient.getApplicationContext());
    }

    public Boolean getIsAnonymous() {
        return this.isAnonymous;
    }

    public void setAnonymous(Boolean anonymous) {
        isAnonymous = anonymous;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return this.fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String identifier) {
        validateIdentifier(identifier);
    }

    private void validateIdentifier(String identifier) {
        if (identifier == null) {
            this.identifier = RaygunNetworkUtils.getDeviceUuid(RaygunClient.getApplicationContext());
        } else {
            this.identifier = identifier;
        }
    }
}
