package io.github.teamteds.tedsmodpacksinstaller

import com.google.gson.JsonElement
import javax.imageio.ImageIO

private const val PROJECT_BASE = "https://api.modrinth.com/v2/project"

class Modpack(val id: String) {
    val versions = requestCriticalJson("$PROJECT_BASE/$id/version").asJsonArray
        .asSequence()
        .map(JsonElement::getAsJsonObject)
        .map { PackVersion(this, it) }
        .run {
            val result = mutableMapOf<String, MutableMap<String, PackVersion>>()
            for (version in this) {
                val game = result.getOrPut(version.gameVersion, ::mutableMapOf)
                game[version.packVersion] = maxOf(
                    game[version.packVersion],
                    version,
                    Comparator.comparingInt { it?.loader?.ordinal ?: -1 }
                )!!
            }
            @Suppress("USELESS_CAST") // It's a cast to an immutable interface, Kotlin. That's not unnecessary (imo).
            result as Map<String, Map<String, PackVersion>>
        }

    val name = id.capitalize()
    val windowTitle = I18N.getString("window.title", name)
    val image = ImageIO.read(javaClass.getResource("/${id}96.png"))!!
    val launcherIcon = javaClass.getResource("/${id}32.png")
        ?.readBytes()
        ?.toBase64()
        ?.prefix("data:image/png;base64,")
    val supportedMcVersions = buildSet {
        for ((key, value) in versions) {
            if (value.values.any { it.data["featured"].asBoolean }) {
                add(key)
            }
        }
    }
}
