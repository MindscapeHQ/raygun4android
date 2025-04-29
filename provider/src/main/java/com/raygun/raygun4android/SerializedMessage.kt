package com.raygun.raygun4android

import java.io.Serializable

/**
 * SerializedMessage stores and serialises a crash reporting message.
 *
 * The message is wrapped into this class to support future extensibility of what data is to be
 * stored.
 */
data class SerializedMessage(var message: String) : Serializable
