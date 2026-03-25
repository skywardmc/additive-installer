package io.github.gaming32.additiveinstaller

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.IOException

// Ported to Kotlin from https://github.com/IrisShaders/Iris-Installer/blob/main/src/main/java/net/hypercubemc/iris_installer/DarkModeDetector.java

private val logger = KotlinLogging.logger {}

private const val REGQUERY_UTIL = "reg query "
private const val REGDWORD_TOKEN = "REG_DWORD"
private const val DARK_THEME_CMD = "$REGQUERY_UTIL\"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\" /v AppsUseLightTheme"
private val DARK_THEME_PATTERN = Regex(".*dark.*", RegexOption.IGNORE_CASE)

fun isDarkMode(): Boolean {
    return when (operatingSystem) {
        OperatingSystem.WINDOWS -> isWindowsDarkMode()
        OperatingSystem.MACOS -> isMacOsDarkMode()
        OperatingSystem.LINUX -> (isGnome() && isGnomeDarkMode()) || (isKde() && isKdeDarkMode())
        else -> false
    }
}

private fun isMacOsDarkMode(): Boolean {
    val result = query("defaults read -g AppleInterfaceStyle")
    return result == "Dark"
}

private fun isWindowsDarkMode(): Boolean {
    return try {
        val result = query(DARK_THEME_CMD)
        result.substringAfter(REGDWORD_TOKEN)
            .ifEmpty { null }
            ?.trim()
            ?.substring(2)
            ?.toInt(16) == 0
    } catch (e: Exception) {
        false
    }
}

private fun isGnomeDarkMode(): Boolean {
    val result = query("gsettings get org.gnome.desktop.interface gtk-theme")
    return result matches DARK_THEME_PATTERN
}

private fun isGnome(): Boolean {
    return operatingSystem == OperatingSystem.LINUX && (
            queryResultContains("echo \$XDG_CURRENT_DESKTOP", "gnome") ||
                    queryResultContains("echo \$XDG_DATA_DIRS | grep -Eo 'gnome'", "gnome") ||
                    queryResultContains("ps -e | grep -E -i \"gnome\"", "gnome")
            )
}

private fun isKdeDarkMode(): Boolean {
    val currentLookAndFeel = query("lookandfeeltool --current")
    if (currentLookAndFeel.isEmpty()) {
        val alternativeLookAndFeel = query("kreadconfig5 --group KDE --key LookAndFeelPackage")
        return alternativeLookAndFeel.contains("dark", ignoreCase = true)
    }
    return currentLookAndFeel.contains("dark", ignoreCase = true)
}

private fun isKde(): Boolean {
    return operatingSystem == OperatingSystem.LINUX && (
            queryResultContains("echo \$XDG_CURRENT_DESKTOP", "KDE") ||
                    queryResultContains("echo \$XDG_DATA_DIRS | grep -Eo 'kde'", "kde") ||
                    queryResultContains("ps -e | grep -E -i \"kde\"", "kde")
            )
}

private fun queryResultContains(cmd: String, subResult: String): Boolean {
    return query(cmd).contains(subResult, ignoreCase = true)
}

private fun query(cmd: String): String {
    return try {
        Runtime.getRuntime()
            .exec(cmd)
            .inputStream
            .bufferedReader()
            .use(BufferedReader::readText)
    } catch (e: IOException) {
        logger.error { "Exception caught while querying the OS" }
        ""
    }
}
