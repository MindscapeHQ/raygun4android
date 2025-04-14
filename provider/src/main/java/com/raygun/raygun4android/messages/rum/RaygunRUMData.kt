package com.raygun.raygun4android.messages.rum

data class RaygunRUMData(
    val name: String,
    val timing: RaygunRUMTimingMessage?,
) {
    class Builder(internal val name: String) {
        private var timing: RaygunRUMTimingMessage? = null

        fun timing(timing: RaygunRUMTimingMessage?): Builder {
            this.timing = timing
            return this
        }

        fun build(): RaygunRUMData {
            return RaygunRUMData(name, timing)
        }
    }
}
