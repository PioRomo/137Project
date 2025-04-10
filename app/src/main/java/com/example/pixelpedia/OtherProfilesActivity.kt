package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OtherProfilesActivity : AppCompatActivity() {

    private lateinit var profilePicture: ImageView
    private lateinit var usernameText: TextView
    private lateinit var locationText: TextView
    private lateinit var likesNumberText: TextView
    private lateinit var libraryRecyclerView: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var leftChevron: ImageView

    private val db = FirebaseFirestore.getInstance()
    private lateinit var gameAdapter: GameAdapter
    private lateinit var userId: String
    private val gameList = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_profiles)

        // UI references
        profilePicture = findViewById(R.id.profilePicture)
        usernameText = findViewById(R.id.usernameText)
        locationText = findViewById(R.id.locationText)
        likesNumberText = findViewById(R.id.likesNumberText)
        libraryRecyclerView = findViewById(R.id.libraryRecyclerView)
        bottomNav = findViewById(R.id.bottomNav)
        leftChevron = findViewById(R.id.leftChevron)

        // Get passed user ID or username
        userId = intent.getStringExtra("userId") ?: return

        // Fetch profile data
        fetchUserData()

        //Display User Library
        libraryRecyclerView = findViewById(R.id.libraryRecyclerView)
        libraryRecyclerView.layoutManager = GridLayoutManager(this, 3)

        gameAdapter = GameAdapter()
        libraryRecyclerView.adapter = gameAdapter

        fetchUserGames()


        // Back button logic
        leftChevron.setOnClickListener {
            finish()
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

    private fun fetchUserData(){
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username")
                    val location = document.getString("location")
                    val likes = document.getLong("likes")?.toInt() ?: 0
                    val profileImageUrl = document.getString("profilepic")

                    usernameText.text = username
                    locationText.text = location
                    likesNumberText.text = likes.toString()

                    Glide.with(this)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.placeholder_game_image)
                        .into(profilePicture)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUserGames() {
        db.collection("users").document(userId).collection("owned_games")
            .get()
            .addOnSuccessListener { ownedGames ->
                val gameIds = ownedGames.documents.mapNotNull { it.getString("gameid") }

                gameIds.forEach { gameId ->
                    db.collection("games").document(gameId).get()
                        .addOnSuccessListener { gameDoc ->
                            val gameImage = gameDoc.getString("gameimage") ?: ""

                            if (gameImage.isNotEmpty()) {
                                gameList.add(Pair(gameId, gameImage))
                                gameAdapter.notifyDataSetChanged()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("LibraryActivity", "Error fetching game details", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("LibraryActivity", "Error fetching owned games", e)
            }
    }

    inner class GameAdapter : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

        inner class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val gameImageView: ImageView = itemView.findViewById(R.id.gameImageView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.game_cell, parent, false)
            return GameViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
            val (gameId, imageUrl) = gameList[position]

            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .into(holder.gameImageView)


            holder.gameImageView.setOnClickListener {
                val intent = Intent(holder.itemView.context, IndividualGameActivity::class.java)
                intent.putExtra("gameId", gameId) // Pass gameId to next screen
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = gameList.size
    }
}
