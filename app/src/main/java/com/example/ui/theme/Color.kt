package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Global dynamic theme variable updated from Settings
var currentThemeName = "space_dark"

// Orion AI Premium Cosmic Custom Theme Palettes via custom property getters
val OrionBackground: Color
    get() = when (currentThemeName) {
        "nebula_teal" -> Color(0xFF070B11)
        "aurora_green" -> Color(0xFF050B08)
        "light_starlight" -> Color(0xFFF8FAFC)
        else -> Color(0xFF090D16) // space_dark
    }

val OrionSurface: Color
    get() = when (currentThemeName) {
        "nebula_teal" -> Color(0xFF0E1724)
        "aurora_green" -> Color(0xFF0B1710)
        "light_starlight" -> Color(0xFFFFFFFF)
        else -> Color(0xFF131A26)
    }

val OrionSurfaceVariant: Color
    get() = when (currentThemeName) {
        "nebula_teal" -> Color(0xFF16253B)
        "aurora_green" -> Color(0xFF12281D)
        "light_starlight" -> Color(0xFFE2E8F0)
        else -> Color(0xFF1E293B)
    }

val OrionPrimary: Color
    get() = when (currentThemeName) {
        "nebula_teal" -> Color(0xFF0EA5E9)      // Nebula Teal Blue
        "aurora_green" -> Color(0xFF10B981)     // Aurora Emerald
        "light_starlight" -> Color(0xFF0284C7)  // Starlight sky blue
        else -> Color(0xFF3B82F6)               // space_dark: Bright Nebula Blue
    }

val OrionSecondary: Color
    get() = when (currentThemeName) {
        "nebula_teal" -> Color(0xFF14B8A6)      // Teal Accent
        "aurora_green" -> Color(0xFF84CC16)     // Lime Green Accent
        "light_starlight" -> Color(0xFF4F46E5)  // Indigo Accent
        else -> Color(0xFF8B5CF6)               // space_dark: Cosmic Violet
    }

val OrionTertiary: Color
    get() = when (currentThemeName) {
        "nebula_teal" -> Color(0xFF2DD4BF)
        "aurora_green" -> Color(0xFF22C55E)
        "light_starlight" -> Color(0xFF10B981)
        else -> Color(0xFF10B981)
    }

val OrionTextPrimary: Color
    get() = when (currentThemeName) {
        "light_starlight" -> Color(0xFF0F172A) // Dark Slate text
        else -> Color(0xFFF1F5F9)             // Silver/White text
    }

val OrionTextSecondary: Color
    get() = when (currentThemeName) {
        "light_starlight" -> Color(0xFF475569) // Slate Gray text
        else -> Color(0xFF94A3B8)
    }

val OrionTextMuted: Color
    get() = when (currentThemeName) {
        "light_starlight" -> Color(0xFF94A3B8)
        else -> Color(0xFF64748B)
    }

// Message bubble colors
val UserBubbleBg: Color
    get() = when (currentThemeName) {
        "light_starlight" -> Color(0xFF0284C7)
        else -> Color(0xFF2563EB)
    }

val AiBubbleBg: Color
    get() = when (currentThemeName) {
        "light_starlight" -> Color(0xFFF1F5F9)
        else -> Color(0xFF1E293B)
    }
