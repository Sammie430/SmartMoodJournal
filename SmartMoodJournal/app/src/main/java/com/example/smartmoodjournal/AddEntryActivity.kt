package com.example.smartmoodjournal

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AddEntryActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var noteEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var voiceButton: ImageButton
    private lateinit var adapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var generativeModel: GenerativeModel
    private lateinit var tts: TextToSpeech

    private val REQUEST_CODE_SPEECH_INPUT = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_entry)

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        noteEditText = findViewById(R.id.noteEditText)
        sendButton = findViewById(R.id.sendButton)
        voiceButton = findViewById(R.id.voiceButton)

        adapter = ChatAdapter(chatMessages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = adapter

        tts = TextToSpeech(this, this)

        generativeModel = GenerativeModel(
            modelName = "models/gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        loadConversation()

        sendButton.setOnClickListener {
            val text = noteEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                addUserMessage(text)
                noteEditText.setText("")
                generateAIResponse(text)
            }
        }

        voiceButton.setOnClickListener {
            startVoiceInput()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
        }
    }

    private fun loadConversation() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("conversation")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                chatMessages.clear()
                for (doc in result) {
                    val isUser = doc.getString("type") == "user"
                    val message = doc.getString("message") ?: continue
                    chatMessages.add(ChatMessage(message, isUser))
                }
                adapter.notifyDataSetChanged()
                chatRecyclerView.scrollToPosition(chatMessages.size - 1)
            }
    }

    private fun addUserMessage(text: String) {
        val timestamp = System.currentTimeMillis()
        chatMessages.add(ChatMessage(text, isUser = true))
        adapter.notifyItemInserted(chatMessages.size - 1)
        chatRecyclerView.scrollToPosition(chatMessages.size - 1)

        val entry = hashMapOf(
            "message" to text,
            "timestamp" to timestamp,
            "type" to "user"
        )

        db.collection("users").document(auth.currentUser!!.uid)
            .collection("conversation")
            .add(entry)
    }

    private fun generateAIResponse(prompt: String) {
        coroutineScope.launch {
            val index = chatMessages.size
            withContext(Dispatchers.Main) {
                chatMessages.add(ChatMessage("Typing...", isUser = false))
                adapter.notifyItemInserted(index)
                chatRecyclerView.scrollToPosition(index)
            }

            try {
                val chat = generativeModel.startChat()
                val response = chat.sendMessage(prompt)
                val reply = response.text ?: "I'm here for you."

                withContext(Dispatchers.Main) {
                    chatMessages[index] = ChatMessage(reply, isUser = false)
                    adapter.notifyItemChanged(index)
                    tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null)
                }

                val aiEntry = hashMapOf(
                    "message" to reply,
                    "timestamp" to System.currentTimeMillis(),
                    "type" to "ai"
                )

                db.collection("users")
                    .document(auth.currentUser!!.uid)
                    .collection("conversation")
                    .add(aiEntry)

            } catch (e: Exception) {
                Log.e("GEMINI_ERROR", "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    chatMessages[index] = ChatMessage("Sorry, I'm having trouble thinking right now.", isUser = false)
                    adapter.notifyItemChanged(index)
                }
            }
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your thoughts")
        }
        startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            noteEditText.setText(result?.get(0) ?: "")
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
