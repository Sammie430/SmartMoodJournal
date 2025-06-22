package com.example.smartmoodjournal

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.*

class UserProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var nameField: EditText
    private lateinit var emailField: EditText
    private lateinit var phoneField: EditText
    private lateinit var dobField: EditText
    private lateinit var bioField: EditText
    private lateinit var profileImage: ImageView
    private lateinit var saveButton: Button
    private lateinit var switchUserButton: ImageButton
    private lateinit var logoutButton: Button

    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        nameField = findViewById(R.id.fullNameEditText)
        emailField = findViewById(R.id.emailEditText)
        phoneField = findViewById(R.id.phoneEditText)
        dobField = findViewById(R.id.dobEditText)
        bioField = findViewById(R.id.bioEditText)
        profileImage = findViewById(R.id.profileImageView)
        saveButton = findViewById(R.id.saveProfileButton)
        switchUserButton = findViewById(R.id.switchUserButton)
        logoutButton = findViewById(R.id.logoutButton)

        setupDOBPicker()
        setupProfileImagePicker()

        saveButton.setOnClickListener { saveUserProfile() }
        logoutButton.setOnClickListener { performLogout() }
        switchUserButton.setOnClickListener { showUserSwitchDialog() }
    }

    override fun onResume() {
        super.onResume()
        loadUserDataFromFirestore()
    }

    private fun loadUserDataFromFirestore() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    nameField.setText(doc.getString("name") ?: "")
                    emailField.setText(doc.getString("email") ?: "")
                    phoneField.setText(doc.getString("phone") ?: "")
                    dobField.setText(doc.getString("dob") ?: "")
                    bioField.setText(doc.getString("bio") ?: "")
                    val profileUrl = doc.getString("profileImageUrl")
                    if (!profileUrl.isNullOrEmpty()) {
                        Picasso.get().load(profileUrl).into(profileImage)
                    }
                } else {
                    Toast.makeText(this, "Please update your profile.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupDOBPicker() {
        dobField.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                dobField.setText("$d/${m + 1}/$y")
            }, year, month, day).show()
        }
    }

    private fun setupProfileImagePicker() {
        profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            profileImage.setImageURI(selectedImageUri)
        }
    }

    private fun saveUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        val userMap = hashMapOf<String, Any>(
            "name" to nameField.text.toString(),
            "email" to emailField.text.toString(),
            "phone" to phoneField.text.toString(),
            "dob" to dobField.text.toString(),
            "bio" to bioField.text.toString()
        )

        selectedImageUri?.let { uri ->
            val imageRef = storage.reference.child("profileImages/$uid.jpg")
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        userMap["profileImageUrl"] = downloadUri.toString()
                        saveToFirestore(uid, userMap)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show()
                }
        } ?: saveToFirestore(uid, userMap)
    }

    private fun saveToFirestore(uid: String, data: HashMap<String, Any>) {
        db.collection("users").document(uid)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomepageActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showUserSwitchDialog() {
        val prefs = getSharedPreferences("SmartMoodPrefs", MODE_PRIVATE)
        val users = prefs.getStringSet("users", mutableSetOf())?.toList() ?: emptyList()
        val fullList = users + "➕ Add User"

        AlertDialog.Builder(this)
            .setTitle("Switch User")
            .setItems(fullList.toTypedArray()) { _, which ->
                val selectedUser = fullList[which]
                if (selectedUser == "➕ Add User") {
                    startActivity(Intent(this, LoginActivity::class.java))
                } else {
                    Toast.makeText(this, "Switching to $selectedUser", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.putExtra("switchToUser", selectedUser)
                    startActivity(intent)
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
