package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LikesActivity : BaseActivity() {

    private lateinit var leftChevron: ImageView
    private lateinit var likedProfilesRecyclerView: RecyclerView
    private lateinit var adapter : UserProfileAdapter
    private val likedUsersList = mutableListOf<UserProfile>() // Assuming you're using a User data class
    private lateinit var myLikesTextView: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_likes)
        enableEdgeToEdge()

        leftChevron = findViewById(R.id.leftChevron)
        likedProfilesRecyclerView = findViewById(R.id.likedProfilesRecyclerView)
        myLikesTextView = findViewById(R.id.myLikesTextView)

        adapter = UserProfileAdapter(likedUsersList) { user ->
            val intent = Intent(this, OtherProfilesActivity::class.java)
            intent.putExtra("userId", user.userId)
            startActivity(intent)
        }
        likedProfilesRecyclerView.adapter = adapter
        likedProfilesRecyclerView.layoutManager = LinearLayoutManager(this)

        loadLikedProfiles()

        leftChevron.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
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

    private fun loadLikedProfiles() {
        val currentUserId = auth.currentUser?.uid ?: return


        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                val likedProfileIds = document["likedprofiles"] as? List<String> ?: emptyList()

                if (likedProfileIds.isEmpty()) {
                    myLikesTextView.text = "Your Likes (0)"
                    return@addOnSuccessListener
                }

                myLikesTextView.text = "Your Likes (${likedProfileIds.size})"

                likedUsersList.clear()

                for (userId in likedProfileIds) {
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { profileDoc ->
                            val username = profileDoc.getString("username") ?: ""
                            val profileImageUrl = profileDoc.getString("profilepic") ?: ""
                            val likes = (profileDoc.getLong("likes") ?: 0).toInt()

                            val user = UserProfile(userId, username, profileImageUrl, likes)
                            likedUsersList.add(user)
                            adapter.notifyDataSetChanged()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading likes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}



