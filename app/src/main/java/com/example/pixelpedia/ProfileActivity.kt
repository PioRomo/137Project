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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileLocation: TextView

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = ContextCompat.getColor(this, android.R.color.black)
        window.navigationBarColor = ContextCompat.getColor(this, android.R.color.black)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContentView(R.layout.activity_profile)
        enableEdgeToEdge()

        val topBar = findViewById<RelativeLayout>(R.id.topBar)
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.setPadding(0, topInset, 0, 0)
            insets
        }

        profileImageView = findViewById(R.id.profile_image)
        profileName = findViewById(R.id.profile_name)
        profileLocation = findViewById(R.id.profile_location)

        val settingsIcon = findViewById<ImageView>(R.id.setting)
        settingsIcon.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        loadUserProfile()

        profileImageView.setOnClickListener {
            showInputDialog()
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



    private fun showInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter IMGUR Image URL")

        val input = android.widget.EditText(this)
        input.hint = "Enter IMGUR image URL"
        builder.setView(input)

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
                    profileName.text = username
                    profileLocation.text = location

                    val imageUrl = doc.getString("profilepic")
                    val placeholderRes = R.drawable.placeholder_game_image

                    Glide.with(this)
                        .load(imageUrl)
                        .apply(RequestOptions()
                            .placeholder(placeholderRes)
                            .error(placeholderRes)
                            .transform(CircleCrop()))
                        .into(profileImageView)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
