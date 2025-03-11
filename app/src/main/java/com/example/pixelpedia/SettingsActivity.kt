package com.example.pixelpedia

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var logOutButton: Button
    private lateinit var deleteAccountButton: Button
    private lateinit var changeUsernameButton: Button
    private lateinit var leftChevron: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        //Initialize Firebase and other vars
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        logOutButton = findViewById(R.id.log_out_button)
        deleteAccountButton = findViewById(R.id.delete_account)
        changeUsernameButton = findViewById(R.id.change_username)
        leftChevron = findViewById(R.id.leftChevron)

        //Redirect to Change username form
        changeUsernameButton.setOnClickListener{
            val intent = Intent(this, ChangeUsernameActivity::class.java)
            startActivity(intent)
        }

        //Back to the home page
        leftChevron.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        /*
        resetPasswordButton.setOnClickListener {
            // Get the current logged-in user's email
            val userEmail = auth.currentUser?.email

            if (userEmail != null) {
                sendPasswordResetEmail(userEmail)
            } else {
                Toast.makeText(this, "No user is logged in.", Toast.LENGTH_SHORT).show()
            }
        }*/

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
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.nav_library -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
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

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Successfully sent the reset email
                    Toast.makeText(this, "Reset email sent to $email. Please check your inbox.", Toast.LENGTH_SHORT).show()
                } else {
                    // Failed to send the reset email
                    Toast.makeText(this, "Failed to send reset email. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
    }

}
