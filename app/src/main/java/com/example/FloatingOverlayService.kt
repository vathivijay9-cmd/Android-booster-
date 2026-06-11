package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayContainer: FrameLayout? = null
    private var composeView: ComposeView? = null
    private lateinit var lifecycleOwner: OverlayLifecycleOwner
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()

        // Sync service state with global state Flow
        serviceScope.launch {
            OptimizerState.isOverlayEnabled.collectLatest { enabled ->
                if (enabled) {
                    showOverlay()
                } else {
                    removeOverlayAndService()
                }
            }
        }
    }

    private fun showOverlay() {
        if (overlayContainer != null) return // Already showing

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val container = FrameLayout(this)
        val compose = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            
            setContent {
                MaterialTheme {
                    OverlayContent(
                        onDrag = { dx, dy ->
                            layoutParams.x = (layoutParams.x + dx).roundToInt().coerceIn(0, 2000)
                            layoutParams.y = (layoutParams.y + dy).roundToInt().coerceIn(0, 3000)
                            if (container.parent != null) {
                                windowManager.updateViewLayout(container, layoutParams)
                            }
                        },
                        onCloseOverlay = {
                            OptimizerState.isOverlayEnabled.value = false
                        }
                    )
                }
            }
        }

        container.addView(compose)
        windowManager.addView(container, layoutParams)
        
        overlayContainer = container
        composeView = compose
    }

    private fun removeOverlay() {
        overlayContainer?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayContainer = null
            composeView = null
        }
    }

    private fun removeOverlayAndService() {
        removeOverlay()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        lifecycleOwner.onDestroy()
        serviceScope.cancel()
    }
}

// Custom lifecycle owner required to host Jetpack Compose in an Android Service overlay
class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

// Composable content inside overlay
@Composable
fun OverlayContent(
    onDrag: (Float, Float) -> Unit,
    onCloseOverlay: () -> Unit
) {
    var isMinimized by remember { mutableStateOf(true) }
    
    // Grab states from our centralized atomic Flows
    val isAppWorking by OptimizerState.isAppWorking.collectAsState()
    val isOptimizerEnabled by OptimizerState.optimizerEnabled.collectAsState()
    val aimbotEnabled by OptimizerState.aimbotEnabled.collectAsState()
    val zeroRecoilLevel by OptimizerState.zeroRecoilLevel.collectAsState()
    val choiceGame by OptimizerState.chosenGame.collectAsState()
    val dpiValue by OptimizerState.dpiValue.collectAsState()
    val pingStabilizer by OptimizerState.pingStabilizerActive.collectAsState()
    val currentPing by OptimizerState.currentPing.collectAsState()
    
    val highSensitivityEnabled by OptimizerState.highSensitivityEnabled.collectAsState()
    val fastResponseEnabled by OptimizerState.fastResponseEnabled.collectAsState()
    val aimImprovementEnabled by OptimizerState.aimImprovementEnabled.collectAsState()

    val sizeAnimation by animateDpAsState(
        targetValue = if (isMinimized) 58.dp else 225.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
    )

    Box(
        modifier = Modifier
            .width(sizeAnimation)
            .wrapContentHeight()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        if (isMinimized) {
            // Pulsing gaming bubble
            val infiniteTransition = rememberInfiniteTransition()
            val scalePulse by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            val rotatePulse by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(12000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A0A0A).copy(alpha = 0.9f))
                    .border(
                        2.dp,
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFFEA580C),
                                Color(0xFF9A3412),
                                Color(0xFFEA580C)
                            )
                        ),
                        CircleShape
                    )
                    .clickable { isMinimized = false }
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                // Draws our beautiful custom-generated neon-red gamer target foreground!
                Image(
                    painter = painterResource(id = R.drawable.v_gaming_crosshair),
                    contentDescription = "FF Mini Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotatePulse)
                )
            }
        } else {
            // Expanded Controller Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFA161616))
                    .border(1.5.dp, Color(0xFFEA580C), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                // Overlay header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.v_gaming_crosshair),
                            contentDescription = "Gamer Icon",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "VIVO T2x PANEL",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // Working Indicator (Online/Offline)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isAppWorking) Color(0xFF10B981) else Color(0xFFEA580C))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAppWorking) "ONLINE" else "OFFLINE",
                                    color = if (isAppWorking) Color(0xFF10B981) else Color(0xFFEA580C),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Row {
                        IconButton(
                            onClick = { isMinimized = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloseFullscreen,
                                contentDescription = "Minimize",
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { onCloseOverlay() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Close",
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Divider(
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (!isOptimizerEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CALIBRATOR DISABLED",
                            color = Color(0xFFEA580C),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Fast toggles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Aimbot Assist", color = Color.LightGray, fontSize = 10.sp)
                        Switch(
                            checked = aimbotEnabled,
                            onCheckedChange = { OptimizerState.aimbotEnabled.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEA580C)
                            ),
                            modifier = Modifier.scale(0.7f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Zero Recoil", color = Color.LightGray, fontSize = 10.sp)
                        Switch(
                            checked = zeroRecoilLevel > 0f,
                            onCheckedChange = { 
                                OptimizerState.zeroRecoilLevel.value = if (it) 100f else 0f 
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            ),
                            modifier = Modifier.scale(0.7f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "High Sensitivity", color = Color.LightGray, fontSize = 10.sp)
                        Switch(
                            checked = highSensitivityEnabled,
                            onCheckedChange = { OptimizerState.highSensitivityEnabled.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEA580C)
                            ),
                            modifier = Modifier.scale(0.7f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Fast Response", color = Color.LightGray, fontSize = 10.sp)
                        Switch(
                            checked = fastResponseEnabled,
                            onCheckedChange = { OptimizerState.fastResponseEnabled.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            ),
                            modifier = Modifier.scale(0.7f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Aim Improvement", color = Color.LightGray, fontSize = 10.sp)
                        Switch(
                            checked = aimImprovementEnabled,
                            onCheckedChange = { OptimizerState.aimImprovementEnabled.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEA580C)
                            ),
                            modifier = Modifier.scale(0.7f)
                        )
                    }

                    // Zero Recoil Level slider
                    if (zeroRecoilLevel > 0f) {
                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Recoil Suppression", color = Color.Gray, fontSize = 8.sp)
                                Text(text = "${zeroRecoilLevel.roundToInt()}%", color = Color(0xFF10B981), fontSize = 8.sp)
                            }
                            Slider(
                                value = zeroRecoilLevel,
                                onValueChange = { OptimizerState.zeroRecoilLevel.value = it },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF10B981),
                                    activeTrackColor = Color(0xFF10B981),
                                    inactiveTrackColor = Color(0xFF222222)
                                ),
                                modifier = Modifier
                                    .padding(vertical = 0.dp)
                                    .height(18.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "DPI Boost (${dpiValue})", color = Color.LightGray, fontSize = 10.sp)
                        Text(text = choiceGame, color = Color(0xFFEA580C), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Ping Stabilizer", color = Color.LightGray, fontSize = 10.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${currentPing}ms",
                                color = if (currentPing < 35) Color(0xFF10B981) else Color(0xFFEA580C),
                                fontSize = 8.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Switch(
                                checked = pingStabilizer,
                                onCheckedChange = { OptimizerState.pingStabilizerActive.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF10B981)
                                ),
                                modifier = Modifier.scale(0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                // Quick global bypass switch
                Button(
                    onClick = { 
                        OptimizerState.optimizerEnabled.value = !isOptimizerEnabled 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOptimizerEnabled) Color(0xFF222222) else Color(0xFFEA580C)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isOptimizerEnabled) "DISABLE ALL OPTIONS" else "ENABLE ALL OPTIONS",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


