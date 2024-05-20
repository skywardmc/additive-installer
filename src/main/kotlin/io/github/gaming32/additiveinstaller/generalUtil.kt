package io.github.gaming32.additiveinstaller

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonWriter
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.Component
import java.io.InputStream
import java.io.Writer
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest
import java.text.MessageFormat
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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

fun Component.withLabel(label: String? = null) = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.LINE_AXIS)
    if (label != null) {
        add(JLabel(label))
    }
    add(this@withLabel)
}

fun request(url: String): InputStream {
    logger.info { "Requesting $url" }
    val cnxn = URL(url).openConnection()
    cnxn.setRequestProperty("User-Agent", "Additive Installer/$VERSION")
    return cnxn.getInputStream()
}

fun requestJson(url: String) = request(url).reader().use(JsonParser::parseReader)!!

fun requestCriticalJson(url: String) = try {
    requestJson(url)
} catch (e: Exception) {
    logger.error(e) { "Failed to request $url" }
    JOptionPane.showMessageDialog(
        null,
        I18N.getString("modrinth.access.failed"),
        "Additive Installer",
        JOptionPane.ERROR_MESSAGE
    )
    exitProcess(1)
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

fun JsonElement.writeTo(writer: Writer) = Streams.write(this, JsonWriter(writer).apply { setIndent("  ") })

fun String.hexToByteArray(): ByteArray {
    require((length and 1) == 0) { "$this has an odd number of chars" }
    return ByteArray(length shr 1) {
        (this[it shl 1].digitToInt(16) shl 4 or this[(it shl 1) + 1].digitToInt(16)).toByte()
    }
}

fun isoTime(time: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)) = time.format(DateTimeFormatter.ISO_INSTANT)!!

@Suppress("NOTHING_TO_INLINE")
inline operator fun JsonObject.set(property: String, value: JsonElement?) = add(property, value)

@Suppress("NOTHING_TO_INLINE")
inline operator fun JsonObject.set(property: String, value: String?) = addProperty(property, value)

@Suppress("NOTHING_TO_INLINE")
inline operator fun JsonObject.set(property: String, value: Number?) = addProperty(property, value)

@Suppress("NOTHING_TO_INLINE")
inline operator fun JsonObject.contains(memberName: String) = has(memberName)

fun ByteArray.toBase64() = Base64.getEncoder().encodeToString(this)!!

@Suppress("NOTHING_TO_INLINE")
inline fun String.prefix(other: String) = other + this

private val SHA512 = ThreadLocal.withInitial { MessageDigest.getInstance("SHA-512") }

fun download(file: JsonObject, url: String, dest: Path) {
    val sha512 = SHA512.get()
    Files.copy(DigestInputStream(request(url), sha512), dest, StandardCopyOption.REPLACE_EXISTING)
    if (!sha512.digest().contentEquals(file["hashes"].asJsonObject["sha512"].asString.hexToByteArray())) {
        throw IllegalStateException("Hash mismatch!")
    }
}

private val FORMAT = ThreadLocal.withInitial { MessageFormat("") }

fun ResourceBundle.getString(key: String, vararg args: Any?) = FORMAT.get().let {
    it.locale = locale
    it.applyPattern(getString(key))
    it.format(args)!!
}
