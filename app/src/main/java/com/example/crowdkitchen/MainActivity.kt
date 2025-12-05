package com.example.crowdkitchen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {

    private lateinit var repository: RecipeRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecipeAdapter
    private val recipes = mutableListOf<Recipe>()

    private lateinit var searchEditText: EditText
    private lateinit var micButton: ImageButton
    private lateinit var buttonOpenTimer: Button
    private lateinit var buttonOpenSettings: Button

    private lateinit var adView: AdView

    private var speechRecognizer: SpeechRecognizer? = null

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startVoiceRecognition()
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme BEFORE inflating layout
        val repoForTheme = RecipeRepository.getInstance(this)
        val settings = repoForTheme.getUserSettings()
        AppCompatDelegate.setDefaultNightMode(
            if (settings.darkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = repoForTheme

        // Initialize ads
        MobileAds.initialize(this) {}
        adView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        searchEditText = findViewById(R.id.editTextSearch)
        micButton = findViewById(R.id.buttonMic)
        buttonOpenTimer = findViewById(R.id.buttonOpenTimer)
        buttonOpenSettings = findViewById(R.id.buttonOpenSettings)

        recyclerView = findViewById(R.id.recyclerRecipes)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecipeAdapter(recipes) { recipe ->
            openRecipeDetail(recipe)
        }
        recyclerView.adapter = adapter

        micButton.setOnClickListener {
            checkAudioPermissionAndStart()
        }

        buttonOpenTimer.setOnClickListener {
            val intent = Intent(this, TimerActivity::class.java)
            startActivity(intent)
        }

        buttonOpenSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        loadRecipes()
    }

    private fun loadRecipes() {
        repository.getRecipes(
            onResult = { list ->
                recipes.clear()
                recipes.addAll(list)
                adapter.updateData(recipes)
            },
            onError = {
                Toast.makeText(this, "Failed to load recipes", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun openRecipeDetail(recipe: Recipe) {
        val intent = Intent(this, RecipeDetailActivity::class.java).apply {
            putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.id)
            putExtra(RecipeDetailActivity.EXTRA_RECIPE_TITLE, recipe.title)
            putExtra(RecipeDetailActivity.EXTRA_RECIPE_DESCRIPTION, recipe.description)
            putStringArrayListExtra(
                RecipeDetailActivity.EXTRA_RECIPE_INGREDIENTS,
                ArrayList(recipe.ingredients)
            )
            putStringArrayListExtra(
                RecipeDetailActivity.EXTRA_RECIPE_STEPS,
                ArrayList(recipe.steps)
            )
            putExtra(RecipeDetailActivity.EXTRA_RECIPE_COOK_TIME, recipe.cookTimeMinutes)
            putExtra(RecipeDetailActivity.EXTRA_RECIPE_AVG_RATING, recipe.averageRating)
        }
        startActivity(intent)
    }

    // ---------- Voice Search ----------

    private fun checkAudioPermissionAndStart() {
        val permission = Manifest.permission.RECORD_AUDIO
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED ->
                startVoiceRecognition()

            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(this, "Microphone access is needed for voice search.", Toast.LENGTH_SHORT).show()
                audioPermissionLauncher.launch(permission)
            }

            else -> audioPermissionLauncher.launch(permission)
        }
    }

    private fun startVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        Toast.makeText(this@MainActivity, "Voice error: $error", Toast.LENGTH_SHORT).show()
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val query = matches?.firstOrNull() ?: return
                        searchEditText.setText(query)
                        filterRecipes(query)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak recipe name or ingredient")
        }

        speechRecognizer?.startListening(intent)
    }

    private fun filterRecipes(query: String) {
        val lower = query.lowercase()
        val filtered = recipes.filter { recipe ->
            recipe.title.lowercase().contains(lower) ||
                    recipe.description.lowercase().contains(lower) ||
                    recipe.ingredients.any { it.lowercase().contains(lower) }
        }
        adapter.updateData(filtered)
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }

    // ---------- RecyclerView Adapter ----------

    private inner class RecipeAdapter(
        private var data: List<Recipe>,
        private val onItemClick: (Recipe) -> Unit
    ) : RecyclerView.Adapter<RecipeViewHolder>() {

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecipeViewHolder {
            val view = layoutInflater.inflate(R.layout.item_recipe, parent, false)
            return RecipeViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
            val recipe = data[position]
            holder.bind(recipe)
        }

        override fun getItemCount(): Int = data.size

        fun updateData(newData: List<Recipe>) {
            data = newData
            notifyDataSetChanged()
        }
    }

    private inner class RecipeViewHolder(itemView: android.view.View) :
        RecyclerView.ViewHolder(itemView) {

        private val titleText: android.widget.TextView =
            itemView.findViewById(R.id.textRecipeTitle)
        private val ratingBar: android.widget.RatingBar =
            itemView.findViewById(R.id.ratingBarAverage)

        fun bind(recipe: Recipe) {
            titleText.text = recipe.title
            ratingBar.rating = recipe.averageRating.toFloat()

            itemView.setOnClickListener {
                openRecipeDetail(recipe)
            }
        }
    }
}
