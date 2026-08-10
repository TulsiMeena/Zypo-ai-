package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet

@Composable
fun ZypoLogo(
    modifier: Modifier = Modifier,
    symbolSize: Dp = 40.dp,
    showWordmark: Boolean = true,
    animated: Boolean = false
) {
    val rotation = remember { Animatable(0f) }

    if (animated) {
        LaunchedEffect(Unit) {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(12000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    Row(
        modifier = modifier.testTag("zypo_logo_component"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Abstract Futuristic Zypo Symbol Canvas
        Box(
            modifier = Modifier.size(symbolSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(symbolSize)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // Outer ambient glow ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricCyan.copy(alpha = 0.35f),
                            NeonViolet.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius
                )

                // Outer geometric orbital arc
                val path = Path().apply {
                    moveTo(center.x, center.y - radius * 0.75f)
                    cubicTo(
                        center.x + radius * 0.8f, center.y - radius * 0.5f,
                        center.x + radius * 0.8f, center.y + radius * 0.5f,
                        center.x, center.y + radius * 0.75f
                    )
                    cubicTo(
                        center.x - radius * 0.8f, center.y + radius * 0.5f,
                        center.x - radius * 0.8f, center.y - radius * 0.5f,
                        center.x, center.y - radius * 0.75f
                    )
                }

                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(ElectricCyan, NeonViolet, Color(0xFFFF007F))
                    ),
                    style = Stroke(width = radius * 0.12f)
                )

                // Inner geometric starburst / diamond core
                val corePath = Path().apply {
                    moveTo(center.x, center.y - radius * 0.45f)
                    cubicTo(center.x, center.y, center.x, center.y, center.x + radius * 0.45f, center.y)
                    cubicTo(center.x, center.y, center.x, center.y, center.x, center.y + radius * 0.45f)
                    cubicTo(center.x, center.y, center.x, center.y, center.x - radius * 0.45f, center.y)
                    cubicTo(center.x, center.y, center.x, center.y, center.x, center.y - radius * 0.45f)
                }

                drawPath(
                    path = corePath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, ElectricCyan)
                    )
                )
            }
        }

        if (showWordmark) {
            Spacer(modifier = Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Zypo",
                    fontSize = (symbolSize.value * 0.55f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(ElectricCyan, NeonViolet)
                            )
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AI",
                        fontSize = (symbolSize.value * 0.38f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}
