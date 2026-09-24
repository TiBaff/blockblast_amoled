package com.blockblast.amoled

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class GameViewModel(context: Context) : ViewModel() {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("block_blast_amoled_prefs", Context.MODE_PRIVATE)

    var board by mutableStateOf(Array(8) { BooleanArray(8) { false } })
        private set

    var score by mutableIntStateOf(0)
        private set

    var bestScore by mutableIntStateOf(prefs.getInt("best_score", 0))
        private set

    var combo by mutableIntStateOf(0)
        private set

    var availableShapes by mutableStateOf<List<BlockShape?>>(listOf(null, null, null))
        private set

    var isGameOver by mutableStateOf(false)
        private set

    init {
        startNewGame()
    }

    fun startNewGame() {
        board = Array(8) { BooleanArray(8) { false } }
        score = 0
        combo = 0
        isGameOver = false
        refreshShapes()
    }

    private fun refreshShapes() {
        availableShapes = listOf(
            BlockShape.getRandom(),
            BlockShape.getRandom(),
            BlockShape.getRandom()
        )
        checkGameOver()
    }

    fun canPlace(shape: BlockShape, startRow: Int, startCol: Int): Boolean {
        for ((r, c) in shape.cells) {
            val targetR = startRow + r
            val targetC = startCol + c
            if (targetR !in 0..7 || targetC !in 0..7) return false
            if (board[targetR][targetC]) return false
        }
        return true
    }

    private fun canPlaceAnywhere(shape: BlockShape): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                if (canPlace(shape, r, c)) return true
            }
        }
        return false
    }

    fun placeShape(slotIndex: Int, startRow: Int, startCol: Int): Boolean {
        val shape = availableShapes.getOrNull(slotIndex) ?: return false
        if (!canPlace(shape, startRow, startCol)) return false

        // Copy board and apply shape
        val newBoard = Array(8) { r -> board[r].clone() }
        for ((r, c) in shape.cells) {
            newBoard[startRow + r][startCol + c] = true
        }

        // Add points for placing the shape
        score += shape.cells.size

        // Check full rows and columns
        val fullRows = mutableListOf<Int>()
        for (r in 0..7) {
            if (newBoard[r].all { it }) fullRows.add(r)
        }

        val fullCols = mutableListOf<Int>()
        for (c in 0..7) {
            var colFull = true
            for (r in 0..7) {
                if (!newBoard[r][c]) {
                    colFull = false
                    break
                }
            }
            if (colFull) fullCols.add(c)
        }

        val clearedLines = fullRows.size + fullCols.size
        if (clearedLines > 0) {
            combo += 1
            val clearPoints = clearedLines * 10 * (combo + 1)
            score += clearPoints

            // Clear full rows
            for (r in fullRows) {
                for (c in 0..7) newBoard[r][c] = false
            }
            // Clear full columns
            for (c in fullCols) {
                for (r in 0..7) newBoard[r][c] = false
            }
        } else {
            combo = 0
        }

        board = newBoard

        // Update best score
        if (score > bestScore) {
            bestScore = score
            prefs.edit().putInt("best_score", bestScore).apply()
        }

        // Mark slot as used
        val updatedShapes = availableShapes.toMutableList()
        updatedShapes[slotIndex] = null
        availableShapes = updatedShapes

        // If all 3 shapes used, refresh
        if (availableShapes.all { it == null }) {
            refreshShapes()
        } else {
            checkGameOver()
        }

        return true
    }

    private fun checkGameOver() {
        val activeShapes = availableShapes.filterNotNull()
        if (activeShapes.isEmpty()) return

        val hasValidMove = activeShapes.any { shape -> canPlaceAnywhere(shape) }
        if (!hasValidMove) {
            isGameOver = true
        }
    }
}
