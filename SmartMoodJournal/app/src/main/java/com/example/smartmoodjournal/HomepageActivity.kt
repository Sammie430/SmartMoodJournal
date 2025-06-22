package com.example.smartmoodjournal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartmoodjournal.adapter.EntryAdapter
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

class HomepageActivity : AppCompatActivity() {

    private lateinit var greetingTextView: TextView
    private lateinit var moodBubbleTextView: TextView
    private lateinit var recentEntriesRecyclerView: RecyclerView
    private lateinit var startConversationButton: Button
    private lateinit var insightsButton: Button
    private lateinit var profileIcon: ImageView
    private lateinit var musicButton: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: EntryAdapter
    private val entryList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("HOMEPAGE", "onCreate called") // 👈 Add this
        setContentView(R.layout.activity_homepage)

        greetingTextView = findViewById(R.id.greetingTextView)
        moodBubbleTextView = findViewById(R.id.moodBubbleTextView)
        recentEntriesRecyclerView = findViewById(R.id.recentEntriesRecyclerView)
        startConversationButton = findViewById(R.id.startConversationButton)
        insightsButton = findViewById(R.id.insightsButton)
        profileIcon = findViewById(R.id.profileIcon)
        musicButton = findViewById(R.id.musicButton)

        adapter = EntryAdapter(entryList)
        recentEntriesRecyclerView.layoutManager = LinearLayoutManager(this)
        recentEntriesRecyclerView.adapter = adapter

        loadUserGreeting()
        loadRecentEntries()

        profileIcon.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        startConversationButton.setOnClickListener {
            startActivity(Intent(this, AddEntryActivity::class.java))
        }

        insightsButton.setOnClickListener {
            startActivity(Intent(this, InsightsActivity::class.java))
        }

        musicButton.setOnClickListener {
            startActivity(Intent(this, MusicActivity::class.java))
        }
    }

    private fun loadUserGreeting() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get().addOnSuccessListener {
            val name = it.getString("name") ?: "there"
            greetingTextView.text = "Hey $name, here's how you've been..."
        }
    }

    private fun loadRecentEntries() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("moodEntries")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { docs ->
                entryList.clear()
                for (doc in docs) {
                    val title = doc.getString("summary")
                        ?: doc.getString("note")?.take(40)?.plus("...") ?: "Untitled Entry"
                    entryList.add(title)
                }
                moodBubbleTextView.text = getMoodBubble(entryList)
                adapter.notifyDataSetChanged()
            }
    }

    private fun getMoodBubble(entries: List<String>): String {
        return when {
            entries.isEmpty() -> "🧘 You haven't written anything lately. Want to reflect?"
            entries[0].contains("happy", ignoreCase = true) -> "😊 You've been feeling upbeat!"
            entries[0].contains("tired", ignoreCase = true) -> "😴 You might need a recharge."
            entries[0].contains("sad", ignoreCase = true) -> "💙 It's okay to feel down. You're not alone."
            entries[0].contains("angry", ignoreCase = true) -> "😠 Take a breath — you matter."
            entries[0].contains("stressed", ignoreCase = true) -> "🫂 Remember to pause and breathe."
            else -> "💭 Reflective days lately?"
        }
    }
}
