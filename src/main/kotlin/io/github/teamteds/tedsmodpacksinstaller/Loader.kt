package io.github.gaming32.additiveinstaller

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion

enum class Loader(val dependencyName: String, val apiRoot: String, val addMods: (Version) -> Pair<String, String>) {
    FABRIC("fabric-loader", "https://meta.fabricmc.net/v2", { Pair("fabric.addMods", "") }),
    QUILT(
        "quilt-loader", "https://meta.quiltmc.org/v3",
        {
            Pair(
                if (it >= "0.15.0".toVersion()) "loader.addMods" else "fabric.addMods",
                if (it >= "0.18.1-beta.13".toVersion()) "/*" else ""
            )
        }
    )
}
