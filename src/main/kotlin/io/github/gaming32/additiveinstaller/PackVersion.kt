package io.github.gaming32.additiveinstaller

import com.google.gson.JsonObject
import io.github.gaming32.additiveinstaller.installer.ProgressHandler

class PackVersion(val data: JsonObject) {
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
            versionNumber.substringAfterLast('.')
        }.uppercase().let(Loader::valueOf)
    }

    fun install(progressHandler: ProgressHandler = ProgressHandler.Null) =
        loader.installer.install(this, progressHandler)
}
