package com.example.pixelpedia

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class ChangeUsernameActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var editUsername: EditText
    private lateinit var buttonSubmit: Button
    private lateinit var leftChevron: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_username)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        editUsername = findViewById(R.id.resetPasswordText)
        buttonSubmit = findViewById(R.id.resetPasswordSubmit)
        leftChevron = findViewById(R.id.leftChevron)

        leftChevron.setOnClickListener{
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        buttonSubmit.setOnClickListener {
            val newUsername = editUsername.text.toString().trim()
            if (newUsername.isNotEmpty()) {
                checkIfUsernameExists(newUsername)
            } else {
                Toast.makeText(this, "Enter a username", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkIfUsernameExists(newUsername: String) {
        db.collection("users")
            .whereEqualTo("username", newUsername)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    Toast.makeText(this, "Username already in use", Toast.LENGTH_LONG).show()
                } else {
                    updateUsername(newUsername)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateUsername(newUsername: String) {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid

            db.collection("users").document(userId)
                .update("username", newUsername)
                .addOnSuccessListener {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(newUsername)
                        .build()

                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Username updated!", Toast.LENGTH_SHORT).show()
                                finish() // Redirect back to SettingsActivity
                            }
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
        }
    }
}
