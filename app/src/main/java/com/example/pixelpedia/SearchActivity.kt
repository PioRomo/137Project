package com.example.pixelpedia
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
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
    private lateinit var consoleSpinner: Spinner
    private lateinit var leftChevron: ImageView

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
        consoleSpinner = findViewById(R.id.consoleSpinner)
        leftChevron = findViewById(R.id.leftChevron)

        // Set up back navigation
        leftChevron.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        val sortAdapter = ArrayAdapter.createFromResource(
            this, R.array.sort_options, android.R.layout.simple_spinner_dropdown_item
        )
        sortSpinner.adapter = sortAdapter

        // Set up Spinner with predefined genres
        val genreAdapter = ArrayAdapter.createFromResource(
            this, R.array.genre_options, android.R.layout.simple_spinner_dropdown_item
        )
        genreSpinner.adapter = genreAdapter

        // Set up Spinner with predefined genres
        val consoleAdapter = ArrayAdapter.createFromResource(
            this, R.array.console_options, android.R.layout.simple_spinner_dropdown_item
        )
        consoleSpinner.adapter = consoleAdapter

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

        genreSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                filterGames(searchEditText.text.toString())
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {

            }
        })
        consoleSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                filterGames(searchEditText.text.toString()) // Apply filter when console selection changes
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {

            }
        })
        sortSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                filterGames(searchEditText.text.toString()) // Apply filter when console selection changes
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {

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
                    val gameConsole = document.getString("gameconsole") ?: ""
                    val gameGenre = document.getString("gamegenre") ?: ""
                    val gameDescription = document.getString("gamedescription") ?: ""
                    allGames.add(Game(gameId, gameName, gameImage, gameConsole, gameGenre, gameDescription))
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
        val genreFilter = genreSpinner.selectedItem?.toString() ?: "Genre"
        val consoleFilter = consoleSpinner.selectedItem?.toString() ?: "Console"
        val sortOption = sortSpinner.selectedItem?.toString() ?: ""

        filteredGames.clear()
        filteredGames.addAll(allGames.filter {
            it.gamename.contains(query, ignoreCase = true) &&
                    (genreFilter == "Genre" || it.gamegenre == genreFilter) &&
                    (consoleFilter == "Console" || it.gameconsole == consoleFilter)
        })

        if (sortOption == "Alphabetical") {
            filteredGames.sortBy { it.gamename.lowercase() }
        }

        gameAdapter.notifyDataSetChanged()
    }


}
