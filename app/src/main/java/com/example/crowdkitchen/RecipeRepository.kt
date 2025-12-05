package com.example.crowdkitchen

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

class RecipeRepository private constructor(private val context: Context) {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------- Local persistent data (UserSettings) ----------

    fun getUserSettings(): UserSettings {
        val defaultMinutes = prefs.getInt(KEY_DEFAULT_TIMER_MINUTES, 10)
        val sortByRating = prefs.getBoolean(KEY_SORT_BY_RATING, true)
        return UserSettings(
            defaultTimerMinutes = defaultMinutes,
            sortByRating = sortByRating
        )
    }

    fun saveUserSettings(settings: UserSettings) {
        prefs.edit()
            .putInt(KEY_DEFAULT_TIMER_MINUTES, settings.defaultTimerMinutes)
            .putBoolean(KEY_SORT_BY_RATING, settings.sortByRating)
            .apply()
    }

    // ---------- Remote persistent data (recipes + ratings via Firebase) ----------

    fun getRecipes(
        onResult: (List<Recipe>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        firestore.collection(COLLECTION_RECIPES)
            .get()
            .addOnSuccessListener { snapshot ->
                val recipes = snapshot.documents.mapNotNull { doc ->
                    val recipe = doc.toObject(Recipe::class.java)
                    recipe?.copy(id = doc.id)
                }
                val settings = getUserSettings()
                val sorted = if (settings.sortByRating) {
                    recipes.sortedByDescending { it.averageRating }
                } else {
                    recipes
                }
                onResult(sorted)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load recipes", e)
                onError(e)
            }
    }

    fun submitRating(
        recipeId: String,
        rating: Float,
        onComplete: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        // Simple scheme: store each rating in a "ratings" subcollection
        val ratingData = mapOf(
            "rating" to rating,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection(COLLECTION_RECIPES)
            .document(recipeId)
            .collection(COLLECTION_RATINGS)
            .add(ratingData)
            .addOnSuccessListener {
                // In a more complete implementation, you'd recompute the averageRating here.
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to submit rating", e)
                onError(e)
            }
    }

    companion object {
        private const val TAG = "RecipeRepository"

        private const val PREFS_NAME = "user_settings"
        private const val KEY_DEFAULT_TIMER_MINUTES = "default_timer_minutes"
        private const val KEY_SORT_BY_RATING = "sort_by_rating"

        private const val COLLECTION_RECIPES = "recipes"
        private const val COLLECTION_RATINGS = "ratings"

        @Volatile
        private var instance: RecipeRepository? = null

        fun getInstance(context: Context): RecipeRepository {
            return instance ?: synchronized(this) {
                instance ?: RecipeRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
