package com.example.crowdkitchen

data class Recipe(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val cookTimeMinutes: Int = 0,
    val averageRating: Double = 0.0
)
