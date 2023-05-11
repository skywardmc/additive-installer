package io.github.gaming32.additiveinstaller

enum class Loader(val dependencyName: String, val apiRoot: String) {
    FABRIC("fabric-loader", "https://meta.fabricmc.net/v2"),
    QUILT("quilt-loader", "https://meta.quiltmc.org/v3")
}
