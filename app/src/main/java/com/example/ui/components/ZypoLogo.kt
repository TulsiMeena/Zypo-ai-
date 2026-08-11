package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
    symbolSize: Dp = 60.dp,
    showWordmark: Boolean = true,
    showSubtitle: Boolean = false,
    animated: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")

    // Pulsing scale for animated mode
    val scalePulse by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        androidx.compose.runtime.mutableStateOf(1f)
    }

    // Glowing ring rotation angle
    val rotationAngle by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(10000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
    } else {
        androidx.compose.runtime.mutableStateOf(0f)
    }

    Column(
        modifier = modifier
            .testTag("zypo_logo_component")
            .scale(scalePulse),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo Circular Emblem Canvas
        Box(
            modifier = Modifier.size(symbolSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(symbolSize)) {
                val w = size.width
                val h = size.height
                val center = Offset(w / 2f, h / 2f)
                val radius = w * 0.46f

                // 1. Outer ambient radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00B0FF).copy(alpha = 0.45f),
                            Color(0xFF0052FF).copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = w * 0.65f
                    ),
                    radius = w * 0.65f
                )

                // 2. Outer circular ring arc
                rotate(degrees = rotationAngle, pivot = center) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF00E5FF),
                                Color(0xFF0052FF),
                                Color(0xFF001133),
                                Color(0xFF00E5FF)
                            ),
                            center = center
                        ),
                        radius = radius,
                        style = Stroke(width = w * 0.055f)
                    )
                }

                // 3. Stylized 'Z' Ribbon (Black & Blue 3D Gradient)
                val zPath = Path().apply {
                    // Top Bar of Z
                    moveTo(w * 0.24f, h * 0.26f)
                    lineTo(w * 0.68f, h * 0.26f)
                    cubicTo(w * 0.74f, h * 0.26f, w * 0.76f, h * 0.30f, w * 0.70f, h * 0.36f)
                    // Diagonal downwards
                    lineTo(w * 0.32f, h * 0.68f)
                    // Bottom Bar of Z
                    lineTo(w * 0.72f, h * 0.68f)
                    cubicTo(w * 0.76f, h * 0.68f, w * 0.77f, h * 0.74f, w * 0.72f, h * 0.74f)
                    lineTo(w * 0.22f, h * 0.74f)
                    cubicTo(w * 0.18f, h * 0.74f, w * 0.18f, h * 0.68f, w * 0.22f, h * 0.64f)
                    lineTo(w * 0.58f, h * 0.32f)
                    lineTo(w * 0.24f, h * 0.32f)
                    cubicTo(w * 0.20f, h * 0.32f, w * 0.20f, h * 0.26f, w * 0.24f, h * 0.26f)
                    close()
                }

                drawPath(
                    path = zPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0D1B2A),
                            Color(0xFF0044FF),
                            Color(0xFF00C8FF)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )

                // 4. Upward Stock Growth Trend Line + Arrow Tip
                val trendPath = Path().apply {
                    moveTo(w * 0.44f, h * 0.58f)
                    lineTo(w * 0.56f, h * 0.46f)
                    lineTo(w * 0.64f, h * 0.50f)
                    lineTo(w * 0.82f, h * 0.28f)
                }

                drawPath(
                    path = trendPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0072FF), Color(0xFF00E5FF))
                    ),
                    style = Stroke(
                        width = w * 0.065f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Arrow head on trend line
                val arrowHead = Path().apply {
                    moveTo(w * 0.82f, h * 0.28f)
                    lineTo(w * 0.70f, h * 0.28f)
                    lineTo(w * 0.82f, h * 0.40f)
                    close()
                }
                drawPath(
                    path = arrowHead,
                    color = Color(0xFF00E5FF)
                )

                // 5. Candlestick Chart Bars (Green & Red Market Bars)
                // Bar 1: Green
                drawRect(
                    color = Color(0xFF00E676),
                    topLeft = Offset(w * 0.58f, h * 0.56f),
                    size = Size(w * 0.035f, h * 0.08f)
                )
                drawLine(
                    color = Color(0xFF00E676),
                    start = Offset(w * 0.597f, h * 0.53f),
                    end = Offset(w * 0.597f, h * 0.67f),
                    strokeWidth = w * 0.012f
                )

                // Bar 2: Red
                drawRect(
                    color = Color(0xFFFF3D00),
                    topLeft = Offset(w * 0.64f, h * 0.53f),
                    size = Size(w * 0.035f, h * 0.09f)
                )
                drawLine(
                    color = Color(0xFFFF3D00),
                    start = Offset(w * 0.657f, h * 0.50f),
                    end = Offset(w * 0.657f, h * 0.64f),
                    strokeWidth = w * 0.012f
                )

                // Bar 3: Green
                drawRect(
                    color = Color(0xFF00E676),
                    topLeft = Offset(w * 0.70f, h * 0.46f),
                    size = Size(w * 0.035f, h * 0.11f)
                )
                drawLine(
                    color = Color(0xFF00E676),
                    start = Offset(w * 0.717f, h * 0.42f),
                    end = Offset(w * 0.717f, h * 0.60f),
                    strokeWidth = w * 0.012f
                )

                // Bar 4: Red
                drawRect(
                    color = Color(0xFFFF3D00),
                    topLeft = Offset(w * 0.76f, h * 0.44f),
                    size = Size(w * 0.035f, h * 0.08f)
                )
                drawLine(
                    color = Color(0xFFFF3D00),
                    start = Offset(w * 0.777f, h * 0.40f),
                    end = Offset(w * 0.777f, h * 0.54f),
                    strokeWidth = w * 0.012f
                )
            }
        }

        if (showWordmark) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Zypo",
                    fontSize = (symbolSize.value * 0.48f).sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0066FF), Color(0xFF00E5FF))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AI",
                        fontSize = (symbolSize.value * 0.36f).sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            if (showSubtitle) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "BEST TRADING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF),
                    letterSpacing = 2.sp
                )
                Text(
                    text = "TRADE • ANALYZE • GROW",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

