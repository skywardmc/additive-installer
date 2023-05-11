package io.github.gaming32.additiveinstaller

import com.google.gson.JsonElement
import javax.imageio.ImageIO

private const val PROJECT_BASE = "https://api.modrinth.com/v2/project"

class Modpack(id: String) {
    val versions = requestJson("$PROJECT_BASE/$id/version").asJsonArray
        .asSequence()
        .map(JsonElement::getAsJsonObject)
        .map(::PackVersion)
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

    val windowTitle = "${id.capitalize()} Installer"
    val image = ImageIO.read(javaClass.getResource("/$id.png"))!!
}
