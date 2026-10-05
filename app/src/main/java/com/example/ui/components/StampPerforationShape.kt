package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class StampPerforationShape(
    private val notchRadius: Dp = 5.dp,
    private val notchSpacing: Dp = 15.dp,
    private val shapeType: String = "PORTRAIT" // PORTRAIT, SQUARE, CIRCLE, CONTOUR
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = with(density) { notchRadius.toPx() }
        val spacing = with(density) { notchSpacing.toPx() }

        if (shapeType == "CIRCLE") {
            // Scalloped Circular Stamp
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f - r

            val basePath = Path().apply {
                addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
            }

            val notches = Path()
            val circumference = 2 * PI * radius
            val count = (circumference / spacing).toInt().coerceAtLeast(12)
            val angleStep = (2 * PI) / count

            for (i in 0 until count) {
                val angle = i * angleStep
                val nx = center.x + (radius) * cos(angle).toFloat()
                val ny = center.y + (radius) * sin(angle).toFloat()
                notches.addOval(Rect(nx - r, ny - r, nx + r, ny + r))
            }

            val finalPath = Path()
            finalPath.op(basePath, notches, PathOperation.Difference)
            return Outline.Generic(finalPath)
        }

        val basePath = Path().apply {
            if (shapeType == "CONTOUR") {
                val corner = with(density) { 16.dp.toPx() }
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        0f, 0f, size.width, size.height,
                        corner, corner
                    )
                )
            } else {
                addRect(Rect(0f, 0f, size.width, size.height))
            }
        }

        val notchesPath = Path()

        // Top edge notches
        var x = spacing
        while (x < size.width - spacing / 2) {
            notchesPath.addOval(Rect(x - r, -r, x + r, r))
            x += spacing
        }

        // Bottom edge notches
        x = spacing
        while (x < size.width - spacing / 2) {
            notchesPath.addOval(Rect(x - r, size.height - r, x + r, size.height + r))
            x += spacing
        }

        // Left edge notches
        var y = spacing
        while (y < size.height - spacing / 2) {
            notchesPath.addOval(Rect(-r, y - r, r, y + r))
            y += spacing
        }

        // Right edge notches
        y = spacing
        while (y < size.height - spacing / 2) {
            notchesPath.addOval(Rect(size.width - r, y - r, size.width + r, y + r))
            y += spacing
        }

        val finalPath = Path()
        finalPath.op(basePath, notchesPath, PathOperation.Difference)

        return Outline.Generic(finalPath)
    }
}
