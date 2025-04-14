package com.raygun.raygun4android.messages.rum

import com.raygun.raygun4android.messages.shared.RaygunUserInfo

data class RaygunRUMDataMessage(
    val sessionId: String?,
    val timestamp: String?,
    val type: String,
    val user: RaygunUserInfo?,
    val version: String?,
    val os: String?,
    val osVersion: String?,
    val platform: String?,
    val data: String?,
) {

    class Builder(internal val type: String) {
        private var sessionId: String? = null
        private var timestamp: String? = null
        private var user: RaygunUserInfo? = null
        private var version: String? = null
        private var os: String? = null
        private var osVersion: String? = null
        private var platform: String? = null
        private var data: String? = null

        fun sessionId(sessionId: String?): Builder {
            this.sessionId = sessionId
            return this
        }

        fun timestamp(timestamp: String?): Builder {
            this.timestamp = timestamp
            return this
        }

        fun user(user: RaygunUserInfo?): Builder {
            this.user = user
            return this
        }

        fun version(version: String?): Builder {
            this.version = version
            return this
        }

        fun os(os: String?): Builder {
            this.os = os
            return this
        }

        fun osVersion(osVersion: String?): Builder {
            this.osVersion = osVersion
            return this
        }

        fun platform(platform: String?): Builder {
            this.platform = platform
            return this
        }

        fun data(data: String?): Builder {
            this.data = data
            return this
        }

        fun build(): RaygunRUMDataMessage {
            return RaygunRUMDataMessage(
                sessionId = sessionId,
                timestamp = timestamp,
                type = type,
                user = user,
                version = version,
                os = os,
                osVersion = osVersion,
                platform = platform,
                data = data,
            )
        }
    }
}
