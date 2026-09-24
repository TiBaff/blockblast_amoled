package com.blockblast.amoled

import kotlin.random.Random

data class BlockShape(
    val id: String,
    val cells: List<Pair<Int, Int>>
) {
    val width: Int = (cells.maxOfOrNull { it.second } ?: 0) + 1
    val height: Int = (cells.maxOfOrNull { it.first } ?: 0) + 1

    companion object {
        val ALL_SHAPES = listOf(
            // 1x1 Dot
            BlockShape("dot", listOf(0 to 0)),

            // Lines 1x2 and 2x1
            BlockShape("line_h2", listOf(0 to 0, 0 to 1)),
            BlockShape("line_v2", listOf(0 to 0, 1 to 0)),

            // Lines 1x3 and 3x1
            BlockShape("line_h3", listOf(0 to 0, 0 to 1, 0 to 2)),
            BlockShape("line_v3", listOf(0 to 0, 1 to 0, 2 to 0)),

            // Lines 1x4 and 4x1
            BlockShape("line_h4", listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3)),
            BlockShape("line_v4", listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0)),

            // Lines 1x5 and 5x1
            BlockShape("line_h5", listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4)),
            BlockShape("line_v5", listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0)),

            // Squares
            BlockShape("sq2", listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)),
            BlockShape("sq3", listOf(
                0 to 0, 0 to 1, 0 to 2,
                1 to 0, 1 to 1, 1 to 2,
                2 to 0, 2 to 1, 2 to 2
            )),

            // Rectangles 2x3 and 3x2
            BlockShape("rect_2x3", listOf(
                0 to 0, 0 to 1, 0 to 2,
                1 to 0, 1 to 1, 1 to 2
            )),
            BlockShape("rect_3x2", listOf(
                0 to 0, 0 to 1,
                1 to 0, 1 to 1,
                2 to 0, 2 to 1
            )),

            // Small Corners (2x2)
            BlockShape("corner_tl", listOf(0 to 0, 0 to 1, 1 to 0)),
            BlockShape("corner_tr", listOf(0 to 0, 0 to 1, 1 to 1)),
            BlockShape("corner_bl", listOf(0 to 0, 1 to 0, 1 to 1)),
            BlockShape("corner_br", listOf(0 to 1, 1 to 0, 1 to 1)),

            // Large Corners (3x3)
            BlockShape("corner3_tl", listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 2 to 0)),
            BlockShape("corner3_tr", listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2, 2 to 2)),
            BlockShape("corner3_bl", listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1, 2 to 2)),
            BlockShape("corner3_br", listOf(0 to 2, 1 to 2, 2 to 0, 2 to 1, 2 to 2)),

            // L-shapes (3x2)
            BlockShape("L1", listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1)),
            BlockShape("L2", listOf(0 to 1, 1 to 1, 2 to 1, 2 to 0)),
            BlockShape("L3", listOf(0 to 0, 0 to 1, 1 to 0, 2 to 0)),
            BlockShape("L4", listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1)),
            BlockShape("L5", listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0)),
            BlockShape("L6", listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2)),
            BlockShape("L7", listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2)),
            BlockShape("L8", listOf(0 to 2, 1 to 0, 1 to 1, 1 to 2)),

            // T-shapes
            BlockShape("T1", listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1)),
            BlockShape("T2", listOf(0 to 1, 1 to 0, 1 to 1, 2 to 1)),
            BlockShape("T3", listOf(0 to 1, 1 to 0, 1 to 1, 1 to 2)),
            BlockShape("T4", listOf(0 to 0, 1 to 0, 1 to 1, 2 to 0)),

            // Z & S shapes
            BlockShape("Z1", listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2)),
            BlockShape("Z2", listOf(0 to 1, 0 to 2, 1 to 0, 1 to 1)),
            BlockShape("Z3", listOf(0 to 1, 1 to 0, 1 to 1, 2 to 0)),
            BlockShape("Z4", listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1))
        )

        fun getRandom(): BlockShape {
            return ALL_SHAPES[Random.nextInt(ALL_SHAPES.size)]
        }
    }
}
