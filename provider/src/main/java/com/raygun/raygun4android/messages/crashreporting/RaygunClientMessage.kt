package com.raygun.raygun4android.messages.crashreporting

import com.raygun.raygun4android.RaygunSettings

data class RaygunClientMessage(
    var version: String = RaygunSettings.RAYGUN_CLIENT_VERSION,
    var clientUrl: String = "https://github.com/MindscapeHQ/raygun4android",
    var name: String = "Raygun4Android",
)
