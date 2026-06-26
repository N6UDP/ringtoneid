package com.example.ringtoneid.audio

/** Where a generated tone should be installed when set as a system default sound. */
enum class RingtonePurpose(val relativePath: String) {
    RINGTONE("Ringtones/"),
    NOTIFICATION("Notifications/"),
    ALARM("Alarms/")
}
