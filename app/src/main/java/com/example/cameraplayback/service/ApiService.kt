package com.example.cameraplayback.service

import com.example.cameraplayback.model.Device
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("devices/{id}")
    suspend fun getDevice(@Path("id") id: Int): Device
}

