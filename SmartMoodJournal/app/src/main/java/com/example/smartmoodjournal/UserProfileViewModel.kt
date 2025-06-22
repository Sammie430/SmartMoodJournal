package com.example.smartmoodjournal

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

data class UserProfile(val name: String = "", val email: String = "", val age: Int = 0)

class UserProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    fun loadUserData(userId: String, callback: (UserProfile) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(UserProfile::class.java)
                    if (user != null) {
                        callback(user)
                    }
                }
            }
    }

    fun updateUserProfile(userId: String, name: String, age: Int, callback: (Boolean) -> Unit) {
        val userUpdate = mapOf("name" to name, "age" to age)
        db.collection("users").document(userId).update(userUpdate)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
}
