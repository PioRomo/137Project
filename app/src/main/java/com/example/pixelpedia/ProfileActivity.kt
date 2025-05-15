    package com.example.pixelpedia

    import android.annotation.SuppressLint
    import android.content.Context
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.os.Build
    import android.os.Bundle
    import android.util.Log
    import android.Manifest
    import android.app.NotificationChannel
    import android.app.NotificationManager
    import android.widget.Button
    import android.widget.ImageView
    import android.widget.TextView
    import android.widget.Toast
    import androidx.activity.enableEdgeToEdge
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.appcompat.app.AlertDialog
    import androidx.core.app.ActivityCompat
    import androidx.core.app.NotificationCompat
    import androidx.core.app.NotificationManagerCompat
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.bumptech.glide.Glide
    import com.bumptech.glide.load.resource.bitmap.CircleCrop
    import com.bumptech.glide.request.RequestOptions
    import com.google.android.gms.tasks.Tasks
    import com.google.android.material.bottomnavigation.BottomNavigationView
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.DocumentSnapshot
    import com.google.firebase.firestore.FirebaseFirestore
    import de.hdodenhof.circleimageview.CircleImageView


    class ProfileActivity : BaseActivity() {

        private lateinit var profileImageView: CircleImageView
        private lateinit var uploadProfilePicButton: Button
        private lateinit var userName: TextView
        private lateinit var userLocation: TextView
        private lateinit var profileLikes: TextView
        private lateinit var viewLikes: Button
        private lateinit var infoIcon: ImageView
        private lateinit var recycler: RecyclerView
        private lateinit var settingIcon:  ImageView
        private lateinit var logo: ImageView
        //  private lateinit var leftChevron: ImageView         back button



        private val db = FirebaseFirestore.getInstance()
        private val userId = FirebaseAuth.getInstance().currentUser?.uid

        companion object {
            private const val LIKES_CHANNEL_ID = "likes_channel"
            private const val LIKES_NOTIFICATION_ID = 2
        }

        private val notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                showLikeNotification()
            } else {
                Toast.makeText(this, "Notification Permission Not Granted", Toast.LENGTH_SHORT).show()
            }
        }


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_profile)
            enableEdgeToEdge()

            profileImageView = findViewById<CircleImageView>(R.id.profile_image)
            uploadProfilePicButton = findViewById(R.id.uploadProfilePictureButton)
            userName = findViewById(R.id.profile_name)
            userLocation = findViewById(R.id.profile_location)
            profileLikes = findViewById(R.id.likesNumberText)
            viewLikes = findViewById(R.id.viewLikesButton)
            infoIcon = findViewById(R.id.infoIcon)
            recycler = findViewById(R.id.recycler_likes)
            settingIcon =  findViewById(R.id.setting)
            logo = findViewById(R.id.logo)
           // leftChevron = findViewById(R.id.leftChevron)      back button

            createLikesNotificationChannel()
            loadUserProfile()

            fetchUsersWhoLikedMe { likedUsers ->
                val adapter = UserProfileAdapter(likedUsers) { selectedUser ->
                    val intent = Intent(this, OtherProfilesActivity::class.java).apply {
                        putExtra("userId", selectedUser.userId)
                    }
                    startActivity(intent)
                }
                recycler.adapter = adapter
                recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            }



            uploadProfilePicButton.setOnClickListener {
                showInputDialog()
            }

            viewLikes.setOnClickListener{
                val intent = Intent(this, LikesActivity::class.java)
                startActivity(intent)
            }

            infoIcon.setOnClickListener {
                showUploadInformation()
            }

            logo.setOnClickListener{
                startActivity(Intent(this, MainActivity::class.java))
            }

            settingIcon.setOnClickListener {
                val intent  = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

           // leftChevron.setOnClickListener {                              back button stuff
           //     val intent = Intent(this, MainActivity::class.java)
           //     startActivity(intent)
           // }

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

        private fun showInputDialog() {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter IMGUR Image URL")

            // Set up the input field
            val input = android.widget.EditText(this)
            input.hint = "Enter IMGUR image URL"
            builder.setView(input)

            // Set up the buttons
            builder.setPositiveButton("OK") { _, _ ->
                val imageUrl = input.text.toString().trim()

                if (imageUrl.isNotEmpty()) {
                    saveImageUrlToFirestore(imageUrl)
                    Glide.with(this)
                        .load(imageUrl)
                        .into(profileImageView)
                } else {
                    Toast.makeText(this, "Please enter a valid image URL", Toast.LENGTH_SHORT).show()
                }
            }
            builder.setNegativeButton("Cancel", null)

            // Show the dialog
            builder.show()
        }

        private fun saveImageUrlToFirestore(imageUrl: String) {
            userId?.let {
                db.collection("users").document(it)
                    .update("profilepic", imageUrl)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile image updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update profile image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        private fun loadUserProfile() {
            userId?.let {
                db.collection("users").document(it).get()
                    .addOnSuccessListener { doc ->
                        val username = doc.getString("username") ?: "Sample User"
                        val location = doc.getString("location") ?: "Bikini Bottom"
                        val numLikes = doc.getLong("likes") ?: 0L
                        val currentLikes = doc.getLong("likes")?.toInt() ?: 0
                        userName.text = username
                        userLocation.text = location
                        profileLikes.text = "$numLikes"

                        checkForNewLikes(currentLikes) // to trigger new notification

                        // Load profile image
                        val imageUrl = doc.getString("profilepic")
                        val placeholderRes = R.drawable.placeholder_game_image // your default image in res/drawable

                        Glide.with(this)
                            .load(imageUrl)
                            .apply(RequestOptions()
                                .placeholder(placeholderRes)
                                .error(placeholderRes)
                                .transform(CircleCrop())) // Circle crop transformation
                            .into(profileImageView)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        private fun showUploadInformation(){
            AlertDialog.Builder(this)
                .setTitle("How Do I Upload a Profile Picture?")
                .setMessage("Our app only takes valid Imgur URLS: \n" +
                        "1) You can navigate to your image's page on Igmur (by browsing or uploading) \n" +
                        "2) Click and hold the image, then select the option to open the image\n" +
                        "3) Copy the link. It should be formatted like 'i.imgur.com/blahblahblah'\n"+
                        "4) Paste the link and ta-da! You have a new profile image!"
                )
                .setPositiveButton("OK", null)
                .show()
        }

        private fun createLikesNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Likes Notification"
                val descriptionText = "Notifies when your profile receives a new like"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(LIKES_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        private fun checkForNewLikes(currentLikes: Int) {
            val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
            val previousLikes = prefs.getInt("lastKnownLikes", 0)

            if (currentLikes > previousLikes) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        showLikeNotification()
                    }
                } else {
                    showLikeNotification()
                }

                prefs.edit().putInt("lastKnownLikes", currentLikes).apply()
            } else if (currentLikes != previousLikes) {
                prefs.edit().putInt("lastKnownLikes", currentLikes).apply()
            }
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



        private fun fetchUsersWhoLikedMe(onComplete: (List<UserProfile>) -> Unit) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener { doc ->
                    val likedByIds = doc.get("likedBy") as? List<*> ?: emptyList<String>()

                    if (likedByIds.isEmpty()) {
                        onComplete(emptyList())
                        return@addOnSuccessListener
                    }

                    val likedUsers = mutableListOf<UserProfile>()
                    val tasks = likedByIds.map { id ->
                        db.collection("users").document(id as String).get()
                    }

                    Tasks.whenAllSuccess<DocumentSnapshot>(tasks)
                        .addOnSuccessListener { snapshots ->
                            for (userDoc in snapshots) {
                                val uid = userDoc.id
                                val likes = userDoc.getLong("likes") ?: 0
                                val username = userDoc.getString("username") ?: continue
                                val profilePicUrl = userDoc.getString("profilepic") ?: ""
                                likedUsers.add(UserProfile(uid, username, profilePicUrl, likes.toInt()))
                            }
                            onComplete(likedUsers)
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                            onComplete(emptyList())
                        }
                }
        }

    }
