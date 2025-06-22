// 🎵 Mood-Based MusicActivity.kt

package com.example.smartmoodjournal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class MusicActivity : AppCompatActivity() {

    private lateinit var moodTextView: TextView
    private lateinit var suggestButton: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)

        moodTextView = findViewById(R.id.moodTextView)
        suggestButton = findViewById(R.id.suggestMusicButton)

        fetchMoodAndSuggest()

        suggestButton.setOnClickListener {
            val mood = moodTextView.text.toString().lowercase(Locale.getDefault())
            val query = when {
                "happy" in mood -> "happy+pop+playlist"
                "sad" in mood -> "chill+lofi+beats"
                "angry" in mood -> "calming+instrumental+music"
                "relaxed" in mood -> "jazz+for+studying"
                else -> "mood+booster+music"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://music.youtube.com/search?q=$query")
                `package` = "com.google.android.apps.youtube.music"
            }
            startActivity(intent)
        }
    }

    private fun fetchMoodAndSuggest() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).collection("moodEntries")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { docs ->
                val moodCounts = mutableMapOf<String, Int>()
                for (doc in docs) {
                    val mood = doc.getString("aiMood")?.lowercase(Locale.getDefault())
                        ?: doc.getString("mood")?.lowercase(Locale.getDefault()) ?: continue
                    moodCounts[mood] = moodCounts.getOrDefault(mood, 0) + 1
                }
                val topMood = moodCounts.maxByOrNull { it.value }?.key ?: "neutral"
                moodTextView.text = "Detected Mood: ${topMood.replaceFirstChar { it.uppercase() }}"
            }
            .addOnFailureListener {
                moodTextView.text = "Couldn't detect mood 😕"
            }
    }
}