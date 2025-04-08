package com.raygun.raygun4android

import android.content.Context
import com.raygun.raygun4android.messages.crashreporting.RaygunBreadcrumbMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunClientMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunEnvironmentMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunErrorMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunMessage

class RaygunMessageBuilder private constructor() : IRaygunMessageBuilder {
    private val raygunMessage = RaygunMessage()

    override fun build(): RaygunMessage = raygunMessage

    override fun setMachineName(machineName: String?): IRaygunMessageBuilder {
        raygunMessage.details.machineName = machineName
        return this
    }

    override fun setExceptionDetails(throwable: Throwable?): IRaygunMessageBuilder {
        raygunMessage.details.error = RaygunErrorMessage(throwable)
        return this
    }

    override fun setClientDetails(): IRaygunMessageBuilder {
        raygunMessage.details.client = RaygunClientMessage()
        return this
    }

    override suspend fun setEnvironmentDetails(context: Context): IRaygunMessageBuilder {
        raygunMessage.details.environment = RaygunEnvironmentMessage.invoke(context)
        return this
    }

    override fun setVersion(version: String?): IRaygunMessageBuilder {
        raygunMessage.details.version = version
        return this
    }

    override fun setTags(tags: List<*>?): IRaygunMessageBuilder {
        raygunMessage.details.tags = tags
        return this
    }

    override fun setCustomData(customData: Map<*, *>?): IRaygunMessageBuilder {
        raygunMessage.details.customData = customData
        return this
    }

    override fun setAppContext(identifier: String?): IRaygunMessageBuilder {
        raygunMessage.details.setAppContext(identifier)
        return this
    }

    override fun setUserInfo(): IRaygunMessageBuilder {
        raygunMessage.details.setUserInfo()
        return this
    }

    override fun setNetworkInfo(context: Context): IRaygunMessageBuilder {
        raygunMessage.details.setNetworkInfo(context)
        return this
    }

    override fun setGroupingKey(groupingKey: String?): IRaygunMessageBuilder {
        raygunMessage.details.groupingKey = groupingKey
        return this
    }

    override fun setBreadcrumbs(breadcrumbs: List<RaygunBreadcrumbMessage>?): IRaygunMessageBuilder {
        raygunMessage.details.breadcrumbs = breadcrumbs
        return this
    }

    companion object {
        @JvmStatic fun instance(): RaygunMessageBuilder = RaygunMessageBuilder()
    }
}
