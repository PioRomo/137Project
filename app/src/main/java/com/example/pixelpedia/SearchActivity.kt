package com.example.pixelpedia

import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class SearchActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var gameRecyclerView: RecyclerView
    private lateinit var gameAdapter: GameAdapter
    private lateinit var sortSpinner: Spinner
    private lateinit var genreSpinner: Spinner

    private var allGames: MutableList<Game> = mutableListOf() // Full game list from Firestore
    private var filteredGames: MutableList<Game> = mutableListOf() // Search results

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Initialize UI Elements
        searchEditText = findViewById(R.id.searchEditText)
        gameRecyclerView = findViewById(R.id.gameRecyclerView)
        sortSpinner = findViewById(R.id.sortSpinner)
        genreSpinner = findViewById(R.id.genreSpinner)

        // Set up Spinner with predefined genres
        val genreAdapter = ArrayAdapter.createFromResource(
            this, R.array.genre_options, android.R.layout.simple_spinner_dropdown_item
        )
        genreSpinner.adapter = genreAdapter

        // Set up RecyclerView & Adapter
        gameAdapter = GameAdapter(filteredGames) { selectedGame ->
            val intent = Intent(this, IndividualGameActivity::class.java)
            intent.putExtra("gameId", selectedGame.gameId)
            startActivity(intent)
        }
        gameRecyclerView.layoutManager = LinearLayoutManager(this)
        gameRecyclerView.adapter = gameAdapter

        // Fetch games from Firestore
        fetchAllGames()

        // Search functionality - filter list on text change
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterGames(s.toString())
            }
        })

        // Focus and show keyboard
        searchEditText.requestFocus() // Focus on the EditText
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT) // Show the keyboard
    }

    private fun fetchAllGames() {
        val db = FirebaseFirestore.getInstance()

        db.collection("games").get()
            .addOnSuccessListener { result ->
                allGames.clear()
                for (document in result) {
                    val gameId = document.id
                    val gameName = document.getString("gamename") ?: ""
                    val gameImage = document.getString("gameimage") ?: ""
                    val gameGenre = document.getString("gamegenre") ?: ""

                    allGames.add(Game(gameId, gameName, gameImage, gameGenre))
                }

                // Ensure genreSpinner is ready before filtering
                genreSpinner.post {
                    filterGames(searchEditText.text.toString())
                }
            }
            .addOnFailureListener { e ->
                Log.e("SearchActivity", "Error fetching games", e)
            }
    }

    private fun filterGames(query: String) {
        val genreFilter = genreSpinner.selectedItem?.toString() ?: "Any" // Prevent crash

        filteredGames.clear()
        filteredGames.addAll(allGames.filter {
            it.gamename.contains(query, ignoreCase = true) &&
                    (genreFilter == "Any" || it.gamegenre == genreFilter)
        })
        gameAdapter.notifyDataSetChanged()
    }
}
