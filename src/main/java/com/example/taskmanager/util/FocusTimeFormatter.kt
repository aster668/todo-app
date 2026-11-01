package com.example.taskmanager.util

fun focusTimeLabel(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours > 0 && remainder > 0 -> "${hours}小时${remainder}分"
        hours > 0 -> "${hours}小时"
        else -> "${remainder}分钟"
    }
}

