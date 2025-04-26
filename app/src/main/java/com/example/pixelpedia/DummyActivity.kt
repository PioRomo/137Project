package com.example.pixelpedia

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class DummyActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dummy)
    }

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || !user.isEmailVerified) {
            // Redirect to login page if user is not authenticated or email is not verified
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
