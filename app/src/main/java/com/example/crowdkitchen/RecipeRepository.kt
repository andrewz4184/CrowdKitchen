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
        val darkMode = prefs.getBoolean(KEY_DARK_MODE, false)
        return UserSettings(
            defaultTimerMinutes = defaultMinutes,
            sortByRating = sortByRating,
            darkMode = darkMode
        )
    }

    fun saveUserSettings(settings: UserSettings) {
        prefs.edit()
            .putInt(KEY_DEFAULT_TIMER_MINUTES, settings.defaultTimerMinutes)
            .putBoolean(KEY_SORT_BY_RATING, settings.sortByRating)
            .putBoolean(KEY_DARK_MODE, settings.darkMode)
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
                    // Classic (non-KTX) Firestore API:
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
        val ratingData = mapOf(
            "rating" to rating,
            "timestamp" to System.currentTimeMillis()
        )

        val recipeRef = firestore
            .collection(COLLECTION_RECIPES)
            .document(recipeId)

        // 1) Store individual rating in subcollection
        recipeRef
            .collection(COLLECTION_RATINGS)
            .add(ratingData)
            .addOnSuccessListener {
                // 2) Update aggregate rating fields on parent recipe doc
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(recipeRef)

                    val oldSum = snapshot.getDouble("ratingSum") ?: 0.0
                    val oldCount = snapshot.getLong("ratingCount") ?: 0L

                    val newSum = oldSum + rating
                    val newCount = oldCount + 1
                    val newAvg = if (newCount > 0) newSum / newCount else 0.0

                    val updates = mapOf(
                        "ratingSum" to newSum,
                        "ratingCount" to newCount,
                        "averageRating" to newAvg
                    )

                    transaction.update(recipeRef, updates)
                    null // transaction block must return something
                }
                    .addOnSuccessListener {
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to update aggregate rating", e)
                        onError(e)
                    }
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
        private const val KEY_DARK_MODE = "dark_mode"

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
