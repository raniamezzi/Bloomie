package com.example.bloomie.data.db

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences wrapper for local account credentials, login state,
 * Remember Me preferences, and the first-launch onboarding flag.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** True once the user has signed in (and stayed remembered) and not logged out. */
    val isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false) && userName.isNotBlank()

    /** Display name, or an empty string when nobody is signed in. */
    val userName: String
        get() = prefs.getString(KEY_USER_NAME, "").orEmpty()

    /** Email address, or an empty string when nobody is signed in. */
    val userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "").orEmpty()

    /** True until the welcome/onboarding flow has been completed once. */
    val isFirstLaunch: Boolean
        get() = !prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    // ---------------------------------------------------------------------------------------
    // Account registration & password verification
    // ---------------------------------------------------------------------------------------

    /** True if an account has already been registered with [email]. */
    fun hasAccount(email: String): Boolean {
        val key = email.trim().lowercase()
        return prefs.contains("$PREFIX_PASS$key")
    }

    /** Saves a new account's name and password keyed by lowercase email. */
    fun registerAccount(name: String, email: String, password: String) {
        val key = email.trim().lowercase()
        prefs.edit()
            .putString("$PREFIX_NAME$key", name.trim())
            .putString("$PREFIX_PASS$key", password)
            .apply()
    }

    /** Returns true only if [email] exists and [password] matches the stored password. */
    fun verifyCredentials(email: String, password: String): Boolean {
        val key = email.trim().lowercase()
        val storedPassword = prefs.getString("$PREFIX_PASS$key", null) ?: return false
        return storedPassword == password
    }

    /** Returns the stored display name for [email], or null if not found. */
    fun getRegisteredName(email: String): String? {
        val key = email.trim().lowercase()
        return prefs.getString("$PREFIX_NAME$key", null)
    }

    // ---------------------------------------------------------------------------------------
    // Session & Remember Me (TP SharedPreferences)
    // ---------------------------------------------------------------------------------------

    val isRemembered: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_ME, false)

    val rememberedName: String
        get() = prefs.getString(KEY_REMEMBER_NAME, "").orEmpty()

    val rememberedEmail: String
        get() = prefs.getString(KEY_REMEMBER_EMAIL, "").orEmpty()

    val rememberedPassword: String
        get() = prefs.getString(KEY_REMEMBER_PASSWORD, "").orEmpty()

    /**
     * Stores the profile, updates the login flag, and saves or clears Remember Me credentials.
     */
    fun saveLogin(name: String, email: String, password: String = "", rememberMe: Boolean = true) {
        val editor = prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, rememberMe)
            .putString(KEY_USER_NAME, name.trim())
            .putString(KEY_USER_EMAIL, email.trim())
            .putBoolean(KEY_REMEMBER_ME, rememberMe)

        if (rememberMe) {
            editor.putString(KEY_REMEMBER_NAME, name.trim())
            editor.putString(KEY_REMEMBER_EMAIL, email.trim())
            editor.putString(KEY_REMEMBER_PASSWORD, password)
        } else {
            editor.remove(KEY_REMEMBER_NAME)
            editor.remove(KEY_REMEMBER_EMAIL)
            editor.remove(KEY_REMEMBER_PASSWORD)
        }
        editor.apply()
    }

    /** Marks onboarding as completed so the welcome screen is not shown again. */
    fun setOnboardingComplete() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    /** Clears the active login and profile while keeping registered accounts and Remember Me info. */
    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .apply()
    }

    /** Wipes everything, including accounts, Remember Me, and the onboarding flag. */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "bloomie_session"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_ONBOARDING_DONE = "onboarding_done"

        const val KEY_REMEMBER_ME = "remember_me"
        const val KEY_REMEMBER_NAME = "remember_name"
        const val KEY_REMEMBER_EMAIL = "remember_email"
        const val KEY_REMEMBER_PASSWORD = "remember_password"

        const val PREFIX_NAME = "account_name_"
        const val PREFIX_PASS = "account_pass_"
    }
}