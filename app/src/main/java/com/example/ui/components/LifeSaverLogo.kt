package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LifeSaverLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Box(
        modifier = modifier
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing shadow/ring background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF97316).copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // The stylized Life Saver Ring
        Canvas(
            modifier = Modifier
                .fillMaxSize(0.85f)
        ) {
            val strokeWidth = size.toPx() * 0.14f
            val radiusPx = size.toPx() * 0.33f
            
            // Draw the main orange-gold ring body
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF97316), // OrangeRescue
                        Color(0xFFFCD34D)  // AmberRescue
                    )
                ),
                radius = radiusPx,
                style = Stroke(width = strokeWidth)
            )

            // Draw white lifesaver strap segments
            val segmentWidth = strokeWidth * 1.15f
            val arcSize = radiusPx * 2f
            val centerOffset = (size.toPx() * 0.85f - arcSize) / 2f
            
            // Draw four strap accents (top, bottom, left, right)
            drawArc(
                color = Color.White.copy(alpha = 0.9f),
                startAngle = -15f,
                sweepAngle = 30f,
                useCenter = false,
                style = Stroke(width = segmentWidth),
                size = Size(arcSize, arcSize),
                topLeft = Offset(centerOffset, centerOffset)
            )
            drawArc(
                color = Color.White.copy(alpha = 0.9f),
                startAngle = 75f,
                sweepAngle = 30f,
                useCenter = false,
                style = Stroke(width = segmentWidth),
                size = Size(arcSize, arcSize),
                topLeft = Offset(centerOffset, centerOffset)
            )
            drawArc(
                color = Color.White.copy(alpha = 0.9f),
                startAngle = 165f,
                sweepAngle = 30f,
                useCenter = false,
                style = Stroke(width = segmentWidth),
                size = Size(arcSize, arcSize),
                topLeft = Offset(centerOffset, centerOffset)
            )
            drawArc(
                color = Color.White.copy(alpha = 0.9f),
                startAngle = 255f,
                sweepAngle = 30f,
                useCenter = false,
                style = Stroke(width = segmentWidth),
                size = Size(arcSize, arcSize),
                topLeft = Offset(centerOffset, centerOffset)
            )
        }

        // Central premium Hourglass Icon container
        Box(
            modifier = Modifier
                .fillMaxSize(0.44f)
                .background(Color(0xFF0F0F15), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize(0.55f)
            ) {
                rotate(15f) {
                    val w = this.size.width
                    val h = this.size.height
                    
                    // Hourglass Glass Body Path
                    val glassPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.25f, h * 0.22f)
                        lineTo(w * 0.75f, h * 0.22f)
                        quadraticBezierTo(w * 0.7f, h * 0.38f, w * 0.54f, h * 0.48f)
                        quadraticBezierTo(w * 0.7f, h * 0.58f, w * 0.75f, h * 0.74f)
                        lineTo(w * 0.25f, h * 0.74f)
                        quadraticBezierTo(w * 0.3f, h * 0.58f, w * 0.46f, h * 0.48f)
                        quadraticBezierTo(w * 0.3f, h * 0.38f, w * 0.25f, h * 0.22f)
                        close()
                    }
                    
                    // Draw glass outline
                    drawPath(
                        path = glassPath,
                        color = Color.White.copy(alpha = 0.85f),
                        style = Stroke(width = 1.25.dp.toPx())
                    )

                    // Top & Bottom wooden plates
                    drawRect(
                        color = Color(0xFFFCD34D),
                        topLeft = Offset(w * 0.15f, h * 0.16f),
                        size = Size(w * 0.7f, h * 0.07f)
                    )
                    drawRect(
                        color = Color(0xFFF97316),
                        topLeft = Offset(w * 0.15f, h * 0.73f),
                        size = Size(w * 0.7f, h * 0.07f)
                    )

                    // Top Sand (Filling top chamber)
                    val topSandPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.29f, h * 0.25f)
                        lineTo(w * 0.71f, h * 0.25f)
                        quadraticBezierTo(w * 0.65f, h * 0.38f, w * 0.5f, h * 0.46f)
                        quadraticBezierTo(w * 0.35f, h * 0.38f, w * 0.29f, h * 0.25f)
                        close()
                    }
                    drawPath(
                        path = topSandPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFDE68A), Color(0xFFFCD34D))
                        )
                    )

                    // Bottom Sand (Accumulating at the bottom)
                    val bottomSandPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.5f, h * 0.52f)
                        quadraticBezierTo(w * 0.58f, h * 0.64f, w * 0.72f, h * 0.72f)
                        lineTo(w * 0.28f, h * 0.72f)
                        quadraticBezierTo(w * 0.42f, h * 0.64f, w * 0.5f, h * 0.52f)
                        close()
                    }
                    drawPath(
                        path = bottomSandPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFCD34D), Color(0xFFF97316))
                        )
                    )

                    // Sand trickle
                    drawLine(
                        color = Color(0xFFFCD34D),
                        start = Offset(w * 0.5f, h * 0.45f),
                        end = Offset(w * 0.5f, h * 0.72f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
    }
}
