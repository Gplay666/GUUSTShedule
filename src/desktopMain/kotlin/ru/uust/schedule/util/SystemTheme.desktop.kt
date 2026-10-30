package ru.uust.schedule.util

import androidx.compose.ui.graphics.Color
import java.util.concurrent.TimeUnit

actual fun readSystemTheme(): SystemThemeInfo {
    val scheme = readPortalSetting("color-scheme")
        ?: runCommand("gsettings", "get", "org.gnome.desktop.interface", "color-scheme")
        ?: runCommand("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme")
        ?: runCommand(
            "kreadconfig6",
            "--file",
            "kdeglobals",
            "--group",
            "General",
            "--key",
            "ColorScheme",
        )

    val accent = readPortalAccentColor()
        ?: readGnomeAccentColor()
        ?: readKdeAccentColor()

    return SystemThemeInfo(
        isDark = parseDarkScheme(scheme),
        accentColor = accent,
    )
}

private fun parseDarkScheme(raw: String?): Boolean? {
    if (raw.isNullOrBlank()) return null

    val portalValue = Regex("""uint32\s+(\d+)""")
        .find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
    if (portalValue != null) {
        return when (portalValue) {
            1 -> true
            2 -> false
            else -> null
        }
    }

    val value = raw.lowercase()
    return when {
        "prefer-dark" in value || "dark" in value -> true
        "prefer-light" in value || "default" in value || "light" in value -> false
        else -> null
    }
}

private fun readPortalSetting(key: String): String? = runCommand(
    "gdbus",
    "call",
    "--session",
    "--dest",
    "org.freedesktop.portal.Desktop",
    "--object-path",
    "/org/freedesktop/portal/desktop",
    "--method",
    "org.freedesktop.portal.Settings.Read",
    "org.freedesktop.appearance",
    key,
)

private fun readPortalAccentColor(): Color? {
    val raw = readPortalSetting("accent-color") ?: return null
    val match = Regex(
        """\(\s*([0-9]*\.?[0-9]+)\s*,\s*([0-9]*\.?[0-9]+)\s*,\s*([0-9]*\.?[0-9]+)\s*\)"""
    ).find(raw) ?: return null

    val values = match.groupValues.drop(1).mapNotNull { it.toFloatOrNull() }
    if (values.size != 3) return null
    val scale = if (values.all { it <= 1f }) 255f else 1f
    return Color(
        red = (values[0] * scale / 255f).coerceIn(0f, 1f),
        green = (values[1] * scale / 255f).coerceIn(0f, 1f),
        blue = (values[2] * scale / 255f).coerceIn(0f, 1f),
    )
}

private fun readGnomeAccentColor(): Color? {
    val raw = runCommand("gsettings", "get", "org.gnome.desktop.interface", "accent-color")
        ?: return null
    val name = raw.trim().trim('\'', '"').lowercase()
    val color = when (name) {
        "blue" -> 0xFF3584E4
        "teal" -> 0xFF2190A4
        "green" -> 0xFF3A944A
        "yellow" -> 0xFFC88800
        "orange" -> 0xFFED5B00
        "red" -> 0xFFE62D42
        "pink" -> 0xFFD56199
        "purple" -> 0xFF9141AC
        "slate" -> 0xFF6F8396
        else -> null
    }
    return color?.let(::Color)
}

private fun readKdeAccentColor(): Color? {
    val raw = sequenceOf("kreadconfig6", "kreadconfig5")
        .mapNotNull { command ->
            runCommand(
                command,
                "--file",
                "kdeglobals",
                "--group",
                "General",
                "--key",
                "AccentColor",
            )
        }
        .firstOrNull()
        ?: return null

    val hex = Regex("""#([0-9a-fA-F]{6})""").find(raw)?.groupValues?.get(1)
    if (hex != null) return Color(0xFF000000 or hex.toLong(16))

    val rgb = raw.split(',', ' ', ';')
        .mapNotNull { it.trim().toIntOrNull() }
    if (rgb.size >= 3) {
        return Color(
            red = rgb[0].coerceIn(0, 255) / 255f,
            green = rgb[1].coerceIn(0, 255) / 255f,
            blue = rgb[2].coerceIn(0, 255) / 255f,
        )
    }
    return null
}

private fun runCommand(vararg command: String): String? {
    return try {
        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(900, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            return null
        }
        if (process.exitValue() != 0) return null
        process.inputStream.bufferedReader().use { it.readText() }
            .trim()
            .takeIf { it.isNotEmpty() }
    } catch (_: Throwable) {
        null
    }
}
