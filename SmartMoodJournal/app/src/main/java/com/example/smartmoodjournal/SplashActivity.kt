package com.example.smartmoodjournal

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("SmartMoodPrefs", MODE_PRIVATE)
            val cachedEmail = prefs.getString("activeUserEmail", null)

            if (auth.currentUser != null && auth.currentUser?.email == cachedEmail) {
                val uid = auth.currentUser?.uid ?: return@postDelayed
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { document ->
                        if (document.exists() && !document.getString("name").isNullOrEmpty()) {
                            startActivity(Intent(this, HomepageActivity::class.java))
                        } else {
                            startActivity(Intent(this, UserProfileActivity::class.java))
                        }
                        finish()
                    }
                    .addOnFailureListener {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
            } else {
                // Not logged in, check if any cached email for direct switch
                if (cachedEmail != null) {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.putExtra("switchToUser", cachedEmail)
                    startActivity(intent)
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                finish()
            }
        }, 2000)
    }
}
