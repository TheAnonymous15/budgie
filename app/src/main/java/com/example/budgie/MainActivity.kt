package com.example.budgie

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.budgie.notifications.NotificationHelper
import com.example.budgie.notifications.NotificationScheduler
import com.example.budgie.security.SecurityManager
import com.example.budgie.ui.navigation.BudgieNavigation
import com.example.budgie.ui.theme.BudgieTheme
import com.example.budgie.ui.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {
    private lateinit var securityManager: SecurityManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Schedule notifications when permission granted
            safeScheduleNotifications()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set window background to dark navy immediately to prevent white flash
        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#061318"))

        securityManager = SecurityManager(this)


        // Initialize notification channels safely - defer to avoid blocking startup
        window.decorView.post {
            try {
                NotificationHelper.createNotificationChannels(this)
                requestNotificationPermission()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing notifications", e)
            }
        }

        enableEdgeToEdge()
        setContent {

            BudgieTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color(0xFF061318) // Dark navy background
                ) {
                    BudgieApp(securityManager)
                }
            }
        }
    }

    private fun safeScheduleNotifications() {
        try {
            NotificationScheduler.scheduleAllNotifications(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error scheduling notifications", e)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted, schedule notifications
                    safeScheduleNotifications()
                }
                else -> {
                    // Request permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For Android 12 and below, no runtime permission needed
            safeScheduleNotifications()
        }
    }

    override fun onStop() {
        super.onStop()
        // Destroy session when app goes to background
        securityManager.destroySession()
    }
}

@Composable
fun BudgieApp(securityManager: SecurityManager) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    BudgieNavigation(
        navController = navController,
        viewModel = viewModel,
        securityManager = securityManager
    )
}

