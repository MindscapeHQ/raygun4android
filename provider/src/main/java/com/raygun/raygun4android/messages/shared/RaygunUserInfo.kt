package com.raygun.raygun4android.messages.shared

import com.google.gson.annotations.SerializedName
import com.raygun.raygun4android.RaygunClient
import com.raygun.raygun4android.logging.RaygunLogger
import com.raygun.raygun4android.network.RaygunNetworkUtils

/**
 * Set the current user's info to be transmitted - any parameter can be null if the data is not
 * available or you do not wish to send it.
 *
 * @param firstName The user's first name
 * @param fullName The user's full name - if setting the first name you should set this too
 * @param email User's email address
 * @param identifier Unique identifier for this user. Set this to the internal identifier you use to
 *   look up users, or a correlation ID for anonymous users if you have one. It doesn't have to be
 *   unique, but we will treat any duplicated values as the same user. If you use their email
 *   address here, pass it in as the 'emailAddress' parameter too. If identifier is not set and/or
 *   null, a uuid will be assigned to this field.
 */
class RaygunUserInfo
    @JvmOverloads
    constructor(
        identifier: String? = null,
        firstName: String? = null,
        fullName: String? = null,
        email: String? = null,
    ) {
        @SerializedName("identifier")
        private var _identifier: String

        @SerializedName("isAnonymous")
        private var _isAnonymous: Boolean = true

        @SerializedName("email")
        private var _email: String? = null

        @SerializedName("fullName")
        private var _fullName: String? = null

        @SerializedName("firstName")
        private var _firstName: String? = null

        init {
            if (identifier.isNullOrEmpty()) {
                RaygunLogger.i(
                    "Ignored firstName, fullName and email because created user was deemed" +
                        " anonymous",
                )
                RaygunLogger.i("Created anonymous user")
                _identifier = RaygunNetworkUtils.getDeviceUuid(RaygunClient.getApplicationContext())
                _isAnonymous = true
            } else {
                _identifier = identifier
                _isAnonymous = false
                _firstName = firstName
                _fullName = fullName
                _email = email
            }
        }

        val isAnonymous: Boolean
            get() = _isAnonymous

        val identifier: String
            get() = _identifier

        /**
         * Unique identifier for this user. Set this to the internal identifier you use to look up
         * users, or a correlation ID for anonymous users if you have one. It doesn't have to be unique,
         * but we will treat any duplicated values as the same user. If you use their email address
         * here, please use the full constructor and pass it in as the 'emailAddress' parameter too. If
         * identifier is not set and/or null, a uuid will be assigned to this field.
         */
        fun setIdentifier(identifier: String?) {
            if (identifier.isNullOrEmpty()) {
                _identifier = RaygunNetworkUtils.getDeviceUuid(RaygunClient.getApplicationContext())
                _isAnonymous = true
            } else {
                _identifier = identifier
                _isAnonymous = false
            }
        }

        var email: String?
            get() = _email
            set(value) {
                if (!isAnonymous) {
                    _email = value
                } else {
                    RaygunLogger.i("Ignored email because current user was deemed anonymous")
                }
            }

        var fullName: String?
            get() = _fullName
            set(value) {
                if (!isAnonymous) {
                    _fullName = value
                } else {
                    RaygunLogger.i("Ignored fullName because current user was deemed anonymous")
                }
            }

        var firstName: String?
            get() = _firstName
            set(value) {
                if (!isAnonymous) {
                    _firstName = value
                } else {
                    RaygunLogger.i("Ignored firstName because current user was deemed anonymous")
                }
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RaygunUserInfo

            if (_isAnonymous != other._isAnonymous) return false
            if (_identifier != other._identifier) return false
            if (_email != other._email) return false
            if (_fullName != other._fullName) return false
            if (_firstName != other._firstName) return false

            return true
        }

        override fun hashCode(): Int {
            var result = _isAnonymous.hashCode()
            result = 31 * result + _identifier.hashCode()
            result = 31 * result + (_email?.hashCode() ?: 0)
            result = 31 * result + (_fullName?.hashCode() ?: 0)
            result = 31 * result + (_firstName?.hashCode() ?: 0)
            return result
        }
    }
