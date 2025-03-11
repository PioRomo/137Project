package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
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
        loginButton.setOnClickListener{
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()){
               auth.signInWithEmailAndPassword(email, password).addOnCompleteListener{
                   task ->
                   if(task.isSuccessful()){
                        //Login is valid,  redirect to home page
                       val user = auth.currentUser
                       if (user?.isEmailVerified == true) {
                           Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                           startActivity(Intent(this, MainActivity::class.java))
                           finish()
                       } else {
                           Toast.makeText(this, "Please verify your email before logging in.", Toast.LENGTH_LONG).show()
                           auth.signOut()
                       }
                   }
                   else{
                       //Invalid Login, retry
                       Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                   }
               }

            } else{
                //Password and/or email not entered, try again
                Toast.makeText(this, "Please enter your email and password", Toast .LENGTH_SHORT).show()
            }
        }

        //Set logic for signing up here
        registerButton.setOnClickListener{
            //Open the register activity(screen)
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onStart(){
        super.onStart()
        if(auth.currentUser != null){
            startActivity(Intent(this, DummyActivity::class.java))
            finish()
        }
    }


    // Biometric authentication function
    private fun showBiometricPrompt() {
        // Check if biometric authentication is available
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Authentication succeeded, login the user
                Toast.makeText(applicationContext, "Biometric Authentication Succeeded", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginActivity, DummyActivity::class.java))
                finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // If authentication fails, prompt the user to enter credentials manually
                Toast.makeText(applicationContext, "Biometric Authentication Failed", Toast.LENGTH_SHORT).show()
                // No UI changes needed, just allow them to input credentials manually
                // You can also show a message or update UI if desired
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