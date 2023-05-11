package io.github.gaming32.additiveinstaller.installer

import io.github.gaming32.additiveinstaller.PackVersion

sealed class PackInstaller {
    protected var progressHandler: ProgressHandler = ProgressHandler.Null
        private set

    protected abstract fun installLoader(minecraftVersion: String)

    private fun installPack(packVersion: PackVersion) {
        progressHandler.prepareNewTaskSet("Installing pack...")
    }

    fun install(
        packVersion: PackVersion,
        progressHandler: ProgressHandler = ProgressHandler.Null
    ) {
        this.progressHandler = progressHandler
        installLoader(packVersion.gameVersion)
        installPack(packVersion)
        progressHandler.done()
    }
}
