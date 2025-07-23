package com.example.dumb_app.core.repository

import android.content.Context
import android.content.SharedPreferences

class PairingRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pairing", Context.MODE_PRIVATE)

    // Token
    fun getToken(): String? =
        prefs.getString("pair_token", null)

    fun saveToken(token: String) {
        prefs.edit().putString("pair_token", token).apply()
    }

    // 新增：Host
    fun getLastHost(): String? =
        prefs.getString("last_host", null)

    fun saveLastHost(host: String) {
        prefs.edit().putString("last_host", host).apply()
    }
}
