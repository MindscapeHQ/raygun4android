package com.raygun.raygun4android.messages.crashreporting

import android.annotation.SuppressLint
import android.os.Build
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone

data class RaygunMessage(
    var occurredOn: String? = null,
    var details: RaygunMessageDetails = RaygunMessageDetails()
) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val utcDateTime = LocalDateTime.now(ZoneId.of("UTC"))
            occurredOn = utcDateTime.toString()
        } else {
            @SuppressLint("SimpleDateFormat") val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            df.timeZone = TimeZone.getTimeZone("UTC")
            occurredOn = df.format(Calendar.getInstance().time)
        }
    }
}
