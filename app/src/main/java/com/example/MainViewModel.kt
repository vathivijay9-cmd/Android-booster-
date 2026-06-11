package com.example

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CalibrationRepository
    val calibrationHistory: StateFlow<List<CalibrationRecord>>

    init {
        val database = CalibrationDatabase.getDatabase(application)
        repository = CalibrationRepository(database.calibrationDao())
        
        calibrationHistory = repository.allRecords
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Start our real-time network stability and connection poller
        startNetworkPoller()
    }

    private fun startNetworkPoller() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val start = System.currentTimeMillis()
                val isOnline = checkActualInternetConnection()
                val duration = (System.currentTimeMillis() - start).toInt()

                // Update latency metrics representation
                if (isOnline) {
                    OptimizerState.isAppWorking.value = true
                    OptimizerState.currentPing.value = duration.coerceIn(12, 180)
                } else {
                    OptimizerState.isAppWorking.value = false
                    OptimizerState.currentPing.value = 999
                }
                
                delay(4000) // Poll every 4 seconds
            }
        }
    }

    private fun checkActualInternetConnection(): Boolean {
        return try {
            // First check system basic connectivity manager
            val connManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connManager.activeNetwork ?: return false
            val capabilities = connManager.getNetworkCapabilities(activeNetwork) ?: return false
            
            if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return false
            }

            // Perform a low-overhead socket request to resolve true working status
            val socket = Socket()
            // Connects to Google Public DNS on port 53 with low timeout
            socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Records a flick calibration gesture inside database and updates state
    fun recordFlickCalibration(velocity: Float) {
        val calculatedDpi = OptimizerState.dpiValue.value
        val game = OptimizerState.chosenGame.value
        
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(
                CalibrationRecord(
                    flickSpeed = velocity,
                    calculatedDpi = calculatedDpi,
                    game = game
                )
            )
        }
        
        // Trigger haptic feedback
        triggerPhysicalVibrate(120)
    }

    // Resets calibration database records
    fun clearCalibrationHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clear()
        }
        triggerPhysicalVibrate(250)
    }

    @Suppress("DEPRECATION")
    private fun triggerPhysicalVibrate(durationMs: Long) {
        val context = getApplication<Application>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (e: Exception) {
            // Devices with no vibrator hardware will fail silently
        }
    }
}
