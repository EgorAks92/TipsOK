package com.chaiok.pos.data.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedPrefsSensitiveStorage(context: Context) : SensitiveStorage {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_chaiok",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun saveEncryptedCardToken(token: String) {
        prefs.edit().putString("card_token", token).apply()
    }

    override fun readEncryptedCardToken(): String? = prefs.getString("card_token", null)
}
