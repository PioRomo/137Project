package com.example.pixelpedia

import android.app.AlertDialog
import android.location.Geocoder
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import com.google.android.gms.location.FusedLocationProviderClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.text.InputType
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.location.Address
import java.util.Locale

class SettingsActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var logOutButton: Button
    private lateinit var deleteAccountButton: Button
    private lateinit var changeUsernameButton: Button
    private lateinit var leftChevron: ImageView
    private lateinit var locationButton: Button
    private lateinit var biometricToggle: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        //Initialize Firebase and other vars
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        logOutButton = findViewById(R.id.log_out_button)
        deleteAccountButton = findViewById(R.id.delete_account)
        changeUsernameButton = findViewById(R.id.change_username)
        leftChevron = findViewById(R.id.leftChevron)
        locationButton = findViewById(R.id.change_location)
        biometricToggle = findViewById(R.id.biometric_switch)

        //Biometric Toggle function
        loadBiometricStatus()
        biometricToggle.setOnCheckedChangeListener(){ _, isChecked ->
            val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            if (isChecked) {
                promptForPassword()
            } else {
                disableBiometricLogin()
                loadBiometricStatus()
            }

            editor.putBoolean("biometric_enabled", isChecked) // Save the setting
            editor.apply()
        }

        //Set location function
        locationButton.setOnClickListener {
            showLocationDialog()
        }


        //Change Username Button functionality, redirect to proper page
        changeUsernameButton.setOnClickListener{
            val intent = Intent(this, ChangeUsernameActivity::class.java)
            startActivity(intent)
        }

        //Back to the home page
        leftChevron.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        //Log out functionality
        logOutButton.setOnClickListener{
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))  // Example redirect to login page
            finish()
        }

        //Delete user functionality
        deleteAccountButton.setOnClickListener {
            confirmDeleteAccount()
        }

        // Find BottomNavigationView
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Handle navigation item clicks
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
    private fun loadBiometricStatus() {
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val biometricEnabled = document.getBoolean("biometric_enabled") ?: false
                    biometricToggle.setOnCheckedChangeListener(null)
                    biometricToggle.isChecked = biometricEnabled
                    biometricToggle.setOnCheckedChangeListener { _, isChecked ->
                        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        if (isChecked) {
                            promptForPassword()
                        } else {
                            disableBiometricLogin()
                            loadBiometricStatus()
                        }
                        editor.putBoolean("biometric_enabled", isChecked)
                        editor.apply()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("BiometricStatus", "Failed to load biometric status: ${e.message}")
            }
    }

    private fun promptForPassword(){
        val dialog = AlertDialog.Builder(this)
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        dialog.setView(input)
        dialog.setTitle("Enter Password")

        dialog.setPositiveButton("Submit") { _, _ ->
            val enteredPassword = input.text.toString()
            verifyPassword(enteredPassword)
        }

        dialog.setNegativeButton("Cancel") { _, _ ->
            biometricToggle.isChecked = false // Disable the toggle if canceled
        }

        dialog.show()
    }

    private fun verifyPassword(password: String){
        val user = auth.currentUser
        if (user != null && user.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, password)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    enableBiometricLogin(user.uid)
                    loadBiometricStatus()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                    biometricToggle.isChecked = false // Reset switch
                }
        }
    }

    private fun enableBiometricLogin(userId: String) {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        db.collection("users").document(userId)
            .update(mapOf(
                "biometric_enabled" to true,
                "biometric_device_id" to deviceId
            ))
            .addOnSuccessListener {
                val sharedPreferences = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putBoolean("biometric_login_enabled", true)  // Store the biometric enabled flag
                editor.apply()
                Toast.makeText(this, "Biometric login enabled!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to enable biometric login", Toast.LENGTH_SHORT).show()
                biometricToggle.isChecked = false
            }
    }

    private fun disableBiometricLogin() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .update(mapOf(
                "biometric_enabled" to false,   // Disable biometric login
                "biometric_device_id" to ""     // Optionally reset the device ID if needed
            ))
            .addOnSuccessListener {
                val sharedPreferences = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putBoolean("biometric_login_enabled", false)
                editor.apply()

                Toast.makeText(this, "Biometric login disabled!", Toast.LENGTH_SHORT).show()
                biometricToggle.isChecked = false  // Update toggle to reflect the disabled state
                loadBiometricStatus()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to disable biometric login", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDeleteAccount() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "No user is logged in", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to permanently delete your account?")
            .setPositiveButton("Yes") { _, _ ->
                showPasswordDialog(user.email ?: "")
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showPasswordDialog(email: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_password, null)
        val passwordInput = dialogView.findViewById<EditText>(R.id.editTextPassword)

        AlertDialog.Builder(this)
            .setTitle("Reauthenticate")
            .setMessage("Enter your password to continue:")
            .setView(dialogView)
            .setPositiveButton("Confirm") { _, _ ->
                val password = passwordInput.text.toString().trim()
                if (password.isEmpty()) {
                    Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    reauthenticateAndDelete(email, password)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reauthenticateAndDelete(email: String, password: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "No user found", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to delete account: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Reauthentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showLocationDialog() {
        // Create an AlertDialog with two options
        val options = arrayOf("Set current location", "Set location manually")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose Location Option")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> fetchCurrentLocation() // Option 1: Get current location
                1 -> showManualLocationDialog() // Option 2: Set manually
            }
        }
        builder.show()
    }

    private fun fetchCurrentLocation() {
        // Check for location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            // Get current location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    getCityAndCountry(lat, lon)
                } else {
                    Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getCityAndCountry(lat: Double, lon: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addressList: List<Address> = geocoder.getFromLocation(lat, lon, 1) as List<Address>
            if (addressList.isNotEmpty()) {
                val address = addressList[0]
                val city = address.locality ?: "Unknown city"
                val country = address.countryName ?: "Unknown country"
                val location = "$city, $country"
                // Save this to Firebase
                saveLocationToFirebase(location)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error fetching address", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLocationToFirebase(location: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .update("location", location)
            .addOnSuccessListener {
                Toast.makeText(this, "Location saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showManualLocationDialog() {
        // Show dialog for manual input
        val input = EditText(this)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Location")
        builder.setView(input)
        builder.setPositiveButton("OK") { dialog, _ ->
            val location = input.text.toString().trim()
            if (location.isNotEmpty()) {
                storeLocationInFirebase(location)
                Toast.makeText(this, "Location set to: $location", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location cannot be empty", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun storeLocationInFirebase(location: String) {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid

            // Store location in Firestore under user's document
            db.collection("users").document(userId)
                .update("location", location)
                .addOnSuccessListener {
                    Toast.makeText(this, "Location updated successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, fetch current location
            fetchCurrentLocation()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

}
