package com.example.pixelpedia

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RegisterActivity : AppCompatActivity() {

    private lateinit var userNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var backToLoginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        userNameEditText = findViewById(R.id.registerUsernameText)
        emailEditText = findViewById(R.id.registerEmailText)
        passwordEditText = findViewById(R.id.registerPasswordText)
        registerButton = findViewById(R.id.registerRegisterButton)
        backToLoginButton = findViewById(R.id.registerBackToLogin)

        registerButton.setOnClickListener{
            val username = userNameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            //Implement firebase logic
        }

        backToLoginButton.setOnClickListener{
            finish()
        }
    }
}