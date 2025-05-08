package com.example.pixelpedia

import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    private lateinit var connectivityReceiver: BroadcastReceiver
    private var hasShownLargeToast = false
    private var isDisconnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupConnectivityReceiver()
    }

    private fun setupConnectivityReceiver() {
        connectivityReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val connected = isConnected()
                if (!connected && !isDisconnected) {
                    if (!hasShownLargeToast) {
                        showLargeCenteredToast("No Internet Connection")
                        hasShownLargeToast = true
                    } else {
                        showSmallBottomToast("No Internet Connection")
                    }
                    isDisconnected = true
                } else if (connected && isDisconnected) {
                    showSmallBottomToast("Back Online")
                    isDisconnected = false
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        registerReceiver(connectivityReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(connectivityReceiver)
    }

    private fun isConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showLargeCenteredToast(message: String) {
        val layoutInflater = layoutInflater
        val layout = layoutInflater.inflate(R.layout.toast_large_centered, findViewById(android.R.id.content), false)
        val textView = layout.findViewById<TextView>(R.id.toastText)
        textView.text = message

        val toast = Toast(applicationContext)
        toast.view = layout
        toast.duration = Toast.LENGTH_LONG
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }

    private fun showSmallBottomToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
