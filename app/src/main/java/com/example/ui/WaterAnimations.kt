package com.example.ui

import android.graphics.PointF
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.sin
import kotlin.random.Random

// Represents a water bubble/droplet particle in the visual explosion
data class SplashParticle(
    val id: Int,
    val origin: Offset,
    var currentPos: Offset,
    val velocity: Offset,
    val radius: Float,
    val color: Color,
    var alpha: Float,
    val maxLifetime: Int,
    var age: Int = 0
)

@Composable
fun WaterWaveBottom(
    completionPercentage: Float, // 0.0f to 1.0f
    splashTrigger: SharedFlow<Unit>,
    modifier: Modifier = Modifier,
    waveColor: Color = Color(0x762196F3),
    deepWaterColor: Color = Color(0xBA0D47A1)
) {
    // Phase animators for wave horizontal movement
    val infiniteTransition = rememberInfiniteTransition(label = "wave_animation")
    val phaseOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )
    val phaseOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    // Animated splash amplitude
    val splashAmplitude = remember { Animatable(0f) }
    
    LaunchedEffect(splashTrigger) {
        splashTrigger.collectLatest {
            // Initiate a highly ripple dynamic splash on completion
            splashAmplitude.animateTo(
                targetValue = 35f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            splashAmplitude.animateTo(
                targetValue = 0f,
                animationSpec = tween(1500, easing = FastOutSlowInEasing)
            )
        }
    }

    // Smoothly animate the target water level height percentage
    val animatedProgress by animateFloatAsState(
        targetValue = completionPercentage,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "water_height"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Calculate exact baseline level
            // Ground-zero (empty list) = starting at 12% baseline height at bottom
            // Max completed (100% list completion) = covers 42% height of the screen
            val minHeightFrac = 0.12f
            val maxHeightFrac = 0.42f
            val currentFraction = minHeightFrac + (animatedProgress * (maxHeightFrac - minHeightFrac))
            val baseWaterY = height * (1f - currentFraction)

            // Define Waves using sinusoidal functions
            val path1 = Path()
            val path2 = Path()

            path1.moveTo(0f, height)
            path2.moveTo(0f, height)

            val baseAmplitude = 12f // standard wave height in pixels
            val activeAmplitude = baseAmplitude + splashAmplitude.value

            val waveLength1 = width * 1.1f
            val waveLength2 = width * 0.8f

            // Construct progressive wave coordinates
            for (x in 0..width.toInt() step 5) {
                // Wave 1 formula
                val xFactor1 = (2f * Math.PI.toFloat() * x / waveLength1) + phaseOffset1
                val yOffset1 = sin(xFactor1) * activeAmplitude
                path1.lineTo(x.toFloat(), baseWaterY + yOffset1)

                // Wave 2 formula (cross-frequency layer for organic fluid density visualizer)
                val xFactor2 = (2f * Math.PI.toFloat() * x / waveLength2) + phaseOffset2
                val yOffset2 = sin(xFactor2) * (activeAmplitude * 0.75f)
                path2.lineTo(x.toFloat(), baseWaterY + 5f + yOffset2)
            }

            path1.lineTo(width, height)
            path1.close()

            path2.lineTo(width, height)
            path2.close()

            // Clip & draw deep water backgrounds
            drawPath(path2, color = waveColor)
            drawPath(path1, color = deepWaterColor)

            // Dynamic float reflection bubbles rising inside the water
            val waveBottomCount = 3
            val randomGenerator = Random(42)
            for (i in 1..waveBottomCount) {
                val radius = 8f + randomGenerator.nextFloat() * 10f
                val xOffset = (width * 0.25f * i) + (sin(phaseOffset1 + i) * 20f)
                val yOffset = baseWaterY + 40f + (i * 30f)
                if (yOffset < height) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f),
                        radius = radius,
                        center = Offset(xOffset, yOffset)
                    )
                }
            }
        }
    }
}

@Composable
fun ParticleExplosionCanvas(
    splashTrigger: SharedFlow<Unit>,
    modifier: Modifier = Modifier
) {
    val particles = remember { mutableStateListOf<SplashParticle>() }
    var particleIdCounter by remember { mutableIntStateOf(0) }

    // Particle lifecycle animator loop
    LaunchedEffect(Unit) {
        while (true) {
            if (particles.isNotEmpty()) {
                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.age += 1
                    if (p.age >= p.maxLifetime) {
                        iterator.remove()
                    } else {
                        // Apply movement dynamics (gravity/buoyancy + friction retardation)
                        p.currentPos = Offset(
                            p.currentPos.x + p.velocity.x,
                            p.currentPos.y + p.velocity.y
                        )
                        // Decrease opacity as it gets older
                        p.alpha = 1.0f - (p.age.toFloat() / p.maxLifetime)
                    }
                }
            }
            delay(16) // ~60fps layout loop
        }
    }

    // Capture complete triggers to generate bubbles
    LaunchedEffect(splashTrigger) {
        splashTrigger.collectLatest {
            // Generate a burst of 18 colorful water splash particles
            val random = Random(System.nanoTime())
            // Pick a randomized visual bursting center in lower center where popping happens
            val midX = 500f
            val midY = 1100f
            
            repeat(16) {
                val angle = random.nextDouble() * 2.0 * Math.PI
                val speed = 6f + random.nextDouble() * 12f
                val velX = (Math.cos(angle) * speed).toFloat()
                val velY = (Math.sin(angle) * speed - 5.0f).toFloat() // upward bias splash
                val size = 8f + random.nextFloat() * 16f
                val maxAge = 35 + random.nextInt(20)

                // Alternate hues between light water blue, teal and dynamic tint
                val bubbleColor = when(random.nextInt(3)) {
                    0 -> Color(0xFF81D4FA)
                    1 -> Color(0xFF4DD0E1)
                    else -> Color(0xFFE0F7FA)
                }

                particles.add(
                    SplashParticle(
                        id = particleIdCounter++,
                        origin = Offset(midX, midY),
                        currentPos = Offset(midX + (random.nextFloat() * 300f - 150f), midY - random.nextFloat() * 150f),
                        velocity = Offset(velX, velY),
                        radius = size,
                        color = bubbleColor,
                        alpha = 1.0f,
                        maxLifetime = maxAge
                    )
                )
            }
        }
    }

    Canvas(modifier = modifier) {
        // Redraw all live active particles
        particles.forEach { p ->
            drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.radius,
                center = p.currentPos
            )
            // Add miniature visual white sheen to look spherical & glassy
            drawCircle(
                color = Color.White.copy(alpha = p.alpha * 0.7f),
                radius = p.radius * 0.3f,
                center = Offset(p.currentPos.x - p.radius * 0.3f, p.currentPos.y - p.radius * 0.3f)
            )
        }
    }
}
