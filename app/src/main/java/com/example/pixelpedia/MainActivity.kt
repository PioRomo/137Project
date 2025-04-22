package com.example.pixelpedia

import android.annotation.SuppressLint
import android.content.Intent
import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


class MainActivity : AppCompatActivity() {
    private lateinit var settingsIcon: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var gamerecyclerView: RecyclerView
    private lateinit var profilerecyclerView: RecyclerView
    private lateinit var gameAdapter: GameAdapter
    private lateinit var userProfileAdapter: UserProfileAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userGreeting: TextView
    private val gameList = mutableListOf<Game>()
    private val userList = mutableListOf<UserProfile>()
    val CHANNEL_ID = "ch1"

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){
        result:Boolean ->
            if(result) {
                showNotification()
            }else{
                Toast.makeText(
                    this@MainActivity,
                    "Permission Not Granted",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()

        if(ActivityCompat.checkSelfPermission(
            applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
            ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }else {
            showNotification()
        }

        val searchBar: EditText = findViewById(R.id.searchBar)

        searchBar.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
            }
        }



        settingsIcon = findViewById(R.id.setting)
        gamerecyclerView = findViewById(R.id.gameRecyclerView)
        profilerecyclerView = findViewById(R.id.profilesRecyclerView)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        userGreeting = findViewById(R.id.userGreeting)

        val user = auth.currentUser
        if (user != null) {
            val username = user.displayName ?: "User"
            userGreeting.text = "Hello $username!"
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
        }

        // Game section setup
        gameAdapter = GameAdapter(gameList) { selectedGame ->
            val intent = Intent(this, IndividualGameActivity::class.java)
            intent.putExtra("gameId", selectedGame.gameId)
            startActivity(intent)
        }
        gamerecyclerView.layoutManager = LinearLayoutManager(this)
        gamerecyclerView.adapter = gameAdapter

        loadGames()

        // Profile section setup
        /*userProfileAdapter = UserProfileAdapter(userList) {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }*/
        userProfileAdapter = UserProfileAdapter(userList) { selectedUser ->
            val currentUser = auth.currentUser
            val intent = if (selectedUser.userId == currentUser?.uid) {
                // If it's the current user's profile, go to ProfileActivity
                Intent(this, ProfileActivity::class.java)
            } else {
                // If it's another user's profile, go to OtherProfileActivity
                Intent(this, OtherProfilesActivity::class.java).apply {
                    putExtra("userId", selectedUser.userId)
                }
            }
            startActivity(intent)
        }

        profilerecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        profilerecyclerView.adapter = userProfileAdapter

        loadProfiles()

        settingsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

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

    override fun onResume() {
        super.onResume()
        val searchBar: EditText = findViewById(R.id.searchBar)
        searchBar.clearFocus()
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

    private fun loadProfiles() {
        firestore.collection("users").limit(10).get()
            .addOnSuccessListener { documents ->
                userList.clear()
                for (document in documents) {
                    val user = document.toUserProfile()
                    userList.add(user)
                    Log.d("UserProfile", "Fetched user: ${user.username}")  // Log each user
                }
                Log.d("UserProfile", "Total users fetched: ${userList.size}")  // Log total count
                userProfileAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading profiles: ${e.message}", Toast.LENGTH_SHORT).show()
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



    @SuppressLint("MissingPermission")
    private fun showNotification() {
        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.red_minus_icon)
            .setContentTitle("User Liked Your Profile")
            .setContentText("A fellow user enjoys your profile!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            // defined id is 1
            notify(1, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Likes Notification"
            val descriptionText = "Notifies when someone likes your profile"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // register channel with our system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
