package com.example.crowdkitchen

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var repository: RecipeRepository

    private lateinit var editDefaultTimer: EditText
    private lateinit var switchSortByRating: SwitchCompat
    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var buttonSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        val repoForTheme = RecipeRepository.getInstance(this)
        val currentSettings = repoForTheme.getUserSettings()
        AppCompatDelegate.setDefaultNightMode(
            if (currentSettings.darkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        repository = repoForTheme

        findViewById<ImageButton>(R.id.buttonBackSettings).setOnClickListener {
            finish()
        }

        editDefaultTimer = findViewById(R.id.editDefaultTimerMinutes)
        switchSortByRating = findViewById(R.id.switchSortByRating)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        buttonSave = findViewById(R.id.buttonSaveSettings)

        editDefaultTimer.setText(currentSettings.defaultTimerMinutes.toString())
        switchSortByRating.isChecked = currentSettings.sortByRating
        switchDarkMode.isChecked = currentSettings.darkMode

        buttonSave.setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings() {
        val minutesText = editDefaultTimer.text.toString().trim()
        val minutes = minutesText.toIntOrNull() ?: 10

        val newSettings = UserSettings(
            defaultTimerMinutes = minutes,
            sortByRating = switchSortByRating.isChecked,
            darkMode = switchDarkMode.isChecked
        )

        repository.saveUserSettings(newSettings)

        AppCompatDelegate.setDefaultNightMode(
            if (newSettings.darkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
