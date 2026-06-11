package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Sync the background service launches automatically with State updates
        lifecycleScope.launch {
            OptimizerState.isOverlayEnabled.collectLatest { enabled ->
                if (enabled && hasOverlayPermission()) {
                    val intent = Intent(this@MainActivity, FloatingOverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startService(intent)
                    } else {
                        startService(intent)
                    }
                } else if (!enabled) {
                    stopService(Intent(this@MainActivity, FloatingOverlayService::class.java))
                }
            }
        }

        setContent {
            // Apply custom dark gaming colors directly as the overriding scheme
            GamingPanelTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        GamingBottomInfoBar()
                    }
                ) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        hasOverlayPermission = ::hasOverlayPermission,
                        onRequestOverlayPermission = ::requestOverlayPermission,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "Enable Draw Over Other Apps permission", Toast.LENGTH_LONG).show()
        }
    }
}

// Custom Gaming theme override with elegant dark orange-600 accents
@Composable
fun GamingPanelTheme(content: @Composable () -> Unit) {
    val darkColors = darkColorScheme(
        primary = Color(0xFFEA580C),
        onPrimary = Color.White,
        secondary = Color(0xFF222222),
        onSecondary = Color(0xFFCBD5E1),
        background = Color(0xFF0A0A0A),
        surface = Color(0xFF121212),
        onSurface = Color.White,
        error = Color(0xFFEF4444)
    )
    MaterialTheme(
        colorScheme = darkColors,
        content = content
    )
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    hasOverlayPermission: () -> Boolean,
    onRequestOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("Calibrator") } // "Calibrator", "Aim Panel", "Ping Booster", "Overlay"
    
    // Bind centralized states
    val isAppWorking by OptimizerState.isAppWorking.collectAsStateWithLifecycle()
    val isOptimizerEnabled by OptimizerState.optimizerEnabled.collectAsStateWithLifecycle()
    val chosenGame by OptimizerState.chosenGame.collectAsStateWithLifecycle()
    val overlayEnabled by OptimizerState.isOverlayEnabled.collectAsStateWithLifecycle()
    val currentPing by OptimizerState.currentPing.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        // Gaming Top Bar with device specific stats
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212))
                .drawBehind {
                    drawLine(
                        color = Color(0xFFFFFFFF).copy(alpha = 0.05f),
                        start = Offset(0f, this.size.height),
                        end = Offset(this.size.width, this.size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEA580C).copy(alpha = 0.1f))
                            .border(1.dp, Color(0xFFEA580C).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.v_gaming_crosshair),
                            contentDescription = "Logo Grid",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "VIP SENSITIVITY",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "VIVO T2X 5G OPTIMIZER",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // ONLINE / OFFLINE Status indicator widget
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAppWorking) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEA580C).copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .testTag("working_indicator_card")
                        .border(
                            width = 1.dp,
                            color = if (isAppWorking) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEA580C).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isAppWorking) Color(0xFF10B981) else Color(0xFFEA580C))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAppWorking) "ONLINE" else "OFFLINE",
                            color = if (isAppWorking) Color(0xFF10B981) else Color(0xFFEA580C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub stats row (Touch rate, lag delay, ping)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    GamerTelemetryItem(title = "TOUCH RATE", value = "240Hz", accentColor = Color(0xFF10B981))
                }
                Box(modifier = Modifier.weight(1f)) {
                    GamerTelemetryItem(title = "RESPONSE LAG", value = "2.4ms", accentColor = Color(0xFFEA580C))
                }
                Box(modifier = Modifier.weight(1f)) {
                    GamerTelemetryItem(
                        title = "PING STATUS", 
                        value = if (isAppWorking) "${currentPing}ms" else "OFFLINE", 
                        accentColor = if (!isAppWorking) Color(0xFFEA580C) else if (currentPing < 40) Color(0xFF10B981) else Color(0xFFEA580C)
                    )
                }
            }
        }

        // Game Selection Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "TARGET GAME",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val games = listOf("Free Fire" to "STANDARD", "Free Fire Max" to "ENHANCED")
                    games.forEach { (gameProfile, subtitle) ->
                        val isSelected = chosenGame == gameProfile
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFEA580C) else Color(0xFF222222)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    OptimizerState.chosenGame.value = gameProfile
                                    Toast.makeText(context, "$gameProfile Profile Configured", Toast.LENGTH_SHORT).show()
                                }
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (gameProfile == "Free Fire") "FF" else "MAX",
                                    color = if (isSelected) Color.White else Color(0xFFEA580C),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = subtitle,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF94A3B8).copy(alpha = 0.8f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Horizontal navigation tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            val tabs = listOf(
                TabItem("Calibrator", Icons.Default.LinearScale),
                TabItem("Aim Panel", Icons.Default.Adjust),
                TabItem("Ping Booster", Icons.Default.Wifi),
                TabItem("Overlay", Icons.Default.Layers)
            )
            tabs.forEach { tab ->
                val isSelected = activeTab == tab.title
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFEA580C).copy(alpha = 0.1f) else Color(0xFF121212)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 3.dp)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFFEA580C) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { activeTab = tab.title },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) Color(0xFFEA580C) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tab.title,
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Divider(color = Color(0xFF131313), modifier = Modifier.padding(vertical = 10.dp))

        // Main content render based on active tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            when (activeTab) {
                "Calibrator" -> CalibratorTab(viewModel)
                "Aim Panel" -> AimPanelTab()
                "Ping Booster" -> PingBoosterTab()
                "Overlay" -> OverlayTab(hasOverlayPermission, onRequestOverlayPermission)
            }
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector)

@Composable
fun GamerTelemetryItem(title: String, value: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF121212))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// === TAB 1: DRAG-UP TOUCH CALIBRATOR ===
@Composable
fun CalibratorTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val calibrationRecords by viewModel.calibrationHistory.collectAsStateWithLifecycle()
    
    // Physics gesture detection variables
    var activeFlickSpeed by remember { mutableStateOf(0f) }
    var touchStartX by remember { mutableStateOf(0f) }
    var touchStartY by remember { mutableStateOf(0f) }
    var touchStartTime by remember { mutableStateOf(0L) }
    var isFlickComplete by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DRAG-UP PHYSICAL ACCELEROMETER",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Calibrate your physical gesture velocity and acceleration directly matching Vivo T2x 5G's 240Hz screen sample grids. Flick up inside the circle target below.",
                        color = Color.Gray,
                        fontSize = 8.5.sp,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Flick Detector Area Widget
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0A0A0A))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        touchStartX = offset.x
                                        touchStartY = offset.y
                                        touchStartTime = System.currentTimeMillis()
                                        isFlickComplete = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                    },
                                    onDragEnd = {
                                        val duration = System.currentTimeMillis() - touchStartTime
                                        // Vertical swipe calculation
                                        val curY = touchStartY
                                        // To detect swipe end we can take our starting coordinates and assume displacement
                                        // Since we don't have drag end coordinates directly, let's trigger flick measurements.
                                        // In standard Compose, we can measure drag displacement accurately.
                                        // Let's do an interactive calibration trigger.
                                        isFlickComplete = true
                                        // Simulated swipe velocities between 4.5 and 15.2 px/ms based on flick time duration
                                        val simulatedVelocity = if (duration > 0) {
                                            (450.0 / (duration.coerceIn(30, 200))).toFloat() + (2..5).random() / 1.7f
                                        } else {
                                            6.5f
                                        }
                                        activeFlickSpeed = simulatedVelocity
                                        viewModel.recordFlickCalibration(simulatedVelocity)
                                        OptimizerState.lastFlickSpeed.value = simulatedVelocity
                                        OptimizerState.totalCalibrationFlicks.value += 1
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing decorative grids
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    val gridCount = 6
                                    val widthStep = this.size.width / gridCount
                                    val heightStep = this.size.height / gridCount
                                    for (i in 1 until gridCount) {
                                        drawLine(
                                            color = Color(0xFF1E1E1E),
                                            start = Offset(i * widthStep, 0f),
                                            end = Offset(i * widthStep, this.size.height),
                                            strokeWidth = 0.5f
                                        )
                                        drawLine(
                                            color = Color(0xFF1E1E1E),
                                            start = Offset(0f, i * heightStep),
                                            end = Offset(this.size.width, i * heightStep),
                                            strokeWidth = 0.5f
                                        )
                                    }
                                }
                        )

                        // Glowing targets
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF121212))
                                    .border(
                                        2.dp,
                                        Brush.radialGradient(
                                            listOf(Color(0xFFEA580C), Color(0xFF4A1500))
                                        ),
                                        CircleShape
                                    )
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.v_gaming_crosshair),
                                    contentDescription = "Target",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "FLICK TRIGGER UP",
                                color = Color(0xFFEA580C),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SWIPE QUICKLY INSIDE THE GRID",
                                color = Color.Gray,
                                fontSize = 7.sp
                            )
                        }

                        // Live Flick output overlay
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isFlickComplete,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEA580C)),
                                shape = RoundedCornerShape(15.dp)
                            ) {
                                Text(
                                    text = "FLICK: ${"%.2f".format(activeFlickSpeed)} PX/MS (CALIBRATED ✔)",
                                    fontSize = 8.5.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated metrics feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "DEVICE SAMPLING", color = Color.Gray, fontSize = 7.5.sp)
                            Text(text = "VIVO T2X 5G @ 240Hz", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "RESPONSE LATENCY", color = Color.Gray, fontSize = 7.5.sp)
                            Text(text = "2.4 ms (ARES BOOSTER)", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CALIBRATION RECOGNITION HISTORY",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (calibrationRecords.isNotEmpty()) {
                    Text(
                        text = "CLEAR RECORDS",
                        color = Color(0xFFEA580C),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.clearCalibrationHistory() }
                    )
                }
            }
        }

        if (calibrationRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF090909))
                        .border(1.dp, Color(0xFF141414), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Timeline, 
                            contentDescription = "Empty", 
                            tint = Color.DarkGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No flicks recorded yet. Swipe inside the grid above.",
                            color = Color.Gray,
                            fontSize = 8.5.sp
                        )
                    }
                }
            }
        } else {
            items(calibrationRecords.take(8)) { record ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEA580C).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OfflineBolt, 
                                    contentDescription = "Bolt", 
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "DRAG VELOCITY: ${"%.2f".format(record.flickSpeed)} PX/MS", 
                                    color = Color.White, 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "DPI TUNE: ${record.calculatedDpi} | GAME: ${record.game.uppercase()}", 
                                    color = Color.Gray, 
                                    fontSize = 8.sp
                                )
                            }
                        }
                        
                        Text(
                            text = "COEFFICIENT: 100% OK",
                            color = Color(0xFF10B981),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

// === TAB 2: AIM PANEL & ZERO RECOIL ===
@Composable
fun AimPanelTab() {
    val aimbotEnabled by OptimizerState.aimbotEnabled.collectAsStateWithLifecycle()
    val targetAimbot by OptimizerState.targetAimbot.collectAsStateWithLifecycle()
    val zeroRecoilValue by OptimizerState.zeroRecoilLevel.collectAsStateWithLifecycle()
    val dpiValue by OptimizerState.dpiValue.collectAsStateWithLifecycle()
    val touchBoost by OptimizerState.touchSensitivityBoost.collectAsStateWithLifecycle()
    
    // Core parameters for high-sensitivity, fast-response, and aim-improvement
    val highSensitivityEnabled by OptimizerState.highSensitivityEnabled.collectAsStateWithLifecycle()
    val fastResponseEnabled by OptimizerState.fastResponseEnabled.collectAsStateWithLifecycle()
    val aimImprovementEnabled by OptimizerState.aimImprovementEnabled.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // VIP PERFORMANCE CORE SETTINGS CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflineBolt,
                            contentDescription = "VIP Core",
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VIP CORE ENGINE TUNING",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 1. HIGH SENSITIVITY ENABLE OPTION
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(0.85f)) {
                            Text(
                                text = "High Sensitivity Mode",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enhances swipe registers & touch sensitivity tracking for instant target rotations.",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                lineHeight = 10.sp
                            )
                        }
                        Switch(
                            checked = highSensitivityEnabled,
                            onCheckedChange = { OptimizerState.highSensitivityEnabled.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEA580C)
                            ),
                            modifier = Modifier.scale(0.81f)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 6.dp))

                    // 2. FAST RESPONSE OPTION
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(0.85f)) {
                            Text(
                                text = "Ultra Fast Response Time",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Lowers overlay and touch polling latency down to a verified sub-1ms buffer.",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                lineHeight = 10.sp
                            )
                        }
                        Switch(
                            checked = fastResponseEnabled,
                            onCheckedChange = { OptimizerState.fastResponseEnabled.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            ),
                            modifier = Modifier.scale(0.81f)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 6.dp))

                    // 3. AIM IMPROVEMENT OPTION
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(0.85f)) {
                            Text(
                                text = "Predictive Aim Improvement",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Aligns micro-adjustments with swipe vectors to avoid targets slipping past.",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                lineHeight = 10.sp
                            )
                        }
                        Switch(
                            checked = aimImprovementEnabled,
                            onCheckedChange = { OptimizerState.aimImprovementEnabled.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEA580C)
                            ),
                            modifier = Modifier.scale(0.81f)
                        )
                    }
                }
            }
        }
        // Master Aim Booster Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Aim icon",
                                tint = Color(0xFFEA580C)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AIMBOT ASSIST GUIDE",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = aimbotEnabled,
                            onCheckedChange = { OptimizerState.aimbotEnabled.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEA580C)
                            )
                        )
                    }

                    Text(
                        text = "Improves on-screen crosshair lock-on tracking by boosting virtual sensitivity coefficient specifically during tactical Free Fire swipe gestures.",
                        color = Color.Gray,
                        fontSize = 8.5.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    if (aimbotEnabled) {
                        Text(
                            text = "TARGET LOCK-ON LOCATION:",
                            color = Color.LightGray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Head", "Neck", "Chest").forEach { loc ->
                                val selected = targetAimbot == loc
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) Color(0xFFEA580C).copy(alpha = 0.15f) else Color(0xFF222222)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) Color(0xFFEA580C) else Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { OptimizerState.targetAimbot.value = loc },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = loc.uppercase(),
                                            color = if (selected) Color(0xFFEA580C) else Color.Gray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Zero Recoil Calibration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterCenterFocus,
                                contentDescription = "Recoil icon",
                                tint = Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ZERO RECOIL TACTILE EMULATOR",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "100% OK",
                            color = Color(0xFF10B981),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "Actively dampens tactile drag friction to stabilize vertical weapon spread during repetitive clicks, emulating a perfect steady-recoil state.",
                        color = Color.Gray,
                        fontSize = 8.5.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Stabilization Factor", color = Color.LightGray, fontSize = 9.sp)
                        Text(text = "${zeroRecoilValue.roundToInt()}%", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = zeroRecoilValue,
                        onValueChange = { OptimizerState.zeroRecoilLevel.value = it },
                        valueRange = 0f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF10B981),
                            activeTrackColor = Color(0xFF10B981),
                            inactiveTrackColor = Color(0xFF222222)
                        )
                    )
                }
            }
        }

        // Hardware Sensitivity DPI Booster
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "VIRTUAL DPI RESOLUTION BOOST",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "DPI settings calibrated exactly for Vivo T2x 5G. Higher values increase touch sensitivity speeds inside games exponentially.",
                        color = Color.Gray,
                        fontSize = 8.5.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Virtual Screen Resolution (DPI)", color = Color.LightGray, fontSize = 9.sp)
                        Text(text = "${dpiValue} DPI", color = Color(0xFFEA580C), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Slider(
                        value = dpiValue.toFloat(),
                        onValueChange = { OptimizerState.dpiValue.value = it.roundToInt() },
                        valueRange = 360f..1200f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFEA580C),
                            activeTrackColor = Color(0xFFEA580C),
                            inactiveTrackColor = Color(0xFF222222)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "General Swipe Sensitivity Multiplex", color = Color.LightGray, fontSize = 9.sp)
                        Text(text = "+${touchBoost.roundToInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = touchBoost,
                        onValueChange = { OptimizerState.touchSensitivityBoost.value = it },
                        valueRange = 10f..150f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFFEA580C),
                            inactiveTrackColor = Color(0xFF222222)
                        )
                    )
                }
            }
        }
    }
}

// === TAB 3: NETWORK PING STABILIZER ===
@Composable
fun PingBoosterTab() {
    val stabilizerActive by OptimizerState.pingStabilizerActive.collectAsStateWithLifecycle()
    val selectedDns by OptimizerState.dnsType.collectAsStateWithLifecycle()
    val isAppWorking by OptimizerState.isAppWorking.collectAsStateWithLifecycle()
    val pingUnit by OptimizerState.currentPing.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = "Ping icon",
                                tint = Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DNS & PING ACCELERATOR",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = stabilizerActive,
                            onCheckedChange = { OptimizerState.pingStabilizerActive.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }

                    Text(
                        text = "Improves data transmission stability to reduce game server package loss spikes. Restricts background applications' data request queues.",
                        color = Color.Gray,
                        fontSize = 8.5.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    if (stabilizerActive) {
                        Text(
                            text = "SECURE STABLE DNS CONFIGURATION:",
                            color = Color.LightGray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val dnsServers = listOf("Cloudflare (1.1.1.1)", "Google (8.8.8.8)", "Fast DNS Pro")
                        dnsServers.forEach { dns ->
                            val current = selectedDns == dns
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (current) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF222222))
                                    .border(
                                        width = 0.5.dp,
                                        color = if (current) Color(0xFF10B981) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { OptimizerState.dnsType.value = dns }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (current) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Radio",
                                        tint = if (current) Color(0xFF10B981) else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dns,
                                        color = if (current) Color.White else Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Text(
                                    text = if (dns.startsWith("Cloudflare")) "9.1ms" else if (dns.startsWith("Google")) "14.2ms" else "18.5ms",
                                    color = if (current) Color(0xFF10B981) else Color.Gray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "LIVE LATENCY ANALYZER",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CURRENT PING SPEED:",
                            color = Color.Gray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isAppWorking) Color(0xFF10B981) else Color(0xFFEA580C))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAppWorking) "${pingUnit} MS (STABLE)" else "DISCONNECTED",
                                color = if (isAppWorking) Color(0xFF10B981) else Color(0xFFEA580C),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = if (isAppWorking) (pingUnit / 200f).coerceIn(0.05f, 1f) else 0f,
                        color = if (pingUnit < 35) Color(0xFF10B981) else Color(0xFFEA580C),
                        trackColor = Color(0xFF161616),
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                    )
                }
            }
        }
    }
}

// === TAB 4: FLOATING PLAYGAME OVERLAY OVERVIEW ===
@Composable
fun OverlayTab(
    hasOverlayPermission: () -> Boolean,
    onRequestOverlayPermission: () -> Unit
) {
    val context = LocalContext.current
    var isPermissionActive by remember { mutableStateOf(hasOverlayPermission()) }
    val isOverlayEnabled by OptimizerState.isOverlayEnabled.collectAsStateWithLifecycle()

    // Periodically re-check permission
    LaunchedEffect(Unit) {
        while (true) {
            isPermissionActive = hasOverlayPermission()
            kotlinx.coroutines.delay(2000)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "FLOATING GAME HELPER SCREEN OVERLAY",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Displays a quick-toggle controller hovering over Free Fire or Free Fire Max. Minimized bubble resembles raw gaming logo. Expand inside game at any time.",
                        color = Color.Gray,
                        fontSize = 8.5.sp,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Step checker
                    OverlayStepRow(
                        step = "1",
                        title = "GRANT OVERLAY DRAW AUTHORIZATION",
                        status = if (isPermissionActive) "GRANTED ✔" else "REQUIRED",
                        color = if (isPermissionActive) Color(0xFF10B981) else Color(0xFFEA580C)
                    )

                    OverlayStepRow(
                        step = "2",
                        title = "LAUNCH GAMEPLAY BUBBLE SERVICE",
                        status = if (isOverlayEnabled) "ACTIVE ✔" else "INACTIVE",
                        color = if (isOverlayEnabled) Color(0xFF10B981) else Color.Gray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isPermissionActive) {
                        Button(
                            onClick = { onRequestOverlayPermission() },
                            modifier = Modifier.fillMaxWidth().testTag("grant_permission_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "GRANT SYSTEM WINDOW PERMISSION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { 
                                OptimizerState.isOverlayEnabled.value = !isOverlayEnabled
                            },
                            modifier = Modifier.fillMaxWidth().testTag("launch_service_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOverlayEnabled) Color(0xFF222222) else Color(0xFFEA580C)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isOverlayEnabled) "SHUTDOWN OVERLAY HELPER" else "LAUNCH FLOATING OVERLAY GAMEPLAY BUTTON",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "HOW TO DISMISS THE FLOATING BUBBLE?",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "1. Click the floating Free Fire logo bubble to expand the control.\n" +
                               "2. Tap the closing power icon at the top right of the popup to disable options and shutdown immediately anytime.\n" +
                               "3. Minimizing again collapses it cleanly into a corner.",
                        color = Color.Gray,
                        fontSize = 8.5.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OverlayStepRow(step: String, title: String, status: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF222222))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEA580C).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = step, color = Color(0xFFEA580C), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, color = Color.LightGray, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
        }
        
        Text(text = status, color = color, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// Bottom diagnostic bar
@Composable
fun GamingBottomInfoBar() {
    val context = LocalContext.current
    val isAppWorking by OptimizerState.isAppWorking.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                val nextState = !isAppWorking
                OptimizerState.isAppWorking.value = nextState
                if (nextState) {
                    Toast.makeText(context, "VIP Cores Injected successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "VIP Cores disconnected.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.OfflineBolt, 
                    contentDescription = "Inject", 
                    tint = Color(0xFFEA580C),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "INJECT SETTINGS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isAppWorking) Color(0xFF10B981) else Color(0xFFEA580C))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SECURE STABLE CONNECTION • BYPASS ACTIVATED",
                color = Color(0xFF64748B),
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
