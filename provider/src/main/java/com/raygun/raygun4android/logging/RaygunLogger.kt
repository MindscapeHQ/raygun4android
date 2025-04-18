package com.raygun.raygun4android.logging

import com.raygun.raygun4android.RaygunSettings
import timber.log.Timber

object RaygunLogger {
    @JvmStatic
    fun d(string: String?) {
        if (string != null) {
            Timber.tag(RaygunSettings.LOGGING_TAG).d(string)
        }
    }

    @JvmStatic
    fun i(string: String?) {
        if (string != null) {
            Timber.tag(RaygunSettings.LOGGING_TAG).i(string)
        }
    }

    @JvmStatic
    fun w(string: String?) {
        if (string != null) {
            Timber.tag(RaygunSettings.LOGGING_TAG).w(string)
        }
    }

    @JvmStatic
    fun e(string: String?) {
        if (string != null) {
            Timber.tag(RaygunSettings.LOGGING_TAG).e(string)
        }
    }

    @JvmStatic
    fun v(string: String?) {
        if (string != null) {
            Timber.tag(RaygunSettings.LOGGING_TAG).v(string)
        }
    }

    @JvmStatic
    fun responseCode(responseCode: Int) {
        when (responseCode) {
            RaygunSettings.RESPONSE_CODE_ACCEPTED -> d("Request succeeded")
            RaygunSettings.RESPONSE_CODE_BAD_MESSAGE ->
                e(
                    (
                        "Bad message - could not parse the provided JSON. Check all fields are" +
                            " present, especially both occurredOn (ISO 8601 DateTime) and details" +
                            " { } at the top level"
                    ),
                )

            RaygunSettings.RESPONSE_CODE_INVALID_API_KEY ->
                e(
                    "Invalid API Key - The value specified in the header X-ApiKey did not match" +
                        " with an application in Raygun",
                )

            RaygunSettings.RESPONSE_CODE_LARGE_PAYLOAD ->
                e("Request entity too large - The maximum size of a JSON payload is 128KB")

            RaygunSettings.RESPONSE_CODE_RATE_LIMITED ->
                e("Too Many Requests - Plan limit exceeded for month or plan expired")
            else -> d("Response status code: $responseCode")
        }
    }
}
