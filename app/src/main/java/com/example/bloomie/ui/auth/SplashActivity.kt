package com.example.bloomie.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.bloomie.data.db.SessionManager
import com.example.bloomie.databinding.ActivitySplashBinding
import com.example.bloomie.ui.main.MainActivity

/**
 * Screen 01: shows the logo for 1.8 seconds, then routes to the main hub
 * (when signed in) or to the welcome screen (first launch or signed out).
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())

    private val routeRunnable = Runnable { routeNext() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.splashContent.alpha = 0f
        binding.splashContent.translationY = 24f
        binding.splashContent.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700L)
            .start()
    }

    override fun onStart() {
        super.onStart()
        handler.postDelayed(routeRunnable, SPLASH_DELAY_MS)
    }

    override fun onStop() {
        handler.removeCallbacks(routeRunnable)
        super.onStop()
    }

    private fun routeNext() {
        val session = SessionManager(this)
        val target = if (session.isLoggedIn) MainActivity::class.java else WelcomeActivity::class.java
        startActivity(Intent(this, target))
        finish()
    }

    private companion object {
        const val SPLASH_DELAY_MS = 1800L
    }
}
