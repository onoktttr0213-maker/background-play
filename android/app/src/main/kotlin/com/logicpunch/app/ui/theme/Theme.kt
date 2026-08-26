package com.logicpunch.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette mirrors the web prototype (web/index.html) so both builds feel like one app.
val Ink = Color(0xFF17181C)
val InkDark = Color(0xFFF1EFE6)
val PaperLight = Color(0xFFEEF0EA)
val PaperDark = Color(0xFF131417)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF1D1F24)
val Accent = Color(0xFFF5B400)
val AccentDark = Color(0xFFFFD75E)

val AttrRedLight = Color(0xFFD6453B)
val AttrBlueLight = Color(0xFF276FB5)
val AttrGreenLight = Color(0xFF3C8F52)
val AttrRedDark = Color(0xFFFF8177)
val AttrBlueDark = Color(0xFF7DB3EF)
val AttrGreenDark = Color(0xFF79D492)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Ink,
    background = PaperLight,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    error = AttrRedLight,
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Ink,
    background = PaperDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    error = AttrRedDark,
)

data class AttributeColors(val red: Color, val blue: Color, val green: Color)

val LocalAttributeColors = androidx.compose.runtime.staticCompositionLocalOf {
    AttributeColors(AttrRedLight, AttrBlueLight, AttrGreenLight)
}

@Composable
fun LogicPunchTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) DarkColors else LightColors
    val attrColors = if (dark) {
        AttributeColors(AttrRedDark, AttrBlueDark, AttrGreenDark)
    } else {
        AttributeColors(AttrRedLight, AttrBlueLight, AttrGreenLight)
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalAttributeColors provides attrColors) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
