package com.example.cameraplayback.resource

interface ResourcesService {
    fun getString(key: String): String
    fun getString(key: String, vararg args: Any): String
    fun getString(key: Int, vararg args: Any): String
    fun getColor(colorId: Int): Int
}