package com.raygun.raygun4android.messages.crashreporting

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.raygun.raygun4android.messages.shared.RaygunUserInfo

data class RaygunMessageDetails(
    // Grouping Key
    var groupingKey: String? = null,
    // Machine Name
    var machineName: String? = null,
    // Version
    var version: String? = "Not supplied",
    // Error
    var error: RaygunErrorMessage? = null,
    // Environment
    var environment: RaygunEnvironmentMessage? = null,
    // Client
    var client: RaygunClientMessage? = null,
    // Tags
    var tags: List<*>? = null,
    // Custom Data
    @SerializedName("userCustomData") var customData: Map<*, *>? = null,
    // App Context
    var appContext: RaygunAppContext? = null,
    // User
    var userInfo: RaygunUserInfo? = null,
    // Network Info
    var networkInfo: NetworkInfo? = null,
    var breadcrumbs: List<RaygunBreadcrumbMessage>? = null,
) {
    fun setAppContext(identifier: String?) {
        this.appContext = RaygunAppContext(identifier)
    }

    suspend fun setUserInfo() {
        this.userInfo = RaygunUserInfo.anonymous()
    }

    fun setNetworkInfo(context: Context) {
        this.networkInfo = NetworkInfo(context)
    }
}
