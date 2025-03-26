package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore


class IndividualGameActivity : AppCompatActivity() {
    private var gameNameTextView: TextView? = null
    private var gameGenreTextView: TextView? = null
    private var gameConsoleTextView: TextView? = null
    private var gameImageView: ImageView? = null
    private lateinit var leftChevron: ImageView
    private lateinit var addButton: ImageView
    private var db: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_individual_game)
        enableEdgeToEdge()

        gameNameTextView = findViewById<TextView>(R.id.gameTitle)
        gameGenreTextView = findViewById<TextView>(R.id.gameGenre)
        gameConsoleTextView = findViewById<TextView>(R.id.gameConsole)
        gameImageView = findViewById<ImageView>(R.id.gameImage)
        leftChevron =  findViewById(R.id.leftChevron)
        addButton = findViewById(R.id.addButton)

        db = FirebaseFirestore.getInstance()

        leftChevron.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        addButton.setOnClickListener{
            //add logic!
            Toast.makeText(this, "Add button clicked!", Toast.LENGTH_SHORT).show()
        }

        // Get game ID from intent
        val gameId = intent.getStringExtra("gameId")
        if (gameId != null) {
            loadGameDetails(gameId)
        }


    }

    private fun loadGameDetails(gameId: String) {
        db!!.collection("games").document(gameId).get()
            .addOnSuccessListener { documentSnapshot: DocumentSnapshot ->
                if (documentSnapshot.exists()) {
                    val gameName = documentSnapshot.getString("gamename")
                    val gameGenre = documentSnapshot.getString("gamegenre")
                    val gameConsole = documentSnapshot.getString("gameconsole")
                    val gameImage = documentSnapshot.getString("gameimage")

                    gameNameTextView!!.text = gameName
                    gameGenreTextView!!.text = gameGenre
                    gameConsoleTextView!!.text = gameConsole


                    // Load image using Glide or Picasso
                    Glide.with(this).load(gameImage).into(gameImageView!!)
                }
            }
            .addOnFailureListener { e: Exception? ->
                Toast.makeText(
                    this,
                    "Failed to load game details",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}


