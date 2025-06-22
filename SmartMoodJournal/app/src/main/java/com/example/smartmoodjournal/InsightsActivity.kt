package com.example.smartmoodjournal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.smartmoodjournal.databinding.ActivityInsightsBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class InsightsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInsightsBinding
    private lateinit var generativeModel: GenerativeModel
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val allEntries = mutableListOf<MoodEntry>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val REQUEST_CODE_SPEECH = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInsightsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        generativeModel = GenerativeModel(
            modelName = "models/gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        loadAllEntries()

        binding.summaryButton.setOnClickListener {
            generateOverallSummary()
        }

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            showEntriesForDate(calendar.time)
        }

        binding.generateInsightsButton.setOnClickListener {
            val text = binding.userPromptEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                analyzeWithGemini(text)
            }
        }

        binding.voiceInputButton.setOnClickListener {
            startVoiceInput()
        }

        binding.musicButton.setOnClickListener {
            playMoodMusic()
        }
    }

    private fun loadAllEntries() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("moodEntries")
            .get()
            .addOnSuccessListener { result ->
                allEntries.clear()
                for (doc in result) {
                    val note = doc.getString("note") ?: continue
                    val mood = doc.getString("mood") ?: "neutral"
                    val timestamp = doc.getLong("timestamp") ?: continue
                    val aiMood = doc.getString("aiMood") ?: ""
                    val date = Date(timestamp)
                    allEntries.add(MoodEntry(note, mood, aiMood, date))
                }
                binding.insightsText.text = "📅 Select a date to get insights!"
            }
            .addOnFailureListener {
                binding.insightsText.text = "Error loading data 😢"
            }
    }

    private fun showEntriesForDate(date: Date) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selected = sdf.format(date)
        val dayEntries = allEntries.filter { sdf.format(it.date) == selected }

        if (dayEntries.isEmpty()) {
            binding.insightsText.text = "📭 No entries found for selected date."
            binding.musicSection.visibility = android.view.View.GONE
            return
        }

        val prompt = buildDatePrompt(dayEntries, selected)
        analyzeWithGemini(prompt)

        val mood = dayEntries.groupingBy { it.aiMood.ifEmpty { it.mood } }
            .eachCount().maxByOrNull { it.value }?.key ?: "neutral"

        binding.musicSection.visibility =
            if (mood in listOf("happy", "sad", "angry", "relaxed")) android.view.View.VISIBLE
            else android.view.View.GONE
    }

    private fun buildDatePrompt(entries: List<MoodEntry>, date: String): String {
        val combined = entries.joinToString("\n") { "- ${it.note}" }
        return """
            You're an AI therapist.
            Analyze these journal entries for $date.
            Provide:
            1. An emotional summary
            2. A short mental wellness observation
            3. 1 helpful advice

            Entries:
            $combined
        """.trimIndent()
    }

    private fun generateOverallSummary() {
        if (allEntries.isEmpty()) {
            binding.insightTextView.text = "📭 No entries to summarize."
            return
        }

        val combined = allEntries.joinToString("\n") { "- ${it.note} (${it.mood})" }

        val prompt = """
            Analyze the user's overall journaling trends.
            1. Emotional pattern
            2. Dominant moods
            3. 2 helpful tips or affirmations

            Entries:
            $combined
        """.trimIndent()

        analyzeWithGemini(prompt)
    }

    private fun analyzeWithGemini(prompt: String) {
        binding.insightTextView.text = "🧠 Thinking..."

        coroutineScope.launch {
            val reply = withContext(Dispatchers.IO) {
                try {
                    val result = generativeModel.generateContent(prompt)
                    result.text ?: "No response."
                } catch (e: Exception) {
                    Log.e("Gemini", "Error: ${e.message}")
                    "Error: ${e.message}"
                }
            }

            binding.insightTextView.text = reply
            saveConversation(prompt, reply)
        }
    }

    private fun saveConversation(prompt: String, reply: String) {
        val userId = auth.currentUser?.uid ?: return
        val conversation = mapOf(
            "prompt" to prompt,
            "reply" to reply,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .collection("aiConversations")
            .add(conversation)
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your journal thoughts")
        }

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH)
        } catch (e: Exception) {
            Log.e("Voice", "Speech input error: ${e.message}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            binding.userPromptEditText.setText(result?.get(0) ?: "")
        }
    }

    private fun playMoodMusic() {
        val moodText = binding.insightTextView.text.toString().lowercase()
        val query = when {
            "happy" in moodText -> "happy pop playlist"
            "sad" in moodText -> "sad lofi beats"
            "angry" in moodText -> "calm instrumental"
            "relaxed" in moodText -> "relaxing jazz"
            else -> "mood music"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://music.youtube.com/search?q=$query")
            `package` = "com.google.android.apps.youtube.music"
        }
        startActivity(intent)
    }

    data class MoodEntry(
        val note: String,
        val mood: String,
        val aiMood: String,
        val date: Date
    )
}
