package com.raygun.raygun4android.messages.rum

data class RaygunRUMMessage(
    var eventData: Array<RaygunRUMDataMessage> = emptyArray(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RaygunRUMMessage

        return eventData.contentEquals(other.eventData)
    }

    override fun hashCode(): Int = eventData.contentHashCode()
}
