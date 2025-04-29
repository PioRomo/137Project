package com.example.pixelpedia

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class SearchActivity : BaseActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var gameRecyclerView: RecyclerView
    private lateinit var profileRecyclerView: RecyclerView
    private lateinit var gameAdapter: GameAdapter
    private lateinit var userProfileAdapter: UserProfileAdapter
    private lateinit var sortSpinner: Spinner
    private lateinit var genreSpinner: Spinner
    private lateinit var consoleSpinner: Spinner
    private lateinit var leftChevron: ImageView

    private val allGames = mutableListOf<Game>()
    private val allUsers = mutableListOf<UserProfile>()
    private val filteredGames = mutableListOf<Game>()
    private val filteredUsers = mutableListOf<UserProfile>()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Initialize UI Elements
        searchEditText = findViewById(R.id.searchEditText)
        gameRecyclerView = findViewById(R.id.gameRecyclerView)
        profileRecyclerView = findViewById(R.id.profileRecyclerView)
        sortSpinner = findViewById(R.id.sortSpinner)
        genreSpinner = findViewById(R.id.genreSpinner)
        consoleSpinner = findViewById(R.id.consoleSpinner)
        leftChevron = findViewById(R.id.leftChevron)

        // Set up back navigation
        leftChevron.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Set up spinners
        val sortAdapter = ArrayAdapter.createFromResource(
            this, R.array.sort_options, android.R.layout.simple_spinner_dropdown_item
        )
        sortSpinner.adapter = sortAdapter

        val genreAdapter = ArrayAdapter.createFromResource(
            this, R.array.genre_options, android.R.layout.simple_spinner_dropdown_item
        )
        genreSpinner.adapter = genreAdapter

        val consoleAdapter = ArrayAdapter.createFromResource(
            this, R.array.console_options, android.R.layout.simple_spinner_dropdown_item
        )
        consoleSpinner.adapter = consoleAdapter

        // Set up RecyclerViews and Adapters
        gameAdapter = GameAdapter(filteredGames) { selectedGame ->
            val intent = Intent(this, IndividualGameActivity::class.java)
            intent.putExtra("gameId", selectedGame.gameId)
            startActivity(intent)
        }
        gameRecyclerView.layoutManager = LinearLayoutManager(this)
        gameRecyclerView.adapter = gameAdapter

        userProfileAdapter = UserProfileAdapter(filteredUsers) { selectedUser ->
            val intent = Intent(this, OtherProfilesActivity::class.java)
            intent.putExtra("userId", selectedUser.userId)
            startActivity(intent)
        }
        profileRecyclerView.layoutManager = LinearLayoutManager(this)
        profileRecyclerView.adapter = userProfileAdapter

        // Fetch data
        fetchAllGames()
        fetchAllUsers()

        // Search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterResults(s.toString())
            }
        })

        genreSpinner.setOnItemSelectedListener(createFilterListener())
        consoleSpinner.setOnItemSelectedListener(createFilterListener())
        sortSpinner.setOnItemSelectedListener(createFilterListener())

        // Focus and show keyboard
        searchEditText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun fetchAllGames() {
        firestore.collection("games").get()
            .addOnSuccessListener { documents ->
                allGames.clear()
                for (doc in documents) {
                    val game = doc.toGame()
                    allGames.add(game)
                }
                filterResults(searchEditText.text.toString())
            }
            .addOnFailureListener { e ->
                Log.e("SearchActivity", "Error fetching games", e)
            }
    }

    private fun fetchAllUsers() {
        firestore.collection("users").get()
            .addOnSuccessListener { documents ->
                allUsers.clear()
                for (doc in documents) {
                    val user = doc.toUserProfile()
                    allUsers.add(user)
                }
                filterResults(searchEditText.text.toString())
            }
            .addOnFailureListener { e ->
                Log.e("SearchActivity", "Error fetching users", e)
            }
    }

    private fun filterResults(query: String) {
        val genreFilter = genreSpinner.selectedItem?.toString() ?: "Genre"
        val consoleFilter = consoleSpinner.selectedItem?.toString() ?: "Console"
        val sortOption = sortSpinner.selectedItem?.toString() ?: ""

        // Filter games
        filteredGames.clear()
        filteredGames.addAll(allGames.filter {
            it.gamename.contains(query, ignoreCase = true) &&
                    (genreFilter == "Genre" || it.gamegenre == genreFilter) &&
                    (consoleFilter == "Console" || it.gameconsole == consoleFilter)
        })

        // Filter users
        filteredUsers.clear()
        if (query.isNotBlank()) {
            filteredUsers.addAll(allUsers.filter {
                it.username.contains(query, ignoreCase = true)
            })
        }

        // Sort games
        if (sortOption == "Alphabetical") {
            filteredGames.sortBy { it.gamename.lowercase() }
        }

        gameAdapter.notifyDataSetChanged()
        userProfileAdapter.notifyDataSetChanged()
    }

    private fun createFilterListener(): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterResults(searchEditText.text.toString())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun DocumentSnapshot.toGame(): Game {
        return Game(
            gameId = id,
            gamename = getString("gamename") ?: "Unknown Game",
            gameimage = getString("gameimage") ?: "",
            gameconsole = getString("gameconsole") ?: "Unknown Console",
            gamegenre = getString("gamegenre") ?: "Unknown Genre",
            gamedescription = getString("gamedescription") ?: "No Description Available"
        )
    }

    private fun DocumentSnapshot.toUserProfile(): UserProfile {
        return UserProfile(
            userId = id,
            username = getString("username") ?: "Unknown User",
            profilePicUrl = getString("profilepic") ?: "",
            likes = getLong("likes")?.toInt() ?: 0
        )
    }
}
