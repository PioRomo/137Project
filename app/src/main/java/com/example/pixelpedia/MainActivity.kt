package com.example.pixelpedia

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.messaging

const val CHANNELZ_ID = "channel_id"
const val CHANNEL_ID4 = "channel_id4"
private const val LIKES_CHANNEL_ID = "likes_channel"
private const val LIKES_NOTIFICATION_ID = 2

class MainActivity : BaseActivity() {
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

    val permissionArr = arrayOf(Manifest.permission.POST_NOTIFICATIONS)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result: Boolean ->
        if (result) {
            showNotification()
            checkForNewLikes()
        } else {
            Toast.makeText(this@MainActivity, "Permission Not Granted", Toast.LENGTH_SHORT).show()
        }
    }

    lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            showNotification()
            checkForNewLikes()
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
        userGreeting = findViewById(R.id.userGreeting)

        val user = auth.currentUser
        if (user != null) {
            val username = user.displayName ?: "User"
            userGreeting.text = "Hello $username!"
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
        }

        gameAdapter = GameAdapter(gameList) { selectedGame ->
            val intent = Intent(this, IndividualGameActivity::class.java)
            intent.putExtra("gameId", selectedGame.gameId)
            startActivity(intent)
        }
        gamerecyclerView.layoutManager = LinearLayoutManager(this)
        gamerecyclerView.adapter = gameAdapter
        loadGames()

        userProfileAdapter = UserProfileAdapter(userList) { selectedUser ->
            val currentUser = auth.currentUser
            val intent = if (selectedUser.userId == currentUser?.uid) {
                Intent(this, ProfileActivity::class.java)
            } else {
                Intent(this, OtherProfilesActivity::class.java).apply {
                    putExtra("userId", selectedUser.userId)
                }
            }
            startActivity(intent)
        }
        profilerecyclerView.layoutManager = LinearLayoutManager(this)
        profilerecyclerView.adapter = userProfileAdapter
        loadProfiles()

        settingsIcon.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
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
                    Log.d("UserProfile", "Fetched user: ${user.username}")
                }
                Log.d("UserProfile", "Total users fetched: ${userList.size}")
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
            likes = (getLong("likes") ?: 0).toInt()
        )
    }

    @SuppressLint("MissingPermission")
    private fun showNotification() {
        val sharedPref = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val hasShownNotification = sharedPref.getBoolean("hasShownWelcomeNotification", false)
        if (hasShownNotification) return

        val builder = NotificationCompat.Builder(this, CHANNEL_ID4)
            .setSmallIcon(R.drawable.red_minus_icon)
            .setContentTitle("Welcome to PixelPedia!")
            .setContentText("Please take a look around and add all your favorite games and customize your pfp!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        with(NotificationManagerCompat.from(this)) {
            notify(1, builder.build())
        }

        sharedPref.edit().putBoolean("hasShownWelcomeNotification", true).apply()
    }

    @SuppressLint("MissingPermission")
    private fun showLikeNotification() {
        val builder = NotificationCompat.Builder(this, LIKES_CHANNEL_ID)
            .setSmallIcon(R.drawable.red_minus_icon)
            .setContentTitle("You've got a new like!")
            .setContentText("Someone liked your profile. Check it out!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(LIKES_NOTIFICATION_ID, builder.build())
        }
    }

    private fun checkForNewLikes() {
        val currentUserId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                val currentLikes = document.getLong("likes")?.toInt() ?: 0
                val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
                val previousLikes = prefs.getInt("lastKnownLikes", 0)

                if (currentLikes > previousLikes) {
                    showLikeNotification()
                }

                // Subscribe or unsubscribe from "popular" topic
                if (currentLikes > 1) {
                    Firebase.messaging.subscribeToTopic("popular")
                        .addOnCompleteListener { task ->
                            val msg = if (task.isSuccessful) "Subscribed to popular" else "Subscribe failed"
                            // alert hidden below
                            // Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Firebase.messaging.unsubscribeFromTopic("popular")
                        .addOnCompleteListener { task ->
                            val msg = if (task.isSuccessful) "Unsubscribed from popular" else "Unsubscribe failed"
                            // alert hidden
                            // Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        }
                }

                prefs.edit().putInt("lastKnownLikes", currentLikes).apply()
            }
            .addOnFailureListener {
                Log.e("MainActivity", "Failed to fetch user like count.")
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val generalChannel = NotificationChannel(
                CHANNELZ_ID, "General Notifications", NotificationManager.IMPORTANCE_HIGH
            )

            val welcomeChannel = NotificationChannel(
                CHANNEL_ID4, "Welcome Notifications", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "One-time welcome notification"
            }

            val likesChannel = NotificationChannel(
                LIKES_CHANNEL_ID, "Likes Notification", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when someone likes your profile"
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(generalChannel)
            notificationManager.createNotificationChannel(welcomeChannel)
            notificationManager.createNotificationChannel(likesChannel)
        }
    }
}
