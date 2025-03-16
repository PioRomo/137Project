package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {
    private lateinit var settingsIcon: ImageView
    private lateinit var gameImageView: ImageView
    private lateinit var gameNameTextView: TextView
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        settingsIcon = findViewById(R.id.settingsIcon)
        gameImageView = findViewById(R.id.gameImageView) // Make sure this ID exists in your layout
        gameNameTextView = findViewById(R.id.gameNameTextView) // Make sure this ID exists in your layout

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference.child("games").child("12345")

        // Fetch game data
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val gameName = snapshot.child("gamename").getValue(String::class.java)
                    val gameImage = snapshot.child("gameimage").getValue(String::class.java)

                    // Set text
                    gameNameTextView.text = gameName ?: "Unknown Game"

                    // Load image using Glide
                    Glide.with(this@MainActivity)
                        .load(gameImage)
                        .into(gameImageView)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

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
