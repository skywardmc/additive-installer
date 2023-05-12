package io.github.gaming32.additiveinstaller

enum class Loader(val dependencyName: String, val apiRoot: String, val addMods: String) {
    FABRIC("fabric-loader", "https://meta.fabricmc.net/v2", "fabric.addMods"),
    QUILT("quilt-loader", "https://meta.quiltmc.org/v3", "loader.addMods")
}
