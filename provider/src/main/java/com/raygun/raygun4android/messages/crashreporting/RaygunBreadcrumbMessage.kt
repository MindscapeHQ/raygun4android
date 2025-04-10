package com.raygun.raygun4android.messages.crashreporting

import com.google.gson.annotations.SerializedName
import java.util.WeakHashMap

data class RaygunBreadcrumbMessage(
    var message: String?,
    var category: String?,
    @SerializedName("level") private var _level: Int,
    var type: String = "Manual",
    var customData: Map<String, Any?>,
    var timestamp: Long = System.currentTimeMillis(),
    var className: String?,
    var methodName: String?,
    var lineNumber: Int?,
) {
    var level: RaygunBreadcrumbLevel
        get() = RaygunBreadcrumbLevel.entries[_level]
        set(value) {
            _level = value.ordinal
        }

    class Builder(
        internal val message: String?,
    ) {
        private var category: String? = null
        private var level: Int = RaygunBreadcrumbLevel.INFO.ordinal
        private var customData: Map<String, Any?> = WeakHashMap()
        private var className: String? = null
        private var methodName: String? = null
        private var lineNumber: Int? = null

        fun category(category: String?): Builder {
            this.category = category
            return this
        }

        fun level(level: RaygunBreadcrumbLevel): Builder {
            this.level = level.ordinal
            return this
        }

        fun customData(customData: Map<String, Any?>): Builder {
            this.customData = customData
            return this
        }

        fun className(className: String?): Builder {
            this.className = className
            return this
        }

        fun methodName(methodName: String?): Builder {
            this.methodName = methodName
            return this
        }

        fun lineNumber(lineNumber: Int?): Builder {
            this.lineNumber = lineNumber
            return this
        }

        fun build(): RaygunBreadcrumbMessage =
            RaygunBreadcrumbMessage(
                message = message,
                category = category,
                _level = level,
                customData = customData,
                className = className,
                methodName = methodName,
                lineNumber = lineNumber,
            )
    }
}
