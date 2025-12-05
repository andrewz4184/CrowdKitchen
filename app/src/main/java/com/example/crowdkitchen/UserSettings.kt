package com.example.crowdkitchen

data class UserSettings(
    val defaultTimerMinutes: Int = 10,
    val sortByRating: Boolean = true
)
