package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText;
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    //Firebase Auth instance
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        //Initialize Firebase
        auth = FirebaseAuth.getInstance()

        //Initialize our views here
        emailEditText = findViewById(R.id.loginEmailText)
        passwordEditText = findViewById(R.id.loginPasswordText)
        loginButton = findViewById(R.id.loginLoginButton)
        registerButton = findViewById(R.id.loginRegisterButton)

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
                           startActivity(Intent(this, DummyActivity::class.java))
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
}