package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    //Set variables here
    private lateinit var settingsIcon: ImageView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Initialize variables
        settingsIcon = findViewById(R.id.settingsIcon)
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

        //Redirect to settings
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
                    startActivity(Intent(this, MainActivity::class.java))
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







