package io.github.gaming32.additiveinstaller

import com.google.gson.JsonElement
import java.util.*
import javax.imageio.ImageIO

private const val PROJECT_BASE = "https://api.modrinth.com/v2/project"

class Modpack(val id: String) {
    val versions: Map<String, Map<String, Map<Loader, PackVersion>>> =
        requestCriticalJson("$PROJECT_BASE/$id/version").asJsonArray
            .asSequence()
            .map(JsonElement::getAsJsonObject)
            .map { PackVersion(this, it) }
            .run {
                val result = mutableMapOf<String, MutableMap<String, MutableMap<Loader, PackVersion>>>()
                for (version in this) {
                    val byPackVersion = result.getOrPut(version.gameVersion, ::mutableMapOf)
                    val byLoader = byPackVersion.getOrPut(version.packVersion) { EnumMap(Loader::class.java) }
                    byLoader[version.loader] = version
                }
                result
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
            if (value.values.asSequence().flatMap { it.values }.any(PackVersion::isSupported)) {
                add(key)
            }
        }
    }
}
