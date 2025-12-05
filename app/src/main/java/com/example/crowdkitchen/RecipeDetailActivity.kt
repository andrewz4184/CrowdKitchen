package com.example.crowdkitchen

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var repository: RecipeRepository

    private lateinit var textTitle: TextView
    private lateinit var textDescription: TextView
    private lateinit var textIngredients: TextView
    private lateinit var textSteps: TextView
    private lateinit var textCookTime: TextView
    private lateinit var ratingBarUser: RatingBar
    private lateinit var buttonSubmitRating: Button

    private var recipeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        repository = RecipeRepository.getInstance(this)

        // Custom back button in the layout
        findViewById<ImageButton>(R.id.buttonBack).setOnClickListener {
            finish()
        }

        textTitle = findViewById(R.id.textRecipeTitleDetail)
        textDescription = findViewById(R.id.textRecipeDescriptionDetail)
        textIngredients = findViewById(R.id.textIngredients)
        textSteps = findViewById(R.id.textSteps)
        textCookTime = findViewById(R.id.textCookTime)
        ratingBarUser = findViewById(R.id.ratingBarUser)
        buttonSubmitRating = findViewById(R.id.buttonSubmitRating)

        // Read data passed from MainActivity
        recipeId = intent.getStringExtra(EXTRA_RECIPE_ID)
        val title = intent.getStringExtra(EXTRA_RECIPE_TITLE).orEmpty()
        val description = intent.getStringExtra(EXTRA_RECIPE_DESCRIPTION).orEmpty()
        val ingredients = intent.getStringArrayListExtra(EXTRA_RECIPE_INGREDIENTS) ?: arrayListOf()
        val steps = intent.getStringArrayListExtra(EXTRA_RECIPE_STEPS) ?: arrayListOf()
        val cookTime = intent.getIntExtra(EXTRA_RECIPE_COOK_TIME, 0)
        val avgRating = intent.getDoubleExtra(EXTRA_RECIPE_AVG_RATING, 0.0)

        // Populate UI
        textTitle.text = title
        textDescription.text = description
        textIngredients.text = ingredients.joinToString(separator = "\n") { "• $it" }
        textSteps.text = steps.joinToString(separator = "\n") { "• $it" }
        textCookTime.text = "Cook time: $cookTime min"
        ratingBarUser.rating = avgRating.toFloat()

        // When user submits a rating, save it and then go back
        buttonSubmitRating.setOnClickListener {
            val id = recipeId
            if (id == null) {
                Toast.makeText(this, "Missing recipe ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rating = ratingBarUser.rating
            repository.submitRating(
                recipeId = id,
                rating = rating,
                onComplete = {
                    Toast.makeText(this, "Rating submitted!", Toast.LENGTH_SHORT).show()
                    finish() // Go back to previous screen
                },
                onError = {
                    Toast.makeText(this, "Failed to submit rating", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // This only does something if you later enable an ActionBar "up" button.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_RECIPE_ID = "extra_recipe_id"
        const val EXTRA_RECIPE_TITLE = "extra_recipe_title"
        const val EXTRA_RECIPE_DESCRIPTION = "extra_recipe_description"
        const val EXTRA_RECIPE_INGREDIENTS = "extra_recipe_ingredients"
        const val EXTRA_RECIPE_STEPS = "extra_recipe_steps"
        const val EXTRA_RECIPE_COOK_TIME = "extra_recipe_cook_time"
        const val EXTRA_RECIPE_AVG_RATING = "extra_recipe_avg_rating"
    }
}
