package io.github.gaming32.additiveinstaller

import com.google.gson.JsonArray
import java.nio.file.Path

enum class Loader(val dependencyName: String, val apiRoot: String, val addMods: JsonArray.(Path) -> Unit) {
    FABRIC("fabric-loader", "https://meta.fabricmc.net/v2", {
        add("--fabric.addMods")
        add("$it")
    }),
    QUILT("quilt-loader", "https://meta.quiltmc.org/v3", {
        add("--loader.addMods")
        add("$it/*")
    })
}
