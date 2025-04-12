package com.example.pixelpedia

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.text.InputType
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var userNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var togglePasswordVisibility: ImageView
    private lateinit var registerButton: Button
    private lateinit var backToLoginButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        userNameEditText = findViewById(R.id.registerUsernameText)
        emailEditText = findViewById(R.id.registerEmailText)
        passwordEditText = findViewById(R.id.registerPasswordText)
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility)
        registerButton = findViewById(R.id.registerRegisterButton)
        backToLoginButton = findViewById(R.id.registerBackToLogin)

        //Set font var to fix password text not displaying on load
        val poppinsFont = ResourcesCompat.getFont(this, R.font.poppins_bold)


        //Toggling password visibility
        // Set password input type to hidden by default
        passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordEditText.typeface = poppinsFont

        togglePasswordVisibility.setOnClickListener {
            togglePasswordVisibility(it)
        }

        registerButton.setOnClickListener {
            val username = userNameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (!email.endsWith("@gmail.com")) {
                Toast.makeText(this, "Only Google emails (@gmail.com) are allowed.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!isValidPassword(password)) {
                Toast.makeText(this, "Password must be at least 8 characters, include 1 uppercase, 1 number, and 1 special character.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser

                        // Send verification email
                        user?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                            if (emailTask.isSuccessful) {
                                val userId = user.uid
                                val userData = hashMapOf("username" to username, "email" to email, "biometric_enabled" to false)

                                FirebaseFirestore.getInstance().collection("users").document(userId)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "User registered! Verify your email before logging in.", Toast.LENGTH_SHORT).show()
                                        auth.signOut() // Log out the user until they verify
                                        startActivity(Intent(this, LoginActivity::class.java))
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Failed to save user: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Fill in all fields!", Toast.LENGTH_LONG).show()
            }
        }


        backToLoginButton.setOnClickListener{
            finish()
        }
    }

    private fun isValidPassword(password:String):Boolean {
        val passwordPattern = Regex("^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#\$%^&+=!])(?=\\S+\$).{8,}\$")
        return password.matches(passwordPattern)
    }

    private fun togglePasswordVisibility(view : View){
        val poppinsFont = ResourcesCompat.getFont(this, R.font.poppins_bold)
        // Check if the current input type is visible password
        if (passwordEditText.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            // Set input type to hidden password
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            togglePasswordVisibility.setImageResource(R.drawable.ic_visibility_off) // Closed eye icon
        } else {
            // Set input type to visible password
            passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            togglePasswordVisibility.setImageResource(R.drawable.ic_visibility) // Open eye icon
        }

        // Reapply the text so the cursor stays at the end and  reset font
        passwordEditText.typeface = poppinsFont
        passwordEditText.setSelection(passwordEditText.text.length)
    }
}