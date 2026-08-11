package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SpeakerNotes
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tools.SearchResultCard
import com.example.tools.ToolExecutionLog
import com.example.voice.TranscriptItem
import com.example.voice.VoiceDiagnostics
import com.example.voice.VoiceState
import com.example.voice.VoiceViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceScreen(
    onNavigateBack: () -> Unit,
    viewModel: VoiceViewModel = viewModel()
) {
    val context = LocalContext.current
    val voiceState by viewModel.voiceState.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val showDiagnostics by viewModel.showDiagnostics.collectAsState()
    val showTranscript by viewModel.showTranscript.collectAsState()
    val transcripts by viewModel.transcripts.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val activeSources by viewModel.activeSources.collectAsState()
    val toolExecutionLogs by viewModel.toolExecutionLogs.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startSession()
        }
    }

    LaunchedEffect(Unit) {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPerm) {
            viewModel.startSession()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A10))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Futuristic background gradient ambient glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1E1035),
                            Color(0xFF0D0E17),
                            Color(0xFF050508)
                        )
                    )
                )
        )

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    viewModel.stopSession()
                    onNavigateBack()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ZYPO LIVE VOICE",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = getStatusText(voiceState),
                    style = MaterialTheme.typography.labelSmall,
                    color = getStatusColor(voiceState)
                )
            }

            Row {
                IconButton(onClick = { viewModel.toggleTranscript() }) {
                    Icon(
                        imageVector = Icons.Default.SpeakerNotes,
                        contentDescription = "Transcript",
                        tint = if (showTranscript) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = { viewModel.toggleDiagnostics() }) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Diagnostics",
                        tint = if (showDiagnostics) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Center Content - Reactive Futuristic AI Orb
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            FuturisticAiOrb(
                voiceState = voiceState,
                audioLevel = audioLevel
            )
        }

        // Subtitle / Persona Indicator
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Assistant Persona: Kore",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = "Real-Time Native Audio Pipeline",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00E5FF).copy(alpha = 0.8f)
            )
        }

        // Bottom Controls Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF141622).copy(alpha = 0.9f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute Mic Button
                IconButton(
                    onClick = { viewModel.toggleMute() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isMuted) Color(0xFFFF3D00).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) Color(0xFFFF3D00) else Color.White
                    )
                }

                // Main Power / Mic Start-Stop Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (voiceState == VoiceState.IDLE)
                                Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF)))
                            else
                                Brush.horizontalGradient(listOf(Color(0xFFFF1744), Color(0xFFD50000)))
                        )
                        .clickable {
                            if (voiceState == VoiceState.IDLE) {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPerm) {
                                    viewModel.startSession()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            } else {
                                viewModel.stopSession()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (voiceState == VoiceState.IDLE) Icons.Default.PowerSettingsNew else Icons.Default.CallEnd,
                        contentDescription = "Toggle Session",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Speaker Toggle Button
                IconButton(
                    onClick = { viewModel.toggleSpeaker() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (!isSpeakerOn) Color(0xFFFF9100).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isSpeakerOn) Icons.Default.Speaker else Icons.Default.VolumeOff,
                        contentDescription = "Speaker",
                        tint = if (isSpeakerOn) Color.White else Color(0xFFFF9100)
                    )
                }
            }
        }

        // Live Transcript Overlay
        AnimatedVisibility(
            visible = showTranscript,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 70.dp, start = 16.dp, end = 16.dp)
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10121D).copy(alpha = 0.95f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Live Transcript",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (transcripts.isEmpty()) {
                        Text(
                            text = "Speak to see live transcript...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(transcripts) { item ->
                                Text(
                                    text = "${item.sender}: ${item.text}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (item.sender == "USER") Color.White else Color(0xFF80D8FF)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search Sources Overlay
        AnimatedVisibility(
            visible = activeSources.isNotEmpty() && !showDiagnostics,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101424).copy(alpha = 0.95f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌐 Web Sources Found (${activeSources.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { viewModel.toolRegistry.clearSources() }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(activeSources) { source ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = source.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = source.domain,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF80D8FF)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Diagnostics Overlay
        AnimatedVisibility(
            visible = showDiagnostics,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            DiagnosticsCard(
                diagnostics = diagnostics,
                logs = toolExecutionLogs,
                onClose = { viewModel.toggleDiagnostics() }
            )
        }
    }
}

@Composable
fun FuturisticAiOrb(
    voiceState: VoiceState,
    audioLevel: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val scaleLevel by animateFloatAsState(
        targetValue = 1.0f + (audioLevel * 0.8f),
        animationSpec = tween(100),
        label = "AudioLevelScale"
    )

    val orbColor = when (voiceState) {
        VoiceState.IDLE -> listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF))
        VoiceState.CONNECTING, VoiceState.RECONNECTING -> listOf(Color(0xFFFFD600), Color(0xFFFF9100))
        VoiceState.CONNECTED, VoiceState.LISTENING -> listOf(Color(0xFF00E5FF), Color(0xFF00B0FF))
        VoiceState.USER_SPEAKING -> listOf(Color(0xFF00E676), Color(0xFF00B0FF))
        VoiceState.THINKING -> listOf(Color(0xFFA000FF), Color(0xFFFF007F))
        VoiceState.AI_SPEAKING -> listOf(Color(0xFFFF007F), Color(0xFF00E5FF))
        VoiceState.INTERRUPTED -> listOf(Color(0xFFFFAB00), Color(0xFFFF3D00))
        VoiceState.ERROR -> listOf(Color(0xFFFF1744), Color(0xFFD50000))
    }

    Box(
        modifier = Modifier
            .size(240.dp)
            .scale(scaleLevel * pulseAnim),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2.8f

            // Outer reactive energy ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orbColor[0].copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = radius * 1.8f
                ),
                radius = radius * 1.6f
            )

            // Dynamic rotating particle dots
            val numDots = 12
            for (i in 0 until numDots) {
                val angle = Math.toRadians((i * (360 / numDots) + rotationAnim).toDouble())
                val dotRadius = radius * (1.2f + (audioLevel * 0.3f))
                val x = center.x + (dotRadius * cos(angle)).toFloat()
                val y = center.y + (dotRadius * sin(angle)).toFloat()
                drawCircle(
                    color = orbColor[i % 2].copy(alpha = 0.8f),
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // Inner Orb Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = orbColor,
                    center = center,
                    radius = radius
                ),
                radius = radius
            )

            // Glowing ring stroke
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center Icon Overlay
        Icon(
            imageVector = when (voiceState) {
                VoiceState.AI_SPEAKING -> Icons.Default.RecordVoiceOver
                VoiceState.USER_SPEAKING, VoiceState.LISTENING -> Icons.Default.Mic
                else -> Icons.Default.PowerSettingsNew
            },
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun DiagnosticsCard(
    diagnostics: VoiceDiagnostics,
    logs: List<ToolExecutionLog>,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 70.dp, start = 16.dp, end = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141724))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Session Diagnostics",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { onClose() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            DiagnosticRow("Microphone Status", diagnostics.micStatus)
            DiagnosticRow("Live Session State", diagnostics.sessionStatus)
            DiagnosticRow("Input Format", diagnostics.inputFormat)
            DiagnosticRow("Output Format", diagnostics.outputFormat)
            DiagnosticRow("Active Voice", diagnostics.activeVoice)
            DiagnosticRow("Tool Function", diagnostics.toolStatus)
            DiagnosticRow("Audio Queue Size", "${diagnostics.audioQueueSize} chunks")

            if (logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tool Call History (${logs.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier.height(100.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${log.toolName} (${log.executionTimeMs}ms)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (log.isSuccess) Color(0xFF00E676) else Color(0xFFFF1744),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = log.arguments,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            if (diagnostics.lastError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last Error: ${diagnostics.lastError}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF1744)
                )
            }
        }
    }
}

@Composable
fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

private fun getStatusText(state: VoiceState): String {
    return when (state) {
        VoiceState.IDLE -> "Tap button to start"
        VoiceState.CONNECTING -> "Connecting to Live Session..."
        VoiceState.CONNECTED -> "Connected"
        VoiceState.LISTENING -> "Listening..."
        VoiceState.USER_SPEAKING -> "You are speaking..."
        VoiceState.THINKING -> "Zypo is thinking..."
        VoiceState.AI_SPEAKING -> "Zypo is speaking..."
        VoiceState.INTERRUPTED -> "Interrupted"
        VoiceState.RECONNECTING -> "Reconnecting..."
        VoiceState.ERROR -> "Session error - tap to retry"
    }
}

private fun getStatusColor(state: VoiceState): Color {
    return when (state) {
        VoiceState.IDLE -> Color.White.copy(alpha = 0.6f)
        VoiceState.CONNECTING, VoiceState.RECONNECTING, VoiceState.THINKING -> Color(0xFFFFD600)
        VoiceState.CONNECTED, VoiceState.LISTENING -> Color(0xFF00E5FF)
        VoiceState.USER_SPEAKING -> Color(0xFF00E676)
        VoiceState.AI_SPEAKING -> Color(0xFFFF007F)
        VoiceState.INTERRUPTED -> Color(0xFFFFAB00)
        VoiceState.ERROR -> Color(0xFFFF1744)
    }
}
