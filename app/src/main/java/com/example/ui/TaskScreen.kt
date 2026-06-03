package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Task
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val tasksList by viewModel.allTasks.collectAsStateWithLifecycle()
    
    val activeTasks = tasksList.filter { !it.isCompleted }
    val completedTasks = tasksList.filter { it.isCompleted }
    
    // Water level is governed purely by completion ratio
    val completionPercentage = remember(tasksList) {
        if (tasksList.isEmpty()) 0f 
        else completedTasks.size.toFloat() / tasksList.size.toFloat()
    }

    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Collect notification banners
    LaunchedEffect(Unit) {
        viewModel.bannerFlow.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Capture completion splash particle indicators
    val splashTrigger = viewModel.completedSplashTrigger

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Your Flow",
                            fontWeight = FontWeight.Medium,
                            fontSize = 30.sp,
                            color = Color(0xFF1A1C1E),
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "${activeTasks.size} droplets floating • ${completedTasks.size} frozen",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF44474E).copy(alpha = 0.75f)
                        )
                    }
                },
                actions = {
                    // Evaporate All button as a neat flush mechanism
                    if (tasksList.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearAllTasks() },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cyclone,
                                contentDescription = "Evaporate All Tasks",
                                tint = Color(0xFF001D36)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFD3E4FF), // SleekPrimary
                contentColor = Color(0xFF001D36),   // SleekOnPrimary
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .size(56.dp)
                    .testTag("add_task_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Inflate new droplet",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 🌊 Background wavy water wave reservoir (rises with task completion ratio)
            WaterWaveBottom(
                completionPercentage = completionPercentage,
                splashTrigger = splashTrigger,
                modifier = Modifier.fillMaxSize(),
                waveColor = Color(0x3AA1C4FD),
                deepWaterColor = Color(0x76C2E9FB)
            )

            // 🫧 Particle canvas for bubbly complete pops
            ParticleExplosionCanvas(
                splashTrigger = splashTrigger,
                modifier = Modifier.fillMaxSize()
            )

            if (tasksList.isEmpty()) {
                // Cozy Empty view
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BubbleChart,
                            contentDescription = "Empty droplet tank",
                            tint = Color(0xFF90CAF9),
                            modifier = Modifier.size(80.dp)
                        )
                        Text(
                            text = "Your droplet tank is currently clear.",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1C1E).copy(alpha = 0.8f),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tap the plus below to condense a task.\nDouble tap bubbles to freeze them into solid ice!",
                            color = Color(0xFF44474E).copy(alpha = 0.65f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                // Floating Sandbox Arena where tasks float, bounce, and can be dragged directly
                FloatingDropletsSandbox(
                    tasks = tasksList,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Instruction mini capsule at bottom corner
            if (tasksList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.rl)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "👆 Drag to reposition • ❄️ Double tap to freeze/melt",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001D36).copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    // Modal adding form sheet
    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, priority ->
                viewModel.addTask(title, desc, priority)
                showAddDialog = false
            }
        )
    }
}

// Float helper for pixel conversions
private val Int.rl: Dp get() = this.dp

/**
 * Interactive Sandboxed Container matching physical 2D floating movement.
 * Active water droplets wander randomly with organic wavy floating phases.
 * Ice Spheres sink and slide down to near the bottom, resting heavy but floating subtly.
 */
@Composable
fun FloatingDropletsSandbox(
    tasks: List<Task>,
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val arenaWidth = constraints.maxWidth.toFloat()
        val arenaHeight = constraints.maxHeight.toFloat()
        
        // Endless loop driving dynamic fluid wave vectors (low overhead trig loop)
        val infiniteTransition = rememberInfiniteTransition(label = "fluid_waves")
        val continuousPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "drift_clock"
        )

        // Iterate through all tasks and display them at physical offsets
        tasks.forEachIndexed { index, task ->
            // Base normalized coordinates mapping [0.1f..0.9f] range of screen
            // Initialize deterministically if not adjusted yet
            val baseFracX = if (task.x == 0f) {
                0.15f + ((index * 31) % 70) / 100f
            } else task.x

            val baseFracY = if (task.y == 0f) {
                0.25f + ((index * 47) % 45) / 100f
            } else task.y

            // Continuous liquid drift calculation - distinct per task ID
            val activePhase = continuousPhase + (task.id * 1.618f)
            val waveAmplitude = 18f
            
            // Soft drift offset only applies when not completed and not actively dragging
            // Completed Ice Spheres are heavy and don't slide around matching wind currents, they sink with gravity!
            var isDragging by remember { mutableStateOf(false) }

            // Target fractional heights.
            // When frozen, it sinks to the bottom water wave reservoir dynamic vertical coordinate ~ 0.84f
            val targetFracY = if (task.isCompleted) 0.84f else baseFracY
            val animatedFracY by animateFloatAsState(
                targetValue = targetFracY,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessVeryLow
                ),
                label = "ice_gravity"
            )

            val animatedFracX by animateFloatAsState(
                targetValue = baseFracX,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "x_slide"
            )

            // Convert fractions to absolute screen pixels
            val pixelBaseX = animatedFracX * arenaWidth
            val pixelBaseY = animatedFracY * arenaHeight

            // Translate float offset
            val driftX = if (!task.isCompleted && !isDragging) sin(activePhase) * waveAmplitude else 0f
            val driftY = if (!task.isCompleted && !isDragging) cos(activePhase * 0.8f) * waveAmplitude else {
                // If ice, show slow heavy breathing bobbing
                if (!isDragging) sin(activePhase * 0.4f) * 4f else 0f
            }

            // Absolute calculated composition alignment offsets
            val finalX = (pixelBaseX + driftX).coerceIn(40f, arenaWidth - 120f)
            val finalY = (pixelBaseY + driftY).coerceIn(40f, arenaHeight - 120f)

            // Render interactive Droplet shape card
            FloatingDropletItem(
                task = task,
                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                onDelete = { viewModel.deleteTask(task) },
                modifier = Modifier
                    .offset { IntOffset(finalX.roundToInt(), finalY.roundToInt()) }
                    .graphicsLayer {
                        // Soft scaling based on high/medium visual priorities
                        val baseScale = task.sizeMultiplier
                        this.scaleX = baseScale
                        this.scaleY = baseScale
                    }
                    .pointerInput(task.id) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                // Translate drag offset into the coordinate ratio
                                val newX = (baseFracX + dragAmount.x / arenaWidth).coerceIn(0.1f, 0.9f)
                                val newY = (baseFracY + dragAmount.y / arenaHeight).coerceIn(0.15f, 0.88f)
                                viewModel.updateTaskPosition(task.id, newX, newY)
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false }
                        )
                    }
            )
        }
    }
}

/**
 * Visual representation of an active Droplet vs a frozen Ice Sphere.
 * Supports Double Tap to Freeze/Melt.
 * Supports Single Tap to toggle details and reveal close/evaporate triggers.
 */
@Composable
fun FloatingDropletItem(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Elastic pop scale animation triggered once the item enters composition
    val popScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        popScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    // Visual diameters mapped to priority levels
    val bubbleSize = when (task.priority) {
        "High" -> 130.dp
        "Medium" -> 110.dp
        "Low" -> 90.dp
        else -> 110.dp
    }

    // Slowly morph corner radius to freeze into a cube (Circle has radius = half of size)
    val maxRadius = bubbleSize / 2
    val targetRadius = if (task.isCompleted) 16.dp else maxRadius
    val cornerRadius by animateDpAsState(
        targetValue = targetRadius,
        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
        label = "cubeCornerMorph"
    )
    val customShape = RoundedCornerShape(cornerRadius)

    // Ice blue flat solid colors for active states (no gradients)
    val activeColor = when (task.priority) {
        "High" -> Color(0xFF80D8FF) // Ice blue accent
        "Medium" -> Color(0xFFB3E5FC) // Light ice blue
        else -> Color(0xFFE1F5FE) // Softest ice blue
    }

    // Ice cubes must be of a darker shade of flat blue (no gradients)
    val iceColor = Color(0xFF1565C0) // Flat beautiful dark blue

    val activeOnColor = Color(0xFF001D36) // Deep navy for pristine contrast against flat ice blue
    val iceOnColor = Color(0xFFFFFFFF) // White contrast for dark blue cube

    // Slowly animate background paint color transition to simulate solid freezing
    val animatedBackgroundPaint by animateColorAsState(
        targetValue = if (task.isCompleted) iceColor else activeColor,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "iceBackgroundFreeze"
    )

    // Slowly animate primary contrast foreground color
    val animatedForegroundPaint by animateColorAsState(
        targetValue = if (task.isCompleted) iceOnColor else activeOnColor,
        animationSpec = tween(durationMillis = 1500),
        label = "iceForegroundFreeze"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                // Apply elastic bubble creation pop scale
                scaleX = popScale.value
                scaleY = popScale.value
            }
            .size(bubbleSize)
            .shadow(
                elevation = 4.dp,
                shape = customShape
            )
            .clip(customShape)
            .background(animatedBackgroundPaint)
            .pointerInput(task.id) {
                // Intercept custom tactile taps
                detectTapGestures(
                    onDoubleTap = {
                        onToggleComplete()
                    },
                    onTap = {
                        isExpanded = !isExpanded
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {

        // Inner item contents
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isExpanded) {
                // Small expanded quick control overlay
                Text(
                    text = if (task.description.isEmpty()) "No info" else task.description,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = animatedForegroundPaint,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(6.rl))
                
                // Evaporate button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Evaporate Droplet",
                        tint = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                // Standard mode
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.AcUnit else Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = if (task.isCompleted) Color(0xFFE0F7FA) else animatedForegroundPaint.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.height(4.rl))

                // Title centering
                Text(
                    text = task.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = animatedForegroundPaint,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(2.rl))

                Text(
                    text = if (task.isCompleted) "FROZEN ❄️" else task.priority,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = animatedForegroundPaint.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Dialog input form
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("Medium") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Water,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Condense Droplet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    placeholder = { Text("e.g. Morning routine") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Optional extra details...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Priority Level (Droplet Size)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("High", "Medium", "Low").forEach { priority ->
                            val isSelected = selectedPriority == priority
                            val pColor = when (priority) {
                                "High" -> Color(0xFF1E88E5)
                                "Medium" -> Color(0xFF00897B)
                                else -> Color(0xFF7CB342) // gentle green
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) pColor else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedPriority = priority }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = priority,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Evaporate")
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title, description, selectedPriority)
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("submit_task_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Condense", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
