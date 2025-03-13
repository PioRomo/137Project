package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText;
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button

    //Biometric button instance
    private lateinit var biometricLoginButton: Button

    //Firebase Auth instance
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        //Initialize Firebase
        auth = FirebaseAuth.getInstance()
        auth.signOut()

        //Initialize our views here
        emailEditText = findViewById(R.id.changeUsernameText)
        passwordEditText = findViewById(R.id.loginPasswordText)
        loginButton = findViewById(R.id.changeUsernameSubmit)
        registerButton = findViewById(R.id.loginRegisterButton)

        //Initialize Biometric button
        biometricLoginButton = findViewById(R.id.biometricLoginButton)
        // Biometric login button logic
        biometricLoginButton.setOnClickListener {
            showBiometricPrompt()
        }

        //Handling Login logic
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful()) {
                        //Login is valid,  redirect to home page
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user?.isEmailVerified == true) {
                            val sharedPreferences = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                            sharedPreferences.edit().putString("user_email", user.email).apply()



                            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Please verify your email before logging in.",
                                Toast.LENGTH_LONG
                            ).show()
                            auth.signOut()
                        }
                    } else {
                        //Invalid Login, retry
                        Toast.makeText(
                            this,
                            "Login Failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } else {
                //Password and/or email not entered, try again
                Toast.makeText(this, "Please enter your email and password", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        //Set logic for signing up here
        registerButton.setOnClickListener {
            //Open the register activity(screen)
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onStart() {
        super.onStart()
        // Check if a user is already logged in
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // User is already logged in, navigate to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            // If user is not logged in, show biometric prompt or login page
            showBiometricPrompt()
        }
    }


    // Biometric authentication function
    private fun showBiometricPrompt() {
        // Check if biometric authentication is available
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)

                // Authentication succeeded, now retrieve the stored email
                val sharedPreferences = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                val userEmail = sharedPreferences.getString("user_email", null)

                if (userEmail != null) {
                    // Now sign in with the stored email
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(userEmail, "Bayarea5511884$")
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Login is successful, proceed to the main screen
                                Toast.makeText(applicationContext, "Biometric Authentication Succeeded", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            } else {
                                // If the login fails
                                Toast.makeText(applicationContext, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    // If no email is found, show an error
                    Toast.makeText(applicationContext, "No user found for biometric login", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // If authentication fails, prompt the user to enter credentials manually
                Toast.makeText(applicationContext, "Biometric Authentication Failed", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(applicationContext, "Biometric Authentication Error: $errString", Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login")
            .setSubtitle("Log in using your fingerprint")
            .setNegativeButtonText("Use Password")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }



}