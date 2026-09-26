package com.noora.assistant.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NooraConnectivity(private val context: Context) {
    fun isOnline(): Boolean {
        val cm=context.getSystemService(ConnectivityManager::class.java) ?: return false
        val n=cm.activeNetwork ?: return false
        val c=cm.getNetworkCapabilities(n) ?: return false
        return c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    fun mode() = if (isOnline()) NooraMode.ONLINE else NooraMode.OFFLINE
}
