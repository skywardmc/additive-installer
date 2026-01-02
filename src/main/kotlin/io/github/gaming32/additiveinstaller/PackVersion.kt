package io.github.gaming32.additiveinstaller

import com.google.gson.JsonObject
import java.nio.file.Path
import java.time.Instant

class PackVersion(val modpack: Modpack, val data: JsonObject) {
    val packVersion: String
    val gameVersion: String
    val loader: Loader
    val datePublished: Instant

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
            versionNumber.substringAfterLast('.')
        }.uppercase().let(Loader::valueOf)
        datePublished = Instant.parse(data["date_published"].asString)
    }

    val launcherFolderPath = "${modpack.id}/$packVersion-$gameVersion-$loader"
    val launcherVersionId = "${modpack.id}-$packVersion-$gameVersion-$loader"
    val launcherProfileId = "${modpack.id}-$gameVersion-$loader"

    fun install(destination: Path, progressHandler: ProgressHandler) =
        PackInstaller(this, destination, progressHandler)
            .use(PackInstaller::install)
}
