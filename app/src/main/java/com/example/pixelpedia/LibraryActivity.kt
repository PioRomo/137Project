package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LibraryActivity : AppCompatActivity() {
    private lateinit var settingsIcon: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var gameAdapter: GameAdapter
    private val gameList = mutableListOf<String>() // Stores image URLs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        recyclerView = findViewById(R.id.libraryRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        gameAdapter = GameAdapter(gameList)
        recyclerView.adapter = gameAdapter

        fetchUserGames()

        // Redirect to settings
        settingsIcon = findViewById(R.id.setting)
        settingsIcon.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_library -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun fetchUserGames() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId).collection("owned_games")
            .get()
            .addOnSuccessListener { ownedGames ->
                val gameIds = ownedGames.documents.mapNotNull { it.getString("gameid") }

                gameIds.forEach { gameId ->
                    db.collection("games").document(gameId).get()
                        .addOnSuccessListener { gameDoc ->
                            val gameImage = gameDoc.getString("gameimage") ?: ""

                            if (gameImage.isNotEmpty()) {
                                gameList.add(gameImage)
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
}

class GameAdapter(private val gameList: List<String>) :
    RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gameImageView: ImageView = itemView.findViewById(R.id.gameImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.game_cell, parent, false)
        return GameViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val imageUrl = gameList[position]
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .into(holder.gameImageView)
    }

    override fun getItemCount(): Int = gameList.size
}
