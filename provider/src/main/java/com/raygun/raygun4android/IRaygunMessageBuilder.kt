package com.raygun.raygun4android

import android.content.Context
import com.raygun.raygun4android.messages.crashreporting.RaygunBreadcrumbMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunMessage

interface IRaygunMessageBuilder {
    fun build(): RaygunMessage?

    fun setMachineName(machineName: String?): IRaygunMessageBuilder?

    fun setExceptionDetails(throwable: Throwable?): IRaygunMessageBuilder?

    fun setClientDetails(): IRaygunMessageBuilder?

    suspend fun setEnvironmentDetails(context: Context): IRaygunMessageBuilder?

    fun setVersion(version: String?): IRaygunMessageBuilder?

    fun setTags(tags: List<*>?): IRaygunMessageBuilder?

    fun setCustomData(customData: Map<*, *>?): IRaygunMessageBuilder?

    fun setAppContext(identifier: String?): IRaygunMessageBuilder?

    fun setUserInfo(): IRaygunMessageBuilder?

    fun setNetworkInfo(context: Context): IRaygunMessageBuilder?

    fun setGroupingKey(groupingKey: String?): IRaygunMessageBuilder?

    fun setBreadcrumbs(breadcrumbs: List<RaygunBreadcrumbMessage>?): IRaygunMessageBuilder?
}
