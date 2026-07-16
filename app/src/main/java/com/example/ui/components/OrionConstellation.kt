package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OrionPrimary
import com.example.ui.theme.OrionSecondary
import com.example.ui.theme.OrionTextPrimary
import kotlin.random.Random

@Composable
fun OrionConstellation(modifier: Modifier = Modifier) {
    // Pulse animation for stars and glowing lines
    val infiniteTransition = rememberInfiniteTransition(label = "constellation")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)

        // Generate coordinates relative to the canvas center
        // Representing major stars in the Orion constellation
        val betelgeuse = Offset(center.x - 70.dp.toPx(), center.y - 80.dp.toPx()) // Top-left shoulder
        val bellatrix = Offset(center.x + 70.dp.toPx(), center.y - 75.dp.toPx())  // Top-right shoulder
        val rigel = Offset(center.x + 60.dp.toPx(), center.y + 85.dp.toPx())     // Bottom-right foot
        val saiph = Offset(center.x - 65.dp.toPx(), center.y + 80.dp.toPx())     // Bottom-left foot
        
        // Orion's Belt (three aligned central stars)
        val alnitak = Offset(center.x - 30.dp.toPx(), center.y)                 // Belt-left
        val alnilam = Offset(center.x, center.y + 2.dp.toPx())                   // Belt-center
        val mintaka = Offset(center.x + 30.dp.toPx(), center.y + 4.dp.toPx())     // Belt-right

        // Orion's Sword (hanging from belt)
        val sword1 = Offset(center.x, center.y + 20.dp.toPx())
        val sword2 = Offset(center.x - 5.dp.toPx(), center.y + 40.dp.toPx())

        // Connected lines list (pairs)
        val constellationLines = listOf(
            Pair(betelgeuse, bellatrix), // Shoulder connection
            Pair(betelgeuse, alnitak),   // Left shoulder to belt
            Pair(bellatrix, mintaka),    // Right shoulder to belt
            Pair(alnitak, saiph),        // Belt to left foot
            Pair(mintaka, rigel),        // Belt to right foot
            Pair(saiph, rigel),          // Feet connection
            
            // Belt line
            Pair(alnitak, alnilam),
            Pair(alnilam, mintaka),
            
            // Sword hanging
            Pair(alnilam, sword1),
            Pair(sword1, sword2)
        )

        // Draw connections with glowing effect
        constellationLines.forEach { (start, end) ->
            // Outer glow line
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(OrionSecondary.copy(alpha = 0.15f * pulseScale), OrionPrimary.copy(alpha = 0.15f * pulseScale))
                ),
                start = start,
                end = end,
                strokeWidth = 6.dp.toPx()
            )
            // Core sharp line
            drawLine(
                color = OrionPrimary.copy(alpha = 0.4f + (0.3f * pulseScale)),
                start = start,
                end = end,
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // Draw background stardust particles
        val random = Random(42) // Constant seed so particles don't jump around
        for (i in 0..25) {
            val starX = random.nextFloat() * width
            val starY = random.nextFloat() * height
            val starAlpha = random.nextFloat() * 0.5f * pulseScale
            drawCircle(
                color = Color.White.copy(alpha = starAlpha),
                radius = random.nextFloat() * 2.dp.toPx(),
                center = Offset(starX, starY)
            )
        }

        // Draw major stars with glowing radial pulses
        val majorStars = listOf(
            Pair(betelgeuse, "Betelgeuse"),
            Pair(bellatrix, "Bellatrix"),
            Pair(rigel, "Rigel"),
            Pair(saiph, "Saiph"),
            Pair(alnitak, "Alnitak"),
            Pair(alnilam, "Alnilam"),
            Pair(mintaka, "Mintaka"),
            Pair(sword2, "Nebula")
        )

        majorStars.forEach { (pos, name) ->
            val isSupergiant = name == "Betelgeuse" || name == "Rigel" || name == "Nebula"
            val coreColor = if (name == "Betelgeuse") Color(0xFFF97316) else OrionTextPrimary // Betelgeuse is a red supergiant!

            // Outer nebula glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isSupergiant) OrionSecondary.copy(alpha = 0.35f * pulseScale) else OrionPrimary.copy(alpha = 0.25f * pulseScale),
                        Color.Transparent
                    ),
                    center = pos,
                    radius = if (isSupergiant) 18.dp.toPx() else 12.dp.toPx()
                ),
                center = pos,
                radius = if (isSupergiant) 18.dp.toPx() else 12.dp.toPx()
            )

            // Sharp center star core
            drawCircle(
                color = coreColor,
                radius = if (isSupergiant) 4.dp.toPx() else 2.5.dp.toPx(),
                center = pos
            )
        }
    }
}
