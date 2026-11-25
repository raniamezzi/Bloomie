package com.example.bloomie.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.example.bloomie.R
import com.example.bloomie.data.db.SessionManager
import com.example.bloomie.databinding.ActivityLoginBinding
import com.example.bloomie.ui.main.MainActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Screen 03: local account creation and sign-in with password verification
 * and Remember Me SharedPreferences.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionManager
    private var isRegisterMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        isRegisterMode = intent.getBooleanExtra(EXTRA_REGISTER, false)
        if (isRegisterMode) {
            binding.txtLoginTitle.setText(R.string.register_title)
            binding.txtLoginSubtitle.setText(R.string.register_subtitle)
            binding.btnLogin.setText(R.string.btn_register)
        } else if (session.isRemembered) {
            // Pre-fill saved credentials from SharedPreferences when Remember Me was checked
            binding.cbRememberMe.isChecked = true
            binding.etName.setText(session.rememberedName)
            binding.etEmail.setText(session.rememberedEmail)
            binding.etPassword.setText(session.rememberedPassword)
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnLogin.setOnClickListener { attemptLogin() }

        clearErrorOnTyping(binding.etName, binding.tilName)
        clearErrorOnTyping(binding.etEmail, binding.tilEmail)
        clearErrorOnTyping(binding.etPassword, binding.tilPassword)

        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else {
                false
            }
        }
    }

    private fun attemptLogin() {
        val name = binding.etName.text?.toString().orEmpty().trim()
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()
        val rememberMe = binding.cbRememberMe.isChecked

        var valid = true

        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.error_name_required)
            valid = false
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_invalid)
            valid = false
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            binding.tilPassword.error = getString(R.string.error_password_short)
            valid = false
        }
        if (!valid) return

        if (isRegisterMode) {
            if (session.hasAccount(email)) {
                binding.tilEmail.error = getString(R.string.error_account_exists)
                return
            }
            session.registerAccount(name, email, password)
        } else {
            if (!session.hasAccount(email)) {
                binding.tilEmail.error = getString(R.string.error_account_not_found)
                return
            }
            if (!session.verifyCredentials(email, password)) {
                binding.tilPassword.error = getString(R.string.error_wrong_password)
                return
            }
        }

        val resolvedName = session.getRegisteredName(email) ?: name
        session.saveLogin(resolvedName, email, password, rememberMe)
        session.setOnboardingComplete()

        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun clearErrorOnTyping(editText: TextInputEditText, layout: TextInputLayout) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (layout.error != null) layout.error = null
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    companion object {
        /** Pass true to show the "Create your account" wording. */
        const val EXTRA_REGISTER = "extra_register"
        private const val MIN_PASSWORD_LENGTH = 6
    }
}