package com.example.cameraplayback.utils.extension

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


/**
 * Get the mid time of the current day（12:00:00）
 *
 * @param currentTime
 * @return
 */
fun getMidDayTime(currentTime: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.time = Date(currentTime)
    calendar[Calendar.HOUR_OF_DAY] = 12
    calendar[Calendar.MINUTE] = 0
    calendar[Calendar.SECOND] = 0
    calendar[Calendar.MILLISECOND] = 0
    return calendar.timeInMillis
}

/**
 * Get the ending point of the current day（23:59:59 999ms）
 *
 * @param currentTime
 * @return
 */
fun getEndOfDay(currentTime: Long): Long {
    val start = getStartOfDay(currentTime)
    return start + 24 * 60 * 60 * 1000 - 1
}

fun getStartOfDay(currentTime: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.time = Date(currentTime)
    calendar[Calendar.HOUR_OF_DAY] = 0
    calendar[Calendar.MINUTE] = 0
    calendar[Calendar.SECOND] = 0
    calendar[Calendar.MILLISECOND] = 0
    return calendar.timeInMillis
}

fun getCurrentDate(currentTime: Long, pattern: String): String? {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(currentTime)
}

/**
 * Pattern date format
 */
const val yearMonthDayHourMinutesSecondsPattern = "yyyy_MM_dd_HH_mm_ss_"
const val playbackFileNameToTimePattern = "yyyy_MM_dd_HH_mm_ss"
const val dayMonthYear = "dd/MM/yyyy"
const val yearMonthDay = "yyyy-MM-dd"