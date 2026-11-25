package com.example.bloomie.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bloomie.data.db.SessionManager
import com.example.bloomie.databinding.ActivityWelcomeBinding

/**
 * Screen 02: hero image, headline and the two entry points into [LoginActivity].
 * Neither button signs anyone in; the user always enters their own details on the next screen.
 */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val session = SessionManager(this)

        binding.btnGetStarted.setOnClickListener {
            session.setOnboardingComplete()
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .putExtra(LoginActivity.EXTRA_REGISTER, true)
            )
        }

        binding.btnHaveAccount.setOnClickListener {
            session.setOnboardingComplete()
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    companion object {
        /** Opens the welcome screen as the only screen of a fresh task (used after logging out). */
        fun startClean(context: Context) {
            val intent = Intent(context, WelcomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }
    }
}
