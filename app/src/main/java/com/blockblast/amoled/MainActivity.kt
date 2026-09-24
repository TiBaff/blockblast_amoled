package com.blockblast.amoled

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

val PlacedBlockColor = Color(0xFF4C4C4C)   // Darker gray for blocks already on the board
val ActiveBlockColor = Color(0xFF8E8E8E)   // Lighter gray for candidate and dragged blocks
val GlowColor = Color(0xFFEEEEEE)          // Crisp white glow for lines about to clear

enum class AppScreen {
    MENU,
    GAME
}

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val alpha: Float
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlockBlastTheme {
                val context = LocalContext.current
                val viewModel = remember { GameViewModel(context) }
                var currentScreen by remember { mutableStateOf(AppScreen.MENU) }

                when (currentScreen) {
                    AppScreen.MENU -> {
                        MainMenuScreen(
                            onStartClassic = {
                                viewModel.startNewGame()
                                currentScreen = AppScreen.GAME
                            }
                        )
                    }
                    AppScreen.GAME -> {
                        GameScreen(
                            viewModel = viewModel,
                            onReturnToMenu = {
                                currentScreen = AppScreen.MENU
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainMenuScreen(onStartClassic: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "BLOCK\nBLAST",
                color = Color.White,
                fontSize = 58.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSans,
                textAlign = TextAlign.Center,
                lineHeight = 64.sp
            )

            Spacer(modifier = Modifier.height(130.dp))

            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(ButtonDarkGray)
                    .clickable { onStartClassic() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "∞",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSans
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Classic",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = GoogleSans
                    )
                }
            }
        }
    }
}

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onReturnToMenu: () -> Unit
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    var draggedSlotIndex by remember { mutableStateOf<Int?>(null) }
    var dragGlobalPosition by remember { mutableStateOf(Offset.Zero) }
    var boardBounds by remember { mutableStateOf(Rect.Zero) }

    // Particle explosion state
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var clearMessage by remember { mutableStateOf<String?>(null) }

    // LaunchedEffect loop for particle animation
    LaunchedEffect(particles.isNotEmpty()) {
        while (particles.isNotEmpty()) {
            delay(16)
            particles = particles.mapNotNull { p ->
                val nextAlpha = p.alpha - 0.045f
                if (nextAlpha <= 0f) null
                else p.copy(
                    x = p.x + p.vx,
                    y = p.y + p.vy,
                    alpha = nextAlpha
                )
            }
        }
    }

    val density = LocalDensity.current
    val fingerLiftPx = with(density) { 85.dp.toPx() }

    // Calculate hover coordinate
    val hoveredCoord: Pair<Int, Int>? = remember(draggedSlotIndex, dragGlobalPosition, boardBounds) {
        val slot = draggedSlotIndex ?: return@remember null
        val shape = viewModel.availableShapes.getOrNull(slot) ?: return@remember null
        if (boardBounds.width <= 0f || boardBounds.height <= 0f) return@remember null

        val cellWidth = boardBounds.width / 8f
        val cellHeight = boardBounds.height / 8f

        val pieceCenterX = dragGlobalPosition.x
        val pieceCenterY = dragGlobalPosition.y - fingerLiftPx

        val startCol = ((pieceCenterX - boardBounds.left) / cellWidth - shape.width / 2f).roundToInt()
        val startRow = ((pieceCenterY - boardBounds.top) / cellHeight - shape.height / 2f).roundToInt()

        if (viewModel.canPlace(shape, startRow, startCol)) {
            startRow to startCol
        } else {
            null
        }
    }

    // Determine which lines will be cleared upon placement (Pre-clear Glowing Lines)
    val glowingLines: Pair<Set<Int>, Set<Int>> = remember(hoveredCoord, draggedSlotIndex) {
        val coord = hoveredCoord ?: return@remember emptySet<Int>() to emptySet<Int>()
        val slot = draggedSlotIndex ?: return@remember emptySet<Int>() to emptySet<Int>()
        val shape = viewModel.availableShapes.getOrNull(slot) ?: return@remember emptySet<Int>() to emptySet<Int>()

        val simBoard = Array(8) { r -> viewModel.board[r].clone() }
        for ((r, c) in shape.cells) {
            simBoard[coord.first + r][coord.second + c] = true
        }

        val rows = mutableSetOf<Int>()
        for (r in 0..7) {
            if (simBoard[r].all { it }) rows.add(r)
        }

        val cols = mutableSetOf<Int>()
        for (c in 0..7) {
            var allCol = true
            for (r in 0..7) {
                if (!simBoard[r][c]) {
                    allCol = false
                    break
                }
            }
            if (allCol) cols.add(c)
        }

        rows to cols
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_crown),
                        contentDescription = "Best Score Crown",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${viewModel.bestScore}",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSans
                    )
                }

                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Score and Combo Display Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Animated Combo Header right above the score
                Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = viewModel.combo > 0,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Text(
                            text = "Combo: ${viewModel.combo}",
                            color = Color(0xFFE0E0E0),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GoogleSans,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Current Score
                Text(
                    text = "${viewModel.score}",
                    color = Color.White,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSans,
                    textAlign = TextAlign.Center
                )
            }

            // 8x8 Game Board with Pre-clear Line Glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .onGloballyPositioned { coordinates ->
                        boardBounds = coordinates.boundsInRoot()
                    }
                    .border(1.dp, GridBorderColor)
                    .background(AmoledBlack)
            ) {
                GameBoardGrid(
                    board = viewModel.board,
                    hoverCoord = hoveredCoord,
                    hoverShape = draggedSlotIndex?.let { viewModel.availableShapes.getOrNull(it) },
                    glowingRows = glowingLines.first,
                    glowingCols = glowingLines.second
                )

                // Particle explosion canvas overlay
                if (particles.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        particles.forEach { p ->
                            drawCircle(
                                color = p.color.copy(alpha = p.alpha),
                                radius = p.size,
                                center = Offset(p.x, p.y)
                            )
                        }
                    }
                }

                // Temporary Combo pop-up over board
                if (clearMessage != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = clearMessage ?: "",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GoogleSans
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom 3 shapes slot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..2) {
                    val shape = viewModel.availableShapes.getOrNull(i)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (shape != null) {
                            ShapeItemView(
                                shape = shape,
                                isBeingDragged = (draggedSlotIndex == i),
                                onDragStart = { startPos ->
                                    draggedSlotIndex = i
                                    dragGlobalPosition = startPos
                                },
                                onDrag = { dragDelta ->
                                    dragGlobalPosition += dragDelta
                                },
                                onDragEnd = {
                                    val target = hoveredCoord
                                    if (target != null) {
                                        val (r, c) = target
                                        val rowsToClear = glowingLines.first
                                        val colsToClear = glowingLines.second

                                        val placed = viewModel.placeShape(i, r, c)
                                        if (placed && (rowsToClear.isNotEmpty() || colsToClear.isNotEmpty())) {
                                            // Spawn particles along the cleared cells
                                            val cellW = boardBounds.width / 8f
                                            val cellH = boardBounds.height / 8f
                                            val newParticles = mutableListOf<Particle>()

                                            for (row in rowsToClear) {
                                                for (col in 0..7) {
                                                    val cx = col * cellW + cellW / 2f
                                                    val cy = row * cellH + cellH / 2f
                                                    for (k in 0..5) {
                                                        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                                                        val speed = Random.nextFloat() * 7f + 2f
                                                        newParticles.add(
                                                            Particle(
                                                                x = cx,
                                                                y = cy,
                                                                vx = cos(angle) * speed,
                                                                vy = sin(angle) * speed,
                                                                color = if (k % 2 == 0) Color.White else Color(0xFFB0B0B0),
                                                                size = Random.nextFloat() * 4f + 3f,
                                                                alpha = 1f
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            for (col in colsToClear) {
                                                for (row in 0..7) {
                                                    val cx = col * cellW + cellW / 2f
                                                    val cy = row * cellH + cellH / 2f
                                                    for (k in 0..5) {
                                                        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                                                        val speed = Random.nextFloat() * 7f + 2f
                                                        newParticles.add(
                                                            Particle(
                                                                x = cx,
                                                                y = cy,
                                                                vx = cos(angle) * speed,
                                                                vy = sin(angle) * speed,
                                                                color = if (k % 2 == 0) Color.White else Color(0xFFB0B0B0),
                                                                size = Random.nextFloat() * 4f + 3f,
                                                                alpha = 1f
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            particles = newParticles
                                        }
                                    }
                                    draggedSlotIndex = null
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Shape Overlay during Drag (smooth 1:1 follow with 85dp lift)
        if (draggedSlotIndex != null) {
            val shape = viewModel.availableShapes.getOrNull(draggedSlotIndex!!)
            if (shape != null && boardBounds.width > 0f) {
                val boardCellWidth = boardBounds.width / 8f
                val boardCellHeight = boardBounds.height / 8f
                val boardCellDp = with(density) { boardCellWidth.toDp() }

                val shapeWidthPx = shape.width * boardCellWidth
                val shapeHeightPx = shape.height * boardCellHeight

                val floatLeftPx = dragGlobalPosition.x - shapeWidthPx / 2f
                val floatTopPx = (dragGlobalPosition.y - fingerLiftPx) - shapeHeightPx / 2f

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = floatLeftPx.roundToInt(),
                                y = floatTopPx.roundToInt()
                            )
                        }
                ) {
                    ShapePreview(
                        shape = shape,
                        cellSize = boardCellDp,
                        padding = 2.dp,
                        blockColor = ActiveBlockColor,
                        cornerRadiusFactor = 0.22f
                    )
                }
            }
        }

        // Settings Dialog
        if (showSettingsDialog) {
            Dialog(onDismissRequest = { showSettingsDialog = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = AmoledBlack),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Настройки",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GoogleSans
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                showSettingsDialog = false
                                onReturnToMenu()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonDarkGray),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Text(
                                text = "Главное меню",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = GoogleSans
                            )
                        }
                    }
                }
            }
        }

        // Game Over Dialog
        if (viewModel.isGameOver) {
            Dialog(onDismissRequest = {}) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = AmoledBlack),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Игра окончена",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GoogleSans
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Счет: ${viewModel.score}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontFamily = GoogleSans
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = { viewModel.startNewGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = ActiveBlockColor),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "Играть снова",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = GoogleSans
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { onReturnToMenu() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "Главное меню",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = GoogleSans
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameBoardGrid(
    board: Array<BooleanArray>,
    hoverCoord: Pair<Int, Int>?,
    hoverShape: BlockShape?,
    glowingRows: Set<Int>,
    glowingCols: Set<Int>
) {
    val hoverCells = remember(hoverCoord, hoverShape) {
        if (hoverCoord != null && hoverShape != null) {
            hoverShape.cells.map { (r, c) -> (hoverCoord.first + r) to (hoverCoord.second + c) }.toSet()
        } else {
            emptySet()
        }
    }

    // Pulsing animation for lines about to be cleared
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        for (r in 0..7) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                for (c in 0..7) {
                    val isFilled = board[r][c]
                    val isHovered = (r to c) in hoverCells
                    val isLineGlowing = r in glowingRows || c in glowingCols

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.5.dp, GridBorderColor)
                            .padding(2.dp)
                    ) {
                        if (isFilled) {
                            // Darker block placed on board
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PlacedBlockColor)
                            )
                        } else if (isHovered) {
                            // Semi-transparent ghost preview
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x558E8E8E))
                            )
                        }

                        // Glowing line overlay when this row/col is about to be cleared
                        if (isLineGlowing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GlowColor.copy(alpha = pulseGlowAlpha * 0.35f))
                                    .border(1.5.dp, GlowColor.copy(alpha = pulseGlowAlpha), RoundedCornerShape(6.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShapeItemView(
    shape: BlockShape,
    isBeingDragged: Boolean,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var itemBounds by remember { mutableStateOf(Rect.Zero) }

    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Box(
        modifier = Modifier
            .alpha(if (isBeingDragged) 0f else 1f)
            .onGloballyPositioned { coordinates ->
                itemBounds = coordinates.boundsInRoot()
            }
            .pointerInput(shape) {
                detectDragGestures(
                    onDragStart = { localOffset ->
                        val touchGlobal = itemBounds.topLeft + localOffset
                        currentOnDragStart(touchGlobal)
                    },
                    onDrag = { change, dragDelta ->
                        change.consume()
                        currentOnDrag(dragDelta)
                    },
                    onDragEnd = {
                        currentOnDragEnd()
                    },
                    onDragCancel = {
                        currentOnDragEnd()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        ShapePreview(
            shape = shape,
            cellSize = 20.dp,
            padding = 1.5.dp,
            blockColor = ActiveBlockColor,
            cornerRadiusFactor = 0.22f
        )
    }
}

@Composable
fun ShapePreview(
    shape: BlockShape,
    cellSize: androidx.compose.ui.unit.Dp,
    padding: androidx.compose.ui.unit.Dp = 1.5.dp,
    blockColor: Color = ActiveBlockColor,
    cornerRadiusFactor: Float = 0.22f
) {
    Column {
        for (r in 0 until shape.height) {
            Row {
                for (c in 0 until shape.width) {
                    val hasBlock = (r to c) in shape.cells
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .padding(padding)
                    ) {
                        if (hasBlock) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(cellSize * cornerRadiusFactor))
                                    .background(blockColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
