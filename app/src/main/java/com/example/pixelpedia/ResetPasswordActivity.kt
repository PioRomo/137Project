package com.example.pixelpedia

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.pixelpedia.databinding.ActivityChangeUsernameBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class ResetPasswordActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var resetPassword: EditText
    private lateinit var buttonSubmit: Button
    private lateinit var leftChevron: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        resetPassword = findViewById(R.id.resetPasswordText)
        buttonSubmit = findViewById(R.id.resetPasswordSubmit)
        leftChevron = findViewById(R.id.leftChevron)

        leftChevron.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        buttonSubmit.setOnClickListener {
            val email = resetPassword.text.toString().trim()

            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Reset link sent! Check your email.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Redirect back to login after sending the reset email
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Error: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }





    }


}
