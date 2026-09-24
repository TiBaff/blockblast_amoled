package com.blockblast.amoled

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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
import kotlin.math.roundToInt

enum class AppScreen {
    MENU,
    GAME
}

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

            // Classic Button (pure dark gray, rounded rectangle, ∞ Classic)
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

    // Drag and Drop state
    var draggedSlotIndex by remember { mutableStateOf<Int?>(null) }
    var dragGlobalPosition by remember { mutableStateOf(Offset.Zero) }
    var boardBounds by remember { mutableStateOf(Rect.Zero) }

    val density = LocalDensity.current
    // Offset piece 85dp above finger so finger does not block view of piece or board
    val fingerLiftPx = with(density) { 85.dp.toPx() }

    // Calculate hover grid coordinate (where the ghost preview should appear)
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
                // Crown + Best Score
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

                // Settings Gear
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
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
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 8x8 Game Board
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
                    hoverShape = draggedSlotIndex?.let { viewModel.availableShapes.getOrNull(it) }
                )
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
                                    // Place shape if dropped over valid grid position
                                    hoveredCoord?.let { (r, c) ->
                                        viewModel.placeShape(i, r, c)
                                    }
                                    draggedSlotIndex = null
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Shape Overlay during Drag:
        // Smoothly follows the finger 1:1 with 85dp upward lift without snapping or jumping!
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
                        cornerRadiusFactor = 0.22f
                    )
                }
            }
        }

        // Settings Dialog (Exit to Main Menu)
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
                            colors = ButtonDefaults.buttonColors(containerColor = BlockGray),
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
    hoverShape: BlockShape?
) {
    val hoverCells = remember(hoverCoord, hoverShape) {
        if (hoverCoord != null && hoverShape != null) {
            hoverShape.cells.map { (r, c) -> (hoverCoord.first + r) to (hoverCoord.second + c) }.toSet()
        } else {
            emptySet()
        }
    }

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

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.5.dp, GridBorderColor)
                            .padding(2.dp)
                    ) {
                        if (isFilled) {
                            // Placed solid block
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BlockGray)
                            )
                        } else if (isHovered) {
                            // Semi-transparent ghost preview block on grid (like original Block Blast)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x557E7E7E))
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

    // Use rememberUpdatedState to guarantee latest lambdas are called without stale closures
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
            cornerRadiusFactor = 0.22f
        )
    }
}

@Composable
fun ShapePreview(
    shape: BlockShape,
    cellSize: androidx.compose.ui.unit.Dp,
    padding: androidx.compose.ui.unit.Dp = 1.5.dp,
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
                                    .background(BlockGray)
                            )
                        }
                    }
                }
            }
        }
    }
}
