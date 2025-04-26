// BaseActivity.kt
package com.example.pixelpedia

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

open class BaseActivity : AppCompatActivity() {

    private lateinit var connectivityReceiver: BroadcastReceiver
    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupConnectivityReceiver()
    }

    private fun setupConnectivityReceiver() {
        connectivityReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!isConnected()) {
                    if (snackbar == null) {
                        snackbar = Snackbar.make(
                            findViewById(android.R.id.content),
                            "No Internet Connectionaskjdhflakjshdflkjahsdfl",
                            Snackbar.LENGTH_INDEFINITE
                        ).setAction("Dismiss") {}
                        snackbar?.show()
                    }
                } else {
                    snackbar?.dismiss()
                    snackbar = null
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
}
