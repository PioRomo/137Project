package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot

class MainActivity : AppCompatActivity() {
    private lateinit var settingsIcon: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var gameAdapter: GameAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userGreeting: TextView
    private val gameList = mutableListOf<Game>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        settingsIcon = findViewById(R.id.setting)
        recyclerView = findViewById(R.id.gameRecyclerView)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        userGreeting = findViewById(R.id.userGreeting)

        // Check if a user is logged in
        val user = auth.currentUser
        if (user != null) {
            val username = user.displayName ?: "User"
            userGreeting.text = "Hello $username!"
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
        }

        // Setup RecyclerView
        gameAdapter = GameAdapter(gameList) { selectedGame ->
            val intent = Intent(this, IndividualGameActivity::class.java)
            intent.putExtra("gameId", selectedGame.gameId)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = gameAdapter

        // Load games from Firestore
        loadGames()

        // Redirect to settings
        settingsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
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
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadGames() {
        firestore.collection("games").limit(12).get()
            .addOnSuccessListener { documents ->
                gameList.clear()
                for (document in documents) {
                    val game = document.toGame()
                    gameList.add(game)
                }
                gameAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading games: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun DocumentSnapshot.toGame(): Game {
        return Game(
            gameId = this.id,
            gamename = this.getString("gamename") ?: "Unknown Game",
            gameimage = this.getString("gameimage") ?: ""
        )
    }
}
