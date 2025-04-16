package com.raygun.raygun4android.messages.shared

import androidx.annotation.WorkerThread
import com.google.gson.annotations.SerializedName
import com.raygun.raygun4android.RaygunClient
import com.raygun.raygun4android.logging.RaygunLogger
import com.raygun.raygun4android.network.RaygunNetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.Blocking

class RaygunUserInfo
private constructor(
    identifier: String,
    isAnonymous: Boolean,
    firstName: String?,
    fullName: String?,
    email: String?,
) {
    @SerializedName("identifier")
    private var _identifier: String = ""

    @SerializedName("isAnonymous")
    private var _isAnonymous: Boolean = true

    @SerializedName("email")
    private var _email: String? = null

    @SerializedName("fullName")
    private var _fullName: String? = null

    @SerializedName("firstName")
    private var _firstName: String? = null

    val isAnonymous: Boolean
        get() = _isAnonymous

    val identifier: String
        get() = _identifier

    init {
        _identifier = identifier;
        _isAnonymous = isAnonymous;
        _email = email;
        _fullName = fullName;
        _firstName = firstName;
    }

    /**
     * Unique identifier for this user. Set this to the internal identifier you use to look up
     * users, or a correlation ID for anonymous users if you have one. It doesn't have to be unique,
     * but we will treat any duplicated values as the same user. If you use their email address
     * here, please use the full constructor and pass it in as the 'emailAddress' parameter too. If
     * identifier is not set and/or null, a uuid will be assigned to this field.
     *
     * If the identifier is set to null, a new UUID will be generated and the user will be marked
     * as anonymous user. This ID is stored in Android's SharedPreferences and that's a disk
     * operation that should be made asynchronously.
     *
     * This method is synchronous and will block the main thread. Use setIdentifier instead.
     */
    fun setIdentifierSync(identifier: String?) {
        if (identifier.isNullOrEmpty()) {
            runBlocking {
                _identifier = RaygunNetworkUtils.getDeviceUuid(RaygunClient.getApplicationContext())
            }
            _isAnonymous = true
        } else {
            _identifier = identifier
            _isAnonymous = false
        }
    }

    /**
     * Unique identifier for this user. Set this to the internal identifier you use to look up
     * users, or a correlation ID for anonymous users if you have one. It doesn't have to be unique,
     * but we will treat any duplicated values as the same user. If you use their email address
     * here, please use the full constructor and pass it in as the 'emailAddress' parameter too. If
     * identifier is not set and/or null, a uuid will be assigned to this field.
     *
     * If the identifier is set to null, a new UUID will be generated and the user will be marked
     * as anonymous user. This ID is stored in Android's SharedPreferences and that's a disk
     * operation that should be made asynchronously.
     */
    suspend fun setIdentifier(identifier: String?) {
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

    companion object {

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
         *   address here, pass it in as the 'emailAddress' parameter too.
         *
         *   To create an anonymous user, use the asynchronous method anonymous() instead.
         *   If identifier is not set and/or  null, a uuid will be assigned to this field.
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            identifier: String,
            firstName: String? = null,
            fullName: String? = null,
            email: String? = null,
        ): RaygunUserInfo {
            return RaygunUserInfo(
                identifier = identifier,
                isAnonymous = false,
                firstName = firstName,
                fullName = fullName,
                email = email,
            )
        }

        /**
         * Creates an anonymous user synchronously.
         * This method is not recommended for use in the main thread, as it may block the UI and cause ANR errors.
         */
        @JvmStatic
        fun anonymousSync(): RaygunUserInfo {
            return runBlocking(Dispatchers.IO) {
                RaygunLogger.w("Used sync blocking method, use anonymous() instead")
                anonymous()
            }
        }

        /**
         * This static method creates a new `RaygunUserInfo` instance with a random UUID as the identifier.
         * This method is a `suspend` function, because it reads/writes to disk through `SharedPreferences`,
         * so you need to call it from a coroutine when using Kotlin.
         *
         * For Java developers, or for situations where coroutines are not available,
         * the method is available as `RaygunUserInfo.anonymousSync()`,
         * which creates an anonymous user synchronously.
         * This method is not recommended for use in the main thread, as it may block the UI and cause ANR errors.
         */
        @JvmStatic
        suspend fun anonymous(): RaygunUserInfo {
            RaygunLogger.i("Created anonymous user")
            val uuid = RaygunNetworkUtils.getDeviceUuid(RaygunClient.getApplicationContext())
            return RaygunUserInfo(
                identifier = uuid,
                isAnonymous = true,
                firstName = null,
                fullName = null,
                email = null,
            )

        }
    }
}
