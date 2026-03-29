package com.expensemanager.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.expensemanager.utils.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("expense_manager_prefs")

class UserPreferences(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val securePrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "expense_manager_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LOCK] ?: false
    }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC] ?: false
    }

    val monthlyBudget: Flow<Double?> = context.dataStore.data.map { prefs -> prefs[KEY_BUDGET] }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: ThemeMode.SYSTEM
    }

    val useDynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC] ?: false
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_APP_LOCK] = enabled }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRIC] = enabled }
        if (!enabled) {
            // PIN remains; user can still unlock with PIN
        }
    }

    suspend fun setMonthlyBudget(amount: Double?) {
        context.dataStore.edit { prefs ->
            if (amount == null || amount <= 0) {
                prefs.remove(KEY_BUDGET)
            } else {
                prefs[KEY_BUDGET] = amount
            }
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME] = mode }
    }

    suspend fun setUseDynamicColor(use: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC] = use }
    }

    fun hasPin(): Boolean = securePrefs.contains(KEY_PIN)

    fun savePin(pin: String) {
        securePrefs.edit().putString(KEY_PIN, pin.trim()).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = securePrefs.getString(KEY_PIN, null) ?: return false
        return stored == pin.trim()
    }

    fun clearPin() {
        securePrefs.edit().remove(KEY_PIN).apply()
    }

    companion object {
        private val KEY_APP_LOCK: Preferences.Key<Boolean> = booleanPreferencesKey("app_lock_enabled")
        private val KEY_BIOMETRIC: Preferences.Key<Boolean> = booleanPreferencesKey("biometric_enabled")
        private val KEY_BUDGET: Preferences.Key<Double> = doublePreferencesKey("monthly_budget")
        private val KEY_THEME: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC: Preferences.Key<Boolean> = booleanPreferencesKey("dynamic_color")
        private const val KEY_PIN = "pin_code"
    }
}
