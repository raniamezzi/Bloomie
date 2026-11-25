package com.example.bloomie.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bloomie.R
import com.example.bloomie.databinding.ActivityMainBinding
import com.example.bloomie.notifications.WateringNotificationHelper

/** Hosts the main hub: a fragment container and the bottom navigation bar. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                WateringNotificationHelper.scheduleDailyReminder(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWateringNotifications()

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    showFragment(HomeFragment())
                    true
                }
                R.id.menu_garden -> {
                    showFragment(GardenFragment())
                    true
                }
                R.id.menu_identify -> {
                    showFragment(IdentifyFragment())
                    true
                }
                R.id.menu_library -> {
                    showFragment(LibraryFragment())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.menu_home
        }
    }

    private fun setupWateringNotifications() {
        WateringNotificationHelper.createChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            WateringNotificationHelper.scheduleDailyReminder(this)
        }
    }

    /** Switches the bottom bar (and therefore the visible fragment) to [menuItemId]. */
    fun selectTab(menuItemId: Int) {
        binding.bottomNav.selectedItemId = menuItemId
    }

    private fun showFragment(fragment: Fragment) {
        val current = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (current != null && current::class == fragment::class) return

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}