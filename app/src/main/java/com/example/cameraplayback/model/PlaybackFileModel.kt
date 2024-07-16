package com.example.cameraplayback.model

import com.google.gson.annotations.SerializedName

data class PlaybackFileModel(
    @SerializedName("name") var name: String? = null,   //playback file name
    @SerializedName("event_type") var eventType: String? = null,    // event type of playback file
    @SerializedName("timestamp") var timestamp: Int,    // start time of file (epoch time in second)
    @SerializedName("duration") var duration: Int,   // total time in second
    @SerializedName("page") var page: Int = 0,
    @SerializedName("page_total") var pageTotal: Int = 0,
)