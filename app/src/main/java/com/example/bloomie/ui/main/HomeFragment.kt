package com.example.bloomie.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloomie.R
import com.example.bloomie.data.db.DatabaseHelper
import com.example.bloomie.data.db.SessionManager
import com.example.bloomie.databinding.FragmentHomeBinding
import com.example.bloomie.models.Plant
import com.example.bloomie.ui.auth.WelcomeActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

/**
 * Screen 04, the dashboard: greeting, today's progress and streak,
 * quick actions and the list of plants that need water.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        db = DatabaseHelper(context)
        session = SessionManager(context)

        taskAdapter = TaskAdapter(
            onWater = { plant -> waterPlant(plant) },
            onOpen = { plant -> ScreenLauncher.openPlantDetails(requireContext(), plant.id) }
        )
        binding.recyclerTasks.layoutManager = LinearLayoutManager(context)
        binding.recyclerTasks.adapter = taskAdapter

        binding.btnNotifications.setOnClickListener { ScreenLauncher.openSchedule(requireContext()) }
        binding.actionAddPlant.setOnClickListener { ScreenLauncher.openAddEditPlant(requireContext()) }
        binding.actionCoach.setOnClickListener { ScreenLauncher.openCoach(requireContext()) }
        binding.actionSchedule.setOnClickListener { ScreenLauncher.openSchedule(requireContext()) }
        binding.btnLogout.setOnClickListener { confirmLogout() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerTasks.adapter = null
        db.close()
        _binding = null
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_logout) { _, _ ->
                session.logout()
                WelcomeActivity.startClean(requireContext())
            }
            .show()
    }

    private fun waterPlant(plant: Plant) {
        if (db.markAsWatered(plant.id)) {
            Toast.makeText(
                requireContext(),
                getString(R.string.watered_success, plant.displayName),
                Toast.LENGTH_SHORT
            ).show()
            refresh()
        } else {
            Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show()
        }
    }

    private fun refresh() {
        if (_binding == null) return

        bindGreeting()

        val tasks = db.getPlantsNeedingWater()
        val progress = db.getTodayProgress()
        val streak = db.getCurrentStreakDays()

        taskAdapter.submitList(tasks)
        binding.txtTasksCount.text = tasks.size.toString()
        binding.recyclerTasks.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
        binding.cardEmpty.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE

        binding.txtHeroTitle.text = if (tasks.isEmpty()) {
            getString(R.string.dashboard_hero_all_done)
        } else {
            resources.getQuantityString(R.plurals.dashboard_plants_to_water, tasks.size, tasks.size)
        }

        val percent = (progress.fraction * 100f).toInt()
        binding.progressToday.setProgressCompat(percent, true)
        binding.txtProgress.text = if (progress.total == 0) {
            getString(R.string.dashboard_nothing_due)
        } else {
            getString(R.string.dashboard_progress, progress.completed, progress.total)
        }

        binding.txtStreak.text = if (streak > 0) {
            getString(R.string.dashboard_streak_value, streak)
        } else {
            getString(R.string.dashboard_streak_start)
        }
    }

    private fun bindGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingRes = when {
            hour < 12 -> R.string.greeting_morning
            hour < 17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
        val firstName = session.userName
            .ifBlank { getString(R.string.default_user_name) }
            .trim()
            .substringBefore(' ')
        binding.txtGreeting.text = getString(greetingRes, firstName)
    }
}
