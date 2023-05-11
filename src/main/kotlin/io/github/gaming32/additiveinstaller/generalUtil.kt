package io.github.gaming32.additiveinstaller

import com.google.gson.JsonParser
import io.github.oshai.KotlinLogging
import java.awt.Component
import java.net.URL
import java.util.*
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

enum class OperatingSystem {
    WINDOWS, MACOS, LINUX, SOLARIS, NONE
}

val operatingSystem = System.getProperty("os.name").let { when {
    it.contains("win", ignoreCase = true) -> OperatingSystem.WINDOWS
    it.contains("mac", ignoreCase = true) -> OperatingSystem.MACOS
    it.contains("nix", ignoreCase = true) || it.contains("nux", ignoreCase = true) -> OperatingSystem.LINUX
    it.contains("sunos", ignoreCase = true) -> OperatingSystem.SOLARIS
    else -> OperatingSystem.NONE
} }

fun Component.withLabel(label: String) = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.LINE_AXIS)
    add(JLabel(label))
    add(this@withLabel)
}

fun requestJson(url: String) = try {
    logger.info("Requesting {}", url)
    URL(url).openStream().reader().use(JsonParser::parseReader)!!
} catch (e: Exception) {
    logger.error("Failed to request {}", url, e)
    JOptionPane.showMessageDialog(
        null,
        "Failed to access Modrinth. Please check your internet connection.",
        "Additive Installer",
        JOptionPane.ERROR_MESSAGE
    )
    exitProcess(1)
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
