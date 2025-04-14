package com.raygun.raygun4android.messages.rum

data class RaygunRUMTimingMessage(
    val type: String,
    val duration: Long,
) {
    class Builder(
        internal val type: String,
    ) {
        private var duration: Long = 0

        fun duration(duration: Long): Builder {
            this.duration = duration
            return this
        }

        fun build(): RaygunRUMTimingMessage = RaygunRUMTimingMessage(type, duration)
    }
}
