package com.example.alarm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.DisciplineApp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DisciplineTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceAlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn screen on and display over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContent {
            DisciplineTheme {
                VoiceAlarmScreen(
                    onDismissed = {
                        VoiceAlarmService.stop(this)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun VoiceAlarmScreen(
    onDismissed: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Listening for voice verification...") }
    var isDismissedSuccessfully by remember { mutableStateOf(false) }
    var showManualBackup by remember { mutableStateOf(false) }
    var manualTypedPhrase by remember { mutableStateOf("") }
    var manualPhraseError by remember { mutableStateOf(false) }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val hasAudioPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission.value = isGranted
        if (isGranted) {
            startListeningInternal(
                context = context,
                onListeningChange = { isListening = it },
                onTextRecognized = { text ->
                    recognizedText = text
                    checkPhraseMatch(text) {
                        isDismissedSuccessfully = true
                        coroutineScope.launch {
                            DisciplineApp.instance.database.disciplineStatsDao().incrementVoiceAlarmsCleared()
                            delay(1800)
                            onDismissed()
                        }
                    }
                },
                onStatusChange = { statusMessage = it },
                setRecognizer = { speechRecognizer = it }
            )
        } else {
            statusMessage = "Mic permission denied. Use emergency dismissal below."
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission.value) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startListeningInternal(
                context = context,
                onListeningChange = { isListening = it },
                onTextRecognized = { text ->
                    recognizedText = text
                    checkPhraseMatch(text) {
                        isDismissedSuccessfully = true
                        coroutineScope.launch {
                            DisciplineApp.instance.database.disciplineStatsDao().incrementVoiceAlarmsCleared()
                            delay(1800)
                            onDismissed()
                        }
                    }
                },
                onStatusChange = { statusMessage = it },
                setRecognizer = { speechRecognizer = it }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    // Pulsing animation for active alarm & mic
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alarmColor by animateColorAsState(
        targetValue = if (isDismissedSuccessfully) NeonEmerald else Color(0xFFFF3366),
        label = "color"
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("voice_alarm_screen"),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Warning Chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x33FF3366))
                    .border(1.dp, Color(0x66FF3366), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = null,
                    tint = Color(0xFFFF3366),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CONTINUOUS VIBRATION ACTIVE",
                    color = Color(0xFFFF85A1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Pulsing Alarm / Mic Visualizer Orb
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .scale(if (isDismissedSuccessfully) 1f else pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                alarmColor.copy(alpha = 0.4f),
                                alarmColor.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(2.dp, alarmColor, CircleShape)
            ) {
                if (isDismissedSuccessfully) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = NeonEmerald,
                        modifier = Modifier.size(72.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.NotificationsActive,
                        contentDescription = "Active Alarm",
                        tint = alarmColor,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Wake Up Title
            Text(
                text = if (isDismissedSuccessfully) "DISCIPLINE VERIFIED!" else "WAKE UP, SOLDIER!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = if (isDismissedSuccessfully) NeonEmerald else TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isDismissedSuccessfully)
                    "Alarm dismissed! Your wake-up streak has increased."
                else
                    "Speak the exact verification phrase to stop vibration:",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Required Phrase Display Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phrase_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = if (isDismissedSuccessfully) NeonEmerald else NeonCyan
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SPEAK CLEARLY:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "\"Ami ghum theke uthechi\"",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "(আমি ঘুম থেকে উঠেছি)",
                        fontSize = 16.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live Speech Detection Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isListening) NeonEmerald else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusMessage,
                            fontSize = 12.sp,
                            color = if (isListening) NeonEmerald else TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        if (recognizedText.isNotEmpty()) {
                            Text(
                                text = "Heard: \"$recognizedText\"",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Retry Listening Button if not listening
            if (!isListening && !isDismissedSuccessfully) {
                Button(
                    onClick = {
                        startListeningInternal(
                            context = context,
                            onListeningChange = { isListening = it },
                            onTextRecognized = { text ->
                                recognizedText = text
                                checkPhraseMatch(text) {
                                    isDismissedSuccessfully = true
                                    coroutineScope.launch {
                                        DisciplineApp.instance.database.disciplineStatsDao().incrementVoiceAlarmsCleared()
                                        delay(1800)
                                        onDismissed()
                                    }
                                }
                            },
                            onStatusChange = { statusMessage = it },
                            setRecognizer = { speechRecognizer = it }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("retry_listen_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tap to Speak Phrase",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Emergency Manual Verification Toggle (safety feature so user is never trapped)
            AnimatedVisibility(
                visible = !isDismissedSuccessfully,
                enter = fadeIn() + scaleIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        onClick = { showManualBackup = !showManualBackup },
                        modifier = Modifier.testTag("emergency_backup_toggle"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (showManualBackup) "Hide Backup" else "Emergency Manual Dismissal",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    if (showManualBackup) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Type the phrase manually to prove wakefulness:",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = manualTypedPhrase,
                                    onValueChange = {
                                        manualTypedPhrase = it
                                        manualPhraseError = false
                                    },
                                    placeholder = { Text("Ami ghum theke uthechi", color = TextMuted) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("manual_phrase_input"),
                                    isError = manualPhraseError,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = DarkSurfaceVariant,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        if (manualTypedPhrase.trim().equals("Ami ghum theke uthechi", ignoreCase = true) ||
                                            manualTypedPhrase.trim() == "আমি ঘুম থেকে উঠেছি"
                                        ) {
                                            isDismissedSuccessfully = true
                                            coroutineScope.launch {
                                                DisciplineApp.instance.database.disciplineStatsDao().incrementVoiceAlarmsCleared()
                                                delay(1200)
                                                onDismissed()
                                            }
                                        } else {
                                            manualPhraseError = true
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("manual_phrase_submit"),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Verify & Stop Vibration", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun startListeningInternal(
    context: android.content.Context,
    onListeningChange: (Boolean) -> Unit,
    onTextRecognized: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    setRecognizer: (SpeechRecognizer?) -> Unit
) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        onStatusChange("Speech recognizer unavailable on this device. Use manual dismissal below.")
        onListeningChange(false)
        return
    }

    try {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        setRecognizer(recognizer)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningChange(true)
                onStatusChange("Listening... Say: 'Ami ghum theke uthechi'")
            }

            override fun onBeginningOfSpeech() {
                onStatusChange("Detecting voice...")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                onListeningChange(false)
                onStatusChange("Analyzing speech...")
            }

            override fun onError(error: Int) {
                onListeningChange(false)
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match detected. Try again clearly."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out. Tap button to retry."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                    else -> "Microphone ready. Tap to speak phrase."
                }
                onStatusChange(msg)
            }

            override fun onResults(results: Bundle?) {
                onListeningChange(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                onTextRecognized(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotEmpty()) {
                    onTextRecognized(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    } catch (e: Exception) {
        onStatusChange("Speech error: ${e.message}. Use emergency dismissal.")
        onListeningChange(false)
    }
}

private fun checkPhraseMatch(recognized: String, onSuccess: () -> Unit) {
    val clean = recognized.lowercase(Locale.ROOT).trim()

    // Variations in English transliteration & Bengali script
    val isMatch = clean.contains("ami ghum theke uthechi") ||
            clean.contains("ami ghum theke utheci") ||
            clean.contains("ami ghum theke uthchi") ||
            clean.contains("ami ghum theke") ||
            clean.contains("ghum theke uthechi") ||
            clean.contains("ghum theke utheci") ||
            clean.contains("আমি ঘুম থেকে উঠেছি") ||
            clean.contains("ঘুম থেকে উঠেছি") ||
            (clean.contains("ghum") && clean.contains("uthe"))

    if (isMatch) {
        onSuccess()
    }
}
