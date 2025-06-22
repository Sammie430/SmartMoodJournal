package com.example.smartmoodjournal

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ViewEntryActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val geminiScope = CoroutineScope(Dispatchers.IO)

    private lateinit var entryTextView: TextView
    private lateinit var moodTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var aiResponseText: TextView
    private lateinit var loadingIndicator: ProgressBar

    private lateinit var generativeModel: GenerativeModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_entry)

        entryTextView = findViewById(R.id.entryText)
        moodTextView = findViewById(R.id.moodText)
        dateTextView = findViewById(R.id.dateText)
        aiResponseText = findViewById(R.id.aiResponseText)
        loadingIndicator = findViewById(R.id.loadingIndicator)

        try {
            generativeModel = GenerativeModel(
                modelName = "gemini-pro",
                apiKey = BuildConfig.GEMINI_API_KEY
            )
        } catch (e: Exception) {
            Log.e("GEMINI", "Gemini fallback mode: ${e.message}")
        }

        val entryId = intent.getStringExtra("entryId")
        val uid = auth.currentUser?.uid ?: return

        if (entryId != null) {
            db.collection("users").document(uid).collection("moodEntries").document(entryId)
                .get()
                .addOnSuccessListener { doc ->
                    val note = doc.getString("note") ?: "No entry"
                    val mood = doc.getString("mood") ?: "Unknown"
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))

                    moodTextView.text = "Mood: $mood"
                    dateTextView.text = "Date: $date"
                    entryTextView.text = note

                    generateAIResponse(note, mood)
                }
        }
    }

    private fun generateAIResponse(entry: String, mood: String) {
        loadingIndicator.visibility = ProgressBar.VISIBLE
        aiResponseText.text = ""

        geminiScope.launch {
            try {
                val chat = generativeModel.startChat()
                val prompt = """
                    You are an empathetic AI therapist.
                    The user's mood is "$mood".
                    Here is their journal entry:
                    
                    "$entry"
                    
                    Respond with:
                    - 1 line validating their emotion
                    - 1 line of encouraging or reflective insight
                """.trimIndent()

                val response = chat.sendMessage(prompt)

                runOnUiThread {
                    loadingIndicator.visibility = ProgressBar.GONE
                    aiResponseText.text = response.text ?: "AI couldn't generate a response."
                }

            } catch (e: Exception) {
                Log.e("GEMINI_ERROR", "AI failed: ${e.message}")
                runOnUiThread {
                    loadingIndicator.visibility = ProgressBar.GONE
                    aiResponseText.text = "🤖 Sorry, couldn't connect to AI."
                }
            }
        }
    }
}
