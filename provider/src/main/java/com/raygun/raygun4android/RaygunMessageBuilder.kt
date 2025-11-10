package com.raygun.raygun4android

import android.content.Context
import com.raygun.raygun4android.messages.crashreporting.RaygunBreadcrumbMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunClientMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunEnvironmentMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunErrorMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunMessage
import com.raygun.raygun4android.messages.shared.RaygunUserInfo

class RaygunMessageBuilder {
    private val raygunMessage = RaygunMessage()

    fun build(): RaygunMessage = raygunMessage

    fun setMachineName(machineName: String?): RaygunMessageBuilder {
        raygunMessage.details.machineName = machineName
        return this
    }

    fun setExceptionDetails(throwable: Throwable?): RaygunMessageBuilder {
        raygunMessage.details.error = RaygunErrorMessage(throwable)
        return this
    }

    fun setClientDetails(): RaygunMessageBuilder {
        raygunMessage.details.client = RaygunClientMessage()
        return this
    }

    suspend fun setEnvironmentDetails(context: Context): RaygunMessageBuilder {
        raygunMessage.details.environment = RaygunEnvironmentMessage(context)
        return this
    }

    fun setVersion(version: String?): RaygunMessageBuilder {
        raygunMessage.details.version = version
        return this
    }

    fun setTags(tags: List<*>?): RaygunMessageBuilder {
        raygunMessage.details.tags = tags
        return this
    }

    fun setCustomData(customData: Map<*, *>?): RaygunMessageBuilder {
        raygunMessage.details.customData = customData
        return this
    }

    fun setAppContext(identifier: String?): RaygunMessageBuilder {
        raygunMessage.details.setAppContext(identifier)
        return this
    }

    fun setUserInfo(raygunUserInfo: RaygunUserInfo): RaygunMessageBuilder {
        raygunMessage.details.user = raygunUserInfo
        return this
    }

    fun setNetworkInfo(context: Context): RaygunMessageBuilder {
        raygunMessage.details.setNetworkInfo(context)
        return this
    }

    fun setGroupingKey(groupingKey: String?): RaygunMessageBuilder {
        raygunMessage.details.groupingKey = groupingKey
        return this
    }

    fun setBreadcrumbs(breadcrumbs: List<RaygunBreadcrumbMessage>?): RaygunMessageBuilder {
        raygunMessage.details.breadcrumbs = breadcrumbs
        return this
    }
}
