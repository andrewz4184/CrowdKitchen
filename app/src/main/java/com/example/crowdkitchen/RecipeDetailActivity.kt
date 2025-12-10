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
    private lateinit var buttonShare: Button
    private lateinit var buttonBack: ImageButton

    private var recipeId: String? = null

    // Fields used to build the share message
    private lateinit var shareIngredients: String
    private lateinit var shareSteps: String
    private lateinit var shareTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        repository = RecipeRepository.getInstance(this)

        // Back button in layout
        buttonBack = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener { finish() }

        textTitle = findViewById(R.id.textRecipeTitleDetail)
        textDescription = findViewById(R.id.textRecipeDescriptionDetail)
        textIngredients = findViewById(R.id.textIngredients)
        textSteps = findViewById(R.id.textSteps)
        textCookTime = findViewById(R.id.textCookTime)
        ratingBarUser = findViewById(R.id.ratingBarUser)
        buttonSubmitRating = findViewById(R.id.buttonSubmitRating)
        buttonShare = findViewById(R.id.btnShare)

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
        textIngredients.text = ingredients.joinToString("\n") { "• $it" }
        textSteps.text = steps.joinToString("\n") { "• $it" }
        textCookTime.text = "Cook time: $cookTime min"
        ratingBarUser.rating = avgRating.toFloat()

        // Prep values for share feature
        shareTitle = title
        shareIngredients = ingredients.joinToString("\n") { "• $it" }
        shareSteps = steps.joinToString("\n") { "• $it" }

        // Submit rating
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
                    finish()
                },
                onError = {
                    Toast.makeText(this, "Failed to submit rating", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Share button
        buttonShare.setOnClickListener {
            shareRecipe()
        }
    }

    private fun shareRecipe() {
        val body = buildString {
            appendLine("Check out this recipe I found in CrowdKitchen:")
            appendLine()
            appendLine("Title: $shareTitle")
            appendLine()
            appendLine("Ingredients:")
            appendLine(shareIngredients)
            appendLine()
            appendLine("Steps:")
            appendLine(shareSteps)
        }

        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Recipe: $shareTitle")
            putExtra(android.content.Intent.EXTRA_TEXT, body)
        }

        startActivity(android.content.Intent.createChooser(sendIntent, "Share recipe via"))
    }

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
