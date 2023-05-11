package io.github.gaming32.additiveinstaller

import com.google.gson.JsonParser
import io.github.oshai.KotlinLogging
import java.awt.Component
import java.net.URL
import java.util.Locale
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

inline fun <T, K1, K2> Sequence<T>.associateByMultiple(
    keySelector1: (T) -> K1, keySelector2: (T) -> K2
): Map<K1, Map<K2, T>> {
    val result = mutableMapOf<K1, MutableMap<K2, T>>()
    for (value in this) {
        result.getOrPut(keySelector1(value), ::mutableMapOf)[keySelector2(value)] = value
    }
    return result
}

inline fun <T, K1, K2, K3> Sequence<T>.associateByMultiple(
    keySelector1: (T) -> K1, keySelector2: (T) -> K2, keySelector3: (T) -> K3
): Map<K1, Map<K2, Map<K3, T>>> {
    val result = mutableMapOf<K1, MutableMap<K2, MutableMap<K3, T>>>()
    for (value in this) {
        result.getOrPut(keySelector1(value), ::mutableMapOf)
            .getOrPut(keySelector2(value), ::mutableMapOf)[keySelector3(value)] = value
    }
    return result
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
