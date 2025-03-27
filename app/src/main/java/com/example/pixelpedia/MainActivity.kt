package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot


class MainActivity : AppCompatActivity() {
    private lateinit var settingsIcon: ImageView

    private lateinit var auth: FirebaseAuth

    private lateinit var gameImageView: ImageView
    private lateinit var gameNameTextView: TextView

    // Firestore instance
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        settingsIcon = findViewById(R.id.setting)

        auth = FirebaseAuth.getInstance()

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // User is logged in, display "Hello [user]"
            val username = user.displayName ?: "User" // If displayName is null, fallback to "User"
            Toast.makeText(this, "Hello $username", Toast.LENGTH_SHORT).show()
        } else {
            // User is not logged in
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
        }

        gameImageView = findViewById(R.id.gameImageView) // Make sure this ID exists in your layout
        gameNameTextView = findViewById(R.id.gameNameTextView) // Make sure this ID exists in your layout

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Fetch game data from Firestore
        val gameDocRef = firestore.collection("games").document("DPJYIx6FocQ1FxgCI288")

        gameDocRef.get().addOnSuccessListener { document: DocumentSnapshot? ->
            if (document != null && document.exists()) {
                val gameName = document.getString("gamename")
                val gameImage = document.getString("gameimage")

                // Set text
                gameNameTextView.text = gameName ?: "Unknown Game"

                // Load image using Glide
                Glide.with(this@MainActivity)
                    .load(gameImage)
                    .into(gameImageView)
            } else {
                Toast.makeText(this, "Game not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Error getting document: ${exception.message}", Toast.LENGTH_SHORT).show()
        }

        gameImageView.setOnClickListener {
            val intent = Intent(this, IndividualGameActivity::class.java)
            intent.putExtra("gameId", "DPJYIx6FocQ1FxgCI288")
            startActivity(intent)
        }

        // Redirect to settings
        settingsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Find BottomNavigationView
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Handle navigation item clicks
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
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
