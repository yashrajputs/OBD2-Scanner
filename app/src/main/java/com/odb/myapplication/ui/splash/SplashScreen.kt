package com.odb.myapplication.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = EaseOutBack
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "textAlpha"
    )

    // Circuit animation
    val circuitAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1200,
            delayMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "circuitAlpha"
    )

    // Subtle pulse animation
    val pulse by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.95f,
        animationSpec = repeatable(
            iterations = 100_000, // A very large number to simulate infinite
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500L)
        onSplashComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Container with Shield Shape
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale * pulse)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                // Shield/Logo Shape - Inverted Pentagon
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    // Circuit Board Pattern
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Top connector/handle
                        Box(
                            modifier = Modifier
                                .size(50.dp, 16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF3B82F6))
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Main shield body - inverted pentagon
                        Box(
                            modifier = Modifier
                                .size(140.dp, 120.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Circuit lines and nodes
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Circuit pattern with lines
                                Row(
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Left circuit with connecting lines
                                    Column(
                                        verticalArrangement = Arrangement.SpaceEvenly,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        repeat(3) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(Color(0xFF3B82F6))
                                                    .alpha(circuitAlpha)
                                            )
                                            if (it < 2) Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }

                                    // Center text
                                    Text(
                                        text = "MYAB",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3B82F6),
                                        textAlign = TextAlign.Center
                                    )

                                    // Right circuit with connecting lines
                                    Column(
                                        verticalArrangement = Arrangement.SpaceEvenly,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        repeat(3) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(Color(0xFF3B82F6))
                                                    .alpha(circuitAlpha)
                                            )
                                            if (it < 2) Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Bottom center node
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF3B82F6))
                                        .alpha(circuitAlpha)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // App Title
            Text(
                text = "OBD2 Scanner",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = "Professional Vehicle Diagnostics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier
                    .size(40.dp)
                    .alpha(alpha),
                color = Color(0xFF3B82F6),
                strokeWidth = 4.dp
            )
        }
    }
}
