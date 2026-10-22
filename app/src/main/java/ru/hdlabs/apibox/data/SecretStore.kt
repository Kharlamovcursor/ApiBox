package ru.hdlabs.apibox.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecretStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences by lazy { open(appContext) }

    fun get(key: String): String = prefs.getString(key, "").orEmpty()

    fun put(key: String, value: String) {
        prefs.edit().putString(key, value).commit()
    }

    fun remove(key: String) {
        prefs.edit().remove(key).commit()
    }

    private fun open(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    companion object {
        private const val FILE_NAME = "apibox_secrets"
    }
}

object SecretKeys {
    fun client(id: String) = "acc_${id}_client"
    fun key(id: String) = "acc_${id}_key"
    fun token(id: String) = "acc_${id}_token"
    fun headers(id: String) = "custom_${id}_headers"
    fun body(id: String) = "custom_${id}_body"
}
