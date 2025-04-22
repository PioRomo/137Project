package com.example.pixelpedia

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.Manifest
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat


class OtherProfilesActivity : AppCompatActivity() {

    private lateinit var profilePicture: ImageView
    private lateinit var usernameText: TextView
    private lateinit var locationText: TextView
    private lateinit var likesNumberText: TextView
    private lateinit var libraryRecyclerView: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var leftChevron: ImageView

    private val db = FirebaseFirestore.getInstance()
    private lateinit var gameAdapter: GameAdapter
    private lateinit var userId: String
    private val gameList = mutableListOf<Pair<String, String>>()

    private lateinit var likeCountTextView: TextView
    private lateinit var likeButton: ImageView
    private var currentLikes: Int = 0

    // permission launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){
            result:Boolean ->
        if(result) {
            sendNotificationToUser()
        }else{
            Toast.makeText(
                this,
                "Notification Permission Not Granted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        private const val CHANNEL_ID = "like_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_profiles)
        createNotificationChannel()

        // UI references
        profilePicture = findViewById(R.id.profilePicture)
        usernameText = findViewById(R.id.usernameText)
        locationText = findViewById(R.id.locationText)
        likesNumberText = findViewById(R.id.likesNumberText)
        libraryRecyclerView = findViewById(R.id.libraryRecyclerView)
        bottomNav = findViewById(R.id.bottomNav)
        leftChevron = findViewById(R.id.leftChevron)
        likeCountTextView = findViewById(R.id.likesNumberText)
        likeButton = findViewById(R.id.likesImage)

        // Get passed user ID or username
        userId = intent.getStringExtra("userId") ?: return

        // Fetch profile data
        fetchUserData()

        // Fetch User Likes
        fetchUserLikes()

        //Display User Library
        libraryRecyclerView = findViewById(R.id.libraryRecyclerView)
        libraryRecyclerView.layoutManager = GridLayoutManager(this, 3)

        gameAdapter = GameAdapter()
        libraryRecyclerView.adapter = gameAdapter

        // Fetch User's Library
        fetchUserGames()

        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentUserRef = db.collection("users").document(currentUserId)
        val profileRef = db.collection("users").document(userId) // userId = the profile being viewed

        // On load: check if the profile is already liked
        currentUserRef.get().addOnSuccessListener { doc ->
            val likedProfiles = doc.get("likedprofiles") as? List<*> ?: emptyList<String>()
            val isLiked = likedProfiles.contains(userId)

            // Set correct heart icon
            likeButton.setImageResource(if (isLiked) R.drawable.pinkheart else R.drawable.heart)
        }

        // Set up the click listener
        likeButton.setOnClickListener {
            currentUserRef.get().addOnSuccessListener { doc ->
                val likedProfiles = doc.get("likedprofiles") as? List<*> ?: emptyList<String>()
                val isLiked = likedProfiles.contains(userId)

                if (isLiked) {
                    // If already liked, remove like
                    profileRef.update("likes", FieldValue.increment(-1))
                    currentUserRef.update("likedprofiles", FieldValue.arrayRemove(userId))
                    currentLikes-= 1
                    likeCountTextView.text = currentLikes.toString()
                    likeButton.setImageResource(R.drawable.heart)

                } else {
                    profileRef.update("likes", FieldValue.increment(1))
                    currentUserRef.update("likedprofiles", FieldValue.arrayUnion(userId))
                    currentLikes += 1
                    likeCountTextView.text = currentLikes.toString()
                    likeButton.setImageResource(R.drawable.pinkheart)

                    // Check notification permission and send if granted
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            sendNotificationToUser()
                        }
                    } else {
                        sendNotificationToUser()
                    }

                    // checking if it worked
                    Toast.makeText(this, "Nice Job, You liked this profile!", Toast.LENGTH_SHORT).show()
                }
            }
        }


        // Back button logic
        leftChevron.setOnClickListener {
            finish()
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
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun fetchUserData(){
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username")
                    val location = document.getString("location")
                    val likes = document.getLong("likes")?.toInt() ?: 0
                    val profileImageUrl = document.getString("profilepic")

                    usernameText.text = username
                    locationText.text = location
                    likesNumberText.text = likes.toString()

                    Glide.with(this)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.placeholder_game_image)
                        .into(profilePicture)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUserGames() {
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

    inner class GameAdapter : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

        inner class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val gameImageView: ImageView = itemView.findViewById(R.id.gameImageView)
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


            holder.gameImageView.setOnClickListener {
                val intent = Intent(holder.itemView.context, IndividualGameActivity::class.java)
                intent.putExtra("gameId", gameId) // Pass gameId to next screen
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = gameList.size
    }

    // Function to fetch the current likes count from Firestore
    private fun fetchUserLikes() {
        if (userId != null) {
            val userRef = FirebaseFirestore.getInstance().collection("users").document(userId!!)
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Get the current likes count from Firestore
                        currentLikes = document.getLong("likes")?.toInt() ?: 0
                        likeCountTextView.text = currentLikes.toString()  // Display the fetched likes count
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("OtherProfileActivity", "Error fetching likes", e)
                }
        }
    }

    // Function to update the likes count in Firestore
    private fun updateLikes(newLikes: Int) {
        if (userId != null) {
            val userRef = FirebaseFirestore.getInstance().collection("users").document(userId!!)
            userRef.update("likes", newLikes)
                .addOnSuccessListener {
                    Log.d("OtherProfileActivity", "Likes updated successfully")
                    currentLikes = newLikes
                }
                .addOnFailureListener { e ->
                    Log.e("OtherProfileActivity", "Error updating likes", e)
                }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Likes Notification Channel"
            val descriptionText = "Channel for like notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendNotificationToUser() {
        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.red_minus_icon) // needs to be updated
            .setContentTitle("Someone liked your profile!")
            .setContentText("Check out your profile to see who liked you.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            // defined id is 1 here
            notify(NOTIFICATION_ID, builder.build())
        }
    }
}

