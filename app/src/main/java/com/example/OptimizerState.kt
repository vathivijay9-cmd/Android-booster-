package com.example

import kotlinx.coroutines.flow.MutableStateFlow

object OptimizerState {
    // Basic service & working status indicator
    val isAppWorking = MutableStateFlow(true) // True = ONLINE, False = OFFLINE
    val optimizerEnabled = MutableStateFlow(true)
    val chosenGame = MutableStateFlow("Free Fire Max") // "Free Fire" or "Free Fire Max"
    
    // Aim Improvement & Recoil
    val aimbotEnabled = MutableStateFlow(true)
    val zeroRecoilLevel = MutableStateFlow(100f) // 0% to 100% Zero Recoil
    val targetAimbot = MutableStateFlow("Head") // Head, Neck, Chest
    val dpiValue = MutableStateFlow(720) // 360 to 1200 DPI
    val touchSensitivityBoost = MutableStateFlow(95f) // DPI multiplier boost
    
    // Core performance toggle switches
    val highSensitivityEnabled = MutableStateFlow(true)
    val fastResponseEnabled = MutableStateFlow(true)
    val aimImprovementEnabled = MutableStateFlow(true)
    
    // Drag-Up Calibrator values
    val lastFlickSpeed = MutableStateFlow(0f) // Computed pixel/ms velocity
    val samplingRate = MutableStateFlow(240) // Hz (Vivo T2X 5G hardware spec)
    val responseLag = MutableStateFlow(2.4f) // ms touch response
    val totalCalibrationFlicks = MutableStateFlow(0)
    
    // Ping Stabilizer settings
    val pingStabilizerActive = MutableStateFlow(true)
    val dnsType = MutableStateFlow("Cloudflare (1.1.1.1)")
    val currentPing = MutableStateFlow(24) // ms latency
    
    // Overlay State
    val isOverlayEnabled = MutableStateFlow(false) // Whether overlay should be active
}
