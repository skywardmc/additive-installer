package io.github.gaming32.additiveinstaller

import io.github.gaming32.additiveinstaller.installer.FabricInstaller
import io.github.gaming32.additiveinstaller.installer.PackInstaller
import io.github.gaming32.additiveinstaller.installer.QuiltInstaller

enum class Loader(val installer: PackInstaller) {
    FABRIC(FabricInstaller), QUILT(QuiltInstaller)
}
