package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.accessibility.DisciplineAccessibilityService
import com.example.focus.FocusSessionManager
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun FocusScreen(
    onNavigateToBlocker: () -> Unit,
    onNavigateToSchedule: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isFocusActive by FocusSessionManager.isFocusActive.collectAsState()
    val timeRemaining by FocusSessionManager.timeRemainingSeconds.collectAsState()
    val totalMinutes by FocusSessionManager.totalDurationMinutes.collectAsState()
    val deskPhotoUri by FocusSessionManager.deskPhotoUri.collectAsState()
    val distractionsBlocked by FocusSessionManager.currentSessionDistractions.collectAsState()
    val isAccessibilityConnected by DisciplineAccessibilityService.isServiceConnected.collectAsState()

    val statsDao = com.example.DisciplineApp.instance.database.disciplineStatsDao()
    val stats by statsDao.getStats().collectAsState(initial = null)
    val currentStats = stats ?: com.example.data.model.DisciplineStats()

    var selectedDuration by remember { mutableIntStateOf(25) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showDeskCaptureHint by remember { mutableStateOf(false) }
    var selectedBadgeForDetail by remember { mutableStateOf<com.example.data.model.Badge?>(null) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var celebratedBadge by remember { mutableStateOf<com.example.data.model.Badge?>(null) }

    LaunchedEffect(Unit) {
        FocusSessionManager.badgeUnlockEvent.collect { badge ->
            celebratedBadge = badge
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            FocusSessionManager.saveDeskPhotoBitmap(context, bitmap)
            showDeskCaptureHint = false
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    val durationOptions = listOf(15, 25, 45, 60, 90)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_focus")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_anim"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("focus_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Focus Streak Hero Card (Consecutive Days & Daily Goal Progress)
        FocusStreakHeroCard(
            stats = currentStats,
            onEditGoal = { showGoalDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Streak Badges Showcase Row (Milestone Badges)
        StreakBadgesShowcase(
            stats = currentStats,
            onBadgeClicked = { selectedBadgeForDetail = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Upcoming Daily Study Schedule & Reminder Preview
        UpcomingScheduleCard(
            onNavigateToSchedule = onNavigateToSchedule,
            onStartSession = { duration ->
                selectedDuration = duration
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Accessibility Service Alert banner if not enabled
        if (!isAccessibilityConnected) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    .testTag("accessibility_warning_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1F0C)),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = NeonAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Blocker Inactive",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber
                        )
                        Text(
                            text = "Tap to enable Discipline Shield in Accessibility Settings to block social apps.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isFocusActive) {
            // ================= ACTIVE FOCUS MODE UI =================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, NeonCyan, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Shield Active Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x3310B981))
                            .border(1.dp, NeonEmerald, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DISCIPLINE SHIELD ACTIVE",
                            color = NeonEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Circular Countdown Timer Display
                    val totalSecs = (totalMinutes * 60).toFloat().coerceAtLeast(1f)
                    val progress = (timeRemaining / totalSecs).coerceIn(0f, 1f)

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(200.dp)
                            .scale(pulseGlow)
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = DarkSurfaceVariant,
                            strokeWidth = 10.dp
                        )
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = NeonCyan,
                            strokeWidth = 10.dp
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val mins = timeRemaining / 60
                            val secs = timeRemaining % 60
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = "FOCUS SESSION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Desk Verified Badge with thumbnail
                    if (deskPhotoUri != null || capturedBitmap != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (capturedBitmap != null) {
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = "Desk thumbnail",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (deskPhotoUri != null) {
                                AsyncImage(
                                    model = File(deskPhotoUri!!),
                                    contentDescription = "Desk thumbnail",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = NeonEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Desk Verified",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "Workspace clean & ready",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Blocked distractions counter chip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22FF3366))
                            .border(1.dp, Color(0x44FF3366), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Distractions Blocked",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "$distractionsBlocked apps intercepted",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF85A1)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Concentration Soundscape Audio Player
                    FocusAmbientSoundCard()

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("end_focus_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exit Focus Session", fontSize = 13.sp)
                    }
                }
            }
        } else {
            // ================= PRE-FOCUS / SETUP UI =================

            // Desk Verification Card (Mandatory Requirement 2: Take desk photo via camera to activate Focus Mode)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("desk_verification_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (capturedBitmap != null) NeonEmerald else DarkCardBorder
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Desk image display: either captured bitmap or sample banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Your study desk photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.desk_sample_banner),
                                contentDescription = "Study desk banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            DarkSurface.copy(alpha = 0.85f),
                                            DarkSurface
                                        )
                                    )
                                )
                        )

                        // Status badge in top right
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (capturedBitmap != null) NeonEmerald else Color(0x99000000)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (capturedBitmap != null) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = if (capturedBitmap != null) Color.Black else TextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (capturedBitmap != null) "DESK VERIFIED" else "PHOTO REQUIRED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (capturedBitmap != null) Color.Black else TextPrimary
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Step 1: Workspace Verification",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (capturedBitmap != null)
                                "Desk photo verified! Your workspace is ready."
                            else
                                "Take a photo of your study desk to activate Focus Mode.",
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val hasCamPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasCamPermission) {
                                    cameraLauncher.launch(null)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("camera_desk_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (capturedBitmap != null) DarkSurfaceVariant else NeonCyan
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = if (capturedBitmap != null) NeonCyan else Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (capturedBitmap != null) "Retake Desk Photo" else "Snap Desk Photo via Camera",
                                color = if (capturedBitmap != null) TextPrimary else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Duration Selector Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("duration_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Step 2: Session Duration",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$selectedDuration mins",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(durationOptions) { mins ->
                            val isSelected = selectedDuration == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) NeonCyan else DarkSurfaceVariant
                                    )
                                    .clickable { selectedDuration = mins }
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$mins min",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App Blocker & Whitelist Status overview card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToBlocker() }
                    .testTag("blocker_preview_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x3306B6D4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Discipline Rules Active",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Blocks: Facebook, Instagram, YT Reels, Adult sites\nWhitelist: WhatsApp, Gmail, Phone calls",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Concentration Soundscape Audio Player (Pre-session)
            FocusAmbientSoundCard()

            Spacer(modifier = Modifier.height(24.dp))

            // CTA: Activate Focus Mode
            Button(
                onClick = {
                    if (capturedBitmap == null) {
                        // Prompt user to take desk photo first
                        showDeskCaptureHint = true
                    } else {
                        FocusSessionManager.startFocusSession(selectedDuration)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("activate_focus_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Activate Focus Mode",
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            if (showDeskCaptureHint) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Please take a desk photo above first to verify your study setup!",
                    fontSize = 12.sp,
                    color = NeonAmber,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Discipline Quote
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x15FFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "\"Discipline equals freedom.\"",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "— Jocko Willink",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }

    // Cancel Session Confirmation Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text("Exit Focus Session?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Quitting early breaks your daily focus streak. Are you sure you want to surrender your discipline?",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        FocusSessionManager.cancelFocusSession()
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3366))
                ) {
                    Text("Exit Early", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Stay Focused", color = NeonCyan)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Badge Details Dialog
    val badgeDetail = selectedBadgeForDetail
    if (badgeDetail != null) {
        BadgeDetailDialog(
            badge = badgeDetail,
            stats = currentStats,
            onDismiss = { selectedBadgeForDetail = null }
        )
    }

    // Edit Daily Study Goal Dialog
    if (showGoalDialog) {
        DailyGoalDialog(
            currentGoal = currentStats.dailyGoalMinutes,
            onSaveGoal = { newGoal ->
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    statsDao.updateDailyGoal(newGoal)
                }
            },
            onDismiss = { showGoalDialog = false }
        )
    }

    // New Badge Unlocked Celebration Dialog
    val unlockedBadge = celebratedBadge
    if (unlockedBadge != null) {
        BadgeCelebrationDialog(
            badge = unlockedBadge,
            onDismiss = { celebratedBadge = null }
        )
    }
}
