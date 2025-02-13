package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var userNameEditText: EditText;
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        //Initialize our views here
        userNameEditText = findViewById(R.id.loginUsernameText)
        passwordEditText = findViewById(R.id.loginPasswordText)
        loginButton = findViewById(R.id.loginLoginButton)
        registerButton = findViewById(R.id.loginRegisterButton)

        //Set logic for login
        loginButton.setOnClickListener{
            val username = userNameEditText.text.toString()
            val password = passwordEditText.text.toString()
            //Implement login logic here
        }

        //Set logic for signing up here
        registerButton.setOnClickListener{
            //Open the register activity(screen)
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

    }
}