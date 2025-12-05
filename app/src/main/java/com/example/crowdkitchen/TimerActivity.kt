package com.example.crowdkitchen

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TimerActivity : AppCompatActivity() {

    private lateinit var repository: RecipeRepository

    private lateinit var seekBar: SeekBar
    private lateinit var timeLabel: TextView
    private lateinit var startButton: Button
    private lateinit var cancelButton: Button
    private lateinit var progressBar: ProgressBar

    private var countDownTimer: CountDownTimer? = null
    private var selectedMinutes: Int = 10
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        repository = RecipeRepository.getInstance(this)
        val settings = repository.getUserSettings()
        selectedMinutes = settings.defaultTimerMinutes

        seekBar = findViewById(R.id.seekBarMinutes)
        timeLabel = findViewById(R.id.textTimeRemaining)
        startButton = findViewById(R.id.buttonStartTimer)
        cancelButton = findViewById(R.id.buttonCancelTimer)
        progressBar = findViewById(R.id.progressBarTimer)

        seekBar.max = 60
        seekBar.progress = selectedMinutes
        updateTimeLabel(selectedMinutes * 60 * 1000L)
        progressBar.max = selectedMinutes * 60

        // New GUI component + listener: SeekBar
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Avoid 0 minutes timer – set a minimum of 1
                selectedMinutes = if (progress < 1) 1 else progress
                updateTimeLabel(selectedMinutes * 60 * 1000L)
                progressBar.max = selectedMinutes * 60
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Persist as new default timer length (local persistent data)
                val newSettings = repository.getUserSettings().copy(
                    defaultTimerMinutes = selectedMinutes
                )
                repository.saveUserSettings(newSettings)
            }
        })

        startButton.setOnClickListener {
            if (!isRunning) {
                startTimer()
            }
        }

        cancelButton.setOnClickListener {
            cancelTimer()
        }
    }

    private fun startTimer() {
        val totalMillis = selectedMinutes * 60 * 1000L
        countDownTimer?.cancel()
        isRunning = true

        countDownTimer = object : CountDownTimer(totalMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimeLabel(millisUntilFinished)
                val secondsRemaining = (millisUntilFinished / 1000L).toInt()
                progressBar.progress = progressBar.max - secondsRemaining
            }

            override fun onFinish() {
                isRunning = false
                updateTimeLabel(0L)
                Toast.makeText(
                    this@TimerActivity,
                    "Time's up! Check your dish.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }

    private fun cancelTimer() {
        countDownTimer?.cancel()
        isRunning = false
        updateTimeLabel(selectedMinutes * 60 * 1000L)
        progressBar.progress = 0
    }

    private fun updateTimeLabel(millis: Long) {
        val totalSeconds = millis / 1000L
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        timeLabel.text = String.format("%02d:%02d", minutes, seconds)
    }

    override fun onStop() {
        super.onStop()
        countDownTimer?.cancel()
    }
}
