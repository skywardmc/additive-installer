package io.github.teamteds.tedsmodpacksinstaller

import com.google.gson.JsonObject
import java.nio.file.Path

class PackVersion(val modpack: Modpack, val data: JsonObject) {
    val packVersion: String
    val gameVersion: String
    val loader: Loader

    init {
        val versionNumber = data["version_number"].asString
        loader = if ('-' in versionNumber && '+' !in versionNumber) {
            packVersion = versionNumber.substringBefore('-')
            gameVersion = versionNumber.substringAfterLast('-')
            if (versionNumber.count { it == '-' } > 1) {
                versionNumber.substringAfter('-').substringBeforeLast('-')
            } else {
                "FABRIC"
            }
        } else {
            packVersion = versionNumber.substringBefore('+')
            gameVersion = versionNumber.substringAfter('+').substringBeforeLast('.')
            if (versionNumber.substringAfterLast('.') == "fabric") {
                "fabric"
            } else {
                "quilt"
            }
        }.uppercase().let(Loader::valueOf)
    }

    val launcherFolderPath = "${modpack.id}/$packVersion-$gameVersion"
    val launcherVersionId = "${modpack.id}-$packVersion-$gameVersion"
    val launcherProfileId = "${modpack.id}-$gameVersion"

    fun install(destination: Path, progressHandler: ProgressHandler) =
        PackInstaller(this, destination, progressHandler)
            .use(PackInstaller::install)
}
