package com.example.pixelpedia

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileActivity : AppCompatActivity() {

    private lateinit var profilePic: ImageView
    private lateinit var displayName: TextView
    private lateinit var email: TextView
    private lateinit var location: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        profilePic = findViewById(R.id.profile_image)
        displayName = findViewById(R.id.profile_name)
        location = findViewById(R.id.profile_location)

        val user = auth.currentUser
        if (user != null) {
            displayName.text = user.displayName ?: "No name"
            email.text = user.email ?: "No email"

            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.profile)
                .into(profilePic)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile
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
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

}
