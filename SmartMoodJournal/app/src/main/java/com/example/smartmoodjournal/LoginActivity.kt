package com.example.smartmoodjournal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(this)

        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()

        val emailField = findViewById<EditText>(R.id.emailField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val googleSignInButton = findViewById<com.google.android.gms.common.SignInButton>(R.id.googleSignInButton)

        val prefilledEmail = intent.getStringExtra("selectedUserEmail")
        if (!prefilledEmail.isNullOrEmpty()) {
            emailField.setText(prefilledEmail)
        }

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        cacheUserToDevice(auth.currentUser)
                        checkUserProfileAndRedirect()
                    } else {
                        Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        val switchTo = intent.getStringExtra("switchToUser")
        val prefs = getSharedPreferences("SmartMoodPrefs", MODE_PRIVATE)
        if (!switchTo.isNullOrEmpty()) {
            val uid = prefs.getString("uid_$switchTo", null)
            if (uid != null && auth.currentUser?.uid == uid) {
                checkUserProfileAndRedirect()
            }
        }
    }

    private fun signInWithGoogle() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                googleSignInLauncher.launch(intentSenderRequest)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                try {
                    val credential: SignInCredential = Identity.getSignInClient(this).getSignInCredentialFromIntent(result.data)
                    val googleIdToken = credential.googleIdToken
                    val email = credential.id

                    if (!googleIdToken.isNullOrEmpty() && !email.isNullOrEmpty()) {
                        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnSuccessListener {
                                cacheUserToDevice(auth.currentUser)
                                checkUserProfileAndRedirect()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                            }
                    }
                } catch (e: ApiException) {
                    Toast.makeText(this, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

    private fun checkUserProfileAndRedirect() {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: return

        getSharedPreferences("SmartMoodPrefs", MODE_PRIVATE).edit().putString("activeUserEmail", email).apply()

        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val intent = if (doc.exists() && !doc.getString("name").isNullOrEmpty()) {
                    Intent(this, HomepageActivity::class.java)
                } else {
                    Intent(this, UserProfileActivity::class.java)
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                Log.e("LOGIN", "Firestore load failed: ${it.message}")
            }
    }

    private fun cacheUserToDevice(user: FirebaseUser?) {
        val email = user?.email ?: return
        val uid = user.uid
        val prefs = getSharedPreferences("SmartMoodPrefs", MODE_PRIVATE)
        val users = prefs.getStringSet("users", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        users.add(email)
        prefs.edit()
            .putStringSet("users", users)
            .putString("uid_$email", uid)
            .apply()
    }
}