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
    private lateinit var editIcon: ImageView
    private lateinit var recyclerView: RecyclerView
    private val gameList = mutableListOf<Pair<String, String>>() // Stores (gameId, imageUrl)
    private var isEditMode = false // Track edit mode
    private lateinit var gameAdapter: GameAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        recyclerView = findViewById(R.id.libraryRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        gameAdapter = GameAdapter()
        recyclerView.adapter = gameAdapter

        fetchUserGames()

        // Toggle edit mode
        editIcon = findViewById(R.id.edit)
        editIcon.setOnClickListener {
            isEditMode = !isEditMode
            gameAdapter.notifyDataSetChanged()
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

    private fun deleteGame(gameId: String) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(userId).collection("owned_games")
            .whereEqualTo("gameid", gameId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    db.collection("users").document(userId).collection("owned_games")
                        .document(document.id)
                        .delete()
                        .addOnSuccessListener {
                            gameList.removeAll { it.first == gameId }
                            gameAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { e ->
                            Log.e("LibraryActivity", "Error deleting game", e)
                        }
                }
            }
    }

    inner class GameAdapter : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

        inner class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val gameImageView: ImageView = itemView.findViewById(R.id.gameImageView)
            val deleteIcon: ImageView = itemView.findViewById(R.id.deleteIcon)
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

            // Show delete icon only in edit mode
            holder.deleteIcon.visibility = if (isEditMode) View.VISIBLE else View.GONE

            holder.deleteIcon.setOnClickListener {
                deleteGame(gameId)
            }
        }

        override fun getItemCount(): Int = gameList.size
    }
}
