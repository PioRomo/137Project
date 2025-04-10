package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var uploadProfilePicButton: Button
    private lateinit var userName: TextView
    private lateinit var userLocation: TextView
    private lateinit var profileLikes: TextView
    private lateinit var viewLikes: Button
    private lateinit var leftChevron: ImageView



    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        enableEdgeToEdge()

        profileImageView = findViewById(R.id.profilePicture)
        uploadProfilePicButton = findViewById(R.id.uploadProfilePictureButton)
        userName = findViewById(R.id.usernameText)
        userLocation = findViewById(R.id.locationText)
        profileLikes = findViewById(R.id.likesNumberText)
        viewLikes = findViewById(R.id.viewLikesButton)
        leftChevron = findViewById(R.id.leftChevron)

        loadUserProfile()

        uploadProfilePicButton.setOnClickListener {
            showInputDialog()
        }

        viewLikes.setOnClickListener{
            val intent = Intent(this, LikesActivity::class.java)
            startActivity(intent)
        }

        leftChevron.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        // Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_library -> {
                    startActivity(Intent(this, LibraryActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

    }

    private fun showInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter IMGUR Image URL")

        // Set up the input field
        val input = android.widget.EditText(this)
        input.hint = "Enter IMGUR image URL"
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton("OK") { _, _ ->
            val imageUrl = input.text.toString().trim()

            if (imageUrl.isNotEmpty()) {
                saveImageUrlToFirestore(imageUrl)
                Glide.with(this)
                    .load(imageUrl)
                    .into(profileImageView)
            } else {
                Toast.makeText(this, "Please enter a valid image URL", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)

        // Show the dialog
        builder.show()
    }

    private fun saveImageUrlToFirestore(imageUrl: String) {
        userId?.let {
            db.collection("users").document(it)
                .update("profilepic", imageUrl)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile image updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update profile image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadUserProfile() {
        userId?.let {
            db.collection("users").document(it).get()
                .addOnSuccessListener { doc ->
                    val username = doc.getString("username") ?: "Sample User"
                    val location = doc.getString("location") ?: "Sample Location, CA"
                    val numLikes = doc.getLong("likes") ?: 0L
                    userName.text = username
                    userLocation.text = location
                    profileLikes.text = "$numLikes"

                    // Load profile image
                    val imageUrl = doc.getString("profilepic")
                    val placeholderRes = R.drawable.placeholder_game_image // your default image in res/drawable

                    Glide.with(this)
                        .load(imageUrl)
                        .apply(RequestOptions()
                            .placeholder(placeholderRes)
                            .error(placeholderRes)
                            .transform(CircleCrop())) // Circle crop transformation
                        .into(profileImageView)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
        }
    }


}
