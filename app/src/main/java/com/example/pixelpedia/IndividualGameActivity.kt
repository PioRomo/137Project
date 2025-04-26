package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class IndividualGameActivity : BaseActivity() {
    private lateinit var gameNameTextView: TextView
    private lateinit var gameGenreTextView: TextView
    private lateinit var gameConsoleTextView: TextView
    private lateinit var gameImageView: ImageView
    private lateinit var gameDescriptionView: TextView
    private lateinit var leftChevron: ImageView
    private lateinit var addButton: ImageView

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private var gameId: String? = null
    private var gameName: String? = null
    private var gameImage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_individual_game)
        enableEdgeToEdge()

        // Initialize views
        gameNameTextView = findViewById(R.id.gameTitle)
        gameGenreTextView = findViewById(R.id.gameGenre)
        gameConsoleTextView = findViewById(R.id.gameConsole)
        gameImageView = findViewById(R.id.gameImage)
        gameDescriptionView = findViewById(R.id.gameDescription)
        leftChevron = findViewById(R.id.leftChevron)
        addButton = findViewById(R.id.addButton)

        // Set up back navigation
        leftChevron.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Get game ID from intent
        gameId = intent.getStringExtra("gameId")
        if (gameId == null) {
            Toast.makeText(this, "Error: Game ID not found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load game details
        loadGameDetails(gameId!!)

        checkIfGameExists()
        addButton.setOnClickListener{
            addGameToUserCollection()
        }
    }

    private fun loadGameDetails(gameId: String) {
        db.collection("games").document(gameId).get()
            .addOnSuccessListener { documentSnapshot: DocumentSnapshot ->
                if (documentSnapshot.exists()) {
                    gameName = documentSnapshot.getString("gamename")
                    val gameGenre = documentSnapshot.getString("gamegenre")
                    val gameConsole = documentSnapshot.getString("gameconsole")
                    gameImage = documentSnapshot.getString("gameimage")
                    val gameDescription = documentSnapshot.getString("gamedescription")

                    // Set text views
                    gameNameTextView.text = gameName ?: "Unknown Game"
                    gameGenreTextView.text = gameGenre ?: "Unknown Genre"
                    gameConsoleTextView.text = gameConsole ?: "Unknown Console"
                    gameDescriptionView.text = gameDescription ?: "No Description Available"

                    // Load image with Glide
                    Glide.with(this).load(gameImage).into(gameImageView)

                    // Check if game is already in the user's collection
                    checkIfGameExists()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load game details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkIfGameExists() {
        val userId = auth.currentUser?.uid ?: return
        val userGamesRef = db.collection("users").document(userId).collection("owned_games")

        gameId?.let { id ->
            userGamesRef.document(id).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    addButton.visibility = ImageView.GONE // Hide button if game is already owned
                } else {
                    addButton.visibility = ImageView.VISIBLE // Show button if game is not owned
                    addButton.setOnClickListener { addGameToUserCollection() } // Add click listener
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error checking game ownership", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addGameToUserCollection() {
        val userId = auth.currentUser?.uid ?: return
        val userGamesRef = db.collection("users").document(userId).collection("owned_games")

        gameId?.let { id ->
            val gameData = hashMapOf(
                "gameid" to id
            )

            userGamesRef.document(id).set(gameData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Game added successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LibraryActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to add game: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
