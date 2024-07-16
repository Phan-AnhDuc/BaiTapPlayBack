package com.example.cameraplayback.model

import com.google.gson.annotations.SerializedName

data class PlaybackByDateModel (
    @SerializedName("amount") var amount: Int,   //total files of date
    @SerializedName("list") var playbackFiles: ArrayList<PlaybackFileModel> = ArrayList(),
    @SerializedName("page") val page: Int = 0,
    @SerializedName("page_total") val pageTotal: Int = 0,
)