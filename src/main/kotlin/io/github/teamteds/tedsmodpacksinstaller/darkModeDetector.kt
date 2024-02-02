package io.github.teamteds.tedsmodpacksinstaller

import io.github.oshai.KotlinLogging
import java.io.BufferedReader
import java.io.IOException

// Ported to Kotlin from https://github.com/IrisShaders/Iris-Installer/blob/main/src/main/java/net/hypercubemc/iris_installer/DarkModeDetector.java

private val logger = KotlinLogging.logger {}

private const val REGQUERY_UTIL = "reg query "
private const val REGDWORD_TOKEN = "REG_DWORD"
private const val DARK_THEME_CMD = "$REGQUERY_UTIL\"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\" /v AppsUseLightTheme"
private val DARK_THEME_PATTERN = Regex(".*dark.*", RegexOption.IGNORE_CASE)

fun isDarkMode() = when (operatingSystem) {
    OperatingSystem.WINDOWS -> isWindowsDarkMode()
    OperatingSystem.MACOS -> isMacOsDarkMode()
    OperatingSystem.LINUX -> isGnome() && isGnomeDarkMode()
    else -> false
}

private fun isMacOsDarkMode() = query("defaults read -g AppleInterfaceStyle") == "Dark"

private fun isWindowsDarkMode() = try {
    query(DARK_THEME_CMD)
        .substringAfter(REGDWORD_TOKEN)
        .ifEmpty { null }
        ?.trim()
        ?.substring(2)
        ?.toInt(16) == 0
} catch (e: Exception) {
    false
}

private fun isGnomeDarkMode() = query("gsettings get org.gnome.desktop.interface gtk-theme") matches DARK_THEME_PATTERN

private fun isGnome() = operatingSystem == OperatingSystem.LINUX && (
    queryResultContains("echo \$XDG_CURRENT_DESKTOP", "gnome") ||
    queryResultContains("echo \$XDG_DATA_DIRS | grep -Eo 'gnome'", "gnome") ||
    queryResultContains("ps -e | grep -E -i \"gnome\"", "gnome")
)

private fun queryResultContains(cmd: String, subResult: String) = query(cmd).contains(subResult, ignoreCase = true)

private fun query(cmd: String) = try {
    Runtime.getRuntime()
        .exec(cmd)
        .inputStream
        .bufferedReader()
        .use(BufferedReader::readText)
} catch (e: IOException) {
    logger.error("Exception caught while querying the OS", e)
    ""
}
