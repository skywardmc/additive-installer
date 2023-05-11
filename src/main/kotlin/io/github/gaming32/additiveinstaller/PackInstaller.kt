package io.github.gaming32.additiveinstaller

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.oshai.KotlinLogging
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

class PackInstaller(
    private val packVersion: PackVersion, private  val progressHandler: ProgressHandler
) : AutoCloseable {
    companion object {
        val DOT_MINECRAFT = Path(
            when (operatingSystem) {
                OperatingSystem.WINDOWS -> "${System.getenv("APPDATA")}\\.minecraft"
                OperatingSystem.MACOS -> "${System.getProperty("user.home")}/Library/Application Support/minecraft"
                else -> "${System.getProperty("user.home")}/.minecraft"
            }
        )
        val VERSIONS = DOT_MINECRAFT / "versions"
        val LAUNCHER_PROFILES = DOT_MINECRAFT / "launcher_profiles.json"
    }

    private val jimfs = Jimfs.newFileSystem(Configuration.unix())!!
    private lateinit var zfs: FileSystem

    private lateinit var packIndex: JsonObject

    private  val modsDir = DOT_MINECRAFT / packVersion.launcherVersionId

    @OptIn(ExperimentalPathApi::class)
    private  fun writeVersionDir(clientJson: JsonObject) {
        progressHandler.newTask("Creating version folder")
        val versionDir = VERSIONS / packVersion.launcherVersionId
        versionDir.deleteRecursively()
        versionDir.createDirectories()

        progressHandler.newTask("Writing client.json")
        versionDir.resolve("${packVersion.launcherVersionId}.json").writer().use(clientJson::writeTo)

        progressHandler.newTask("Writing placeholder client.jar")
        versionDir.resolve("${packVersion.launcherVersionId}.jar").createFile()
    }

    private  fun updateLauncherProfiles() {
        progressHandler.newTask("Reading launcher_profiles.json")
        val launcherProfiles = LAUNCHER_PROFILES.reader().use(JsonParser::parseReader).asJsonObject

        progressHandler.newTask("Patching launcher_profiles.json")
        val profile = launcherProfiles["profiles"]
            .asJsonObject[packVersion.launcherProfileId]
            ?.asJsonObject
            ?: JsonObject()
        if ("created" !in profile) {
            profile["created"] = isoTime()
        }
        if ("icon" !in profile) {
            packVersion.modpack.launcherIcon?.let { profile["icon"] = it }
        }
        profile["lastUsed"] = isoTime()
        if ("name" !in profile) {
            profile["name"] = "${packVersion.modpack.name} ${packVersion.gameVersion}"
        }
        profile["lastVersionId"] = packVersion.launcherVersionId
        if ("type" !in profile) {
            profile["type"] = "custom"
        }
        launcherProfiles["profiles"].asJsonObject.add(packVersion.launcherProfileId, profile)

        progressHandler.newTask("Writing launcher_profiles.json")
        LAUNCHER_PROFILES.writer().use(launcherProfiles::writeTo)
    }

    private fun downloadPack() {
        progressHandler.newTaskSet(3)

        progressHandler.newTask("Downloading pack")
        val files = packVersion.data["files"].asJsonArray
        val file = files.asSequence()
            .map { it.asJsonObject }
            .firstOrNull { it["primary"].asBoolean }
            ?: files[0].asJsonObject
        val jfsPath = jimfs.getPath(file["filename"].asString)
        download(file, file["url"].asString, jfsPath)

        progressHandler.newTask("Opening pack")
        zfs = FileSystems.newFileSystem(URI("jar:${jfsPath.toUri()}!/"), mapOf<String, String>())

        progressHandler.newTask("Reading index")
        packIndex = zfs.getPath("modrinth.index.json").reader().use(JsonParser::parseReader).asJsonObject
        if (packIndex["dependencies"].asJsonObject["minecraft"].asString != packVersion.gameVersion) {
            throw IllegalStateException("Game version mismatch!")
        }
    }

    private fun installLoader() {
        val gameVersion = packVersion.gameVersion

        progressHandler.newTaskSet(8)

        val loaderVersion = packIndex["dependencies"].asJsonObject[packVersion.loader.dependencyName].asString
        logger.info("Using ${packVersion.loader.dependencyName} $loaderVersion")

        progressHandler.newTask("Downloading client.json")
        val clientJson = requestJson(
            "${packVersion.loader.apiRoot}/versions/loader/$gameVersion/$loaderVersion/profile/json"
        ).asJsonObject

        progressHandler.newTask("Patching client.json")
        clientJson["id"] = packVersion.launcherVersionId
        packVersion.loader.addMods(clientJson["arguments"].asJsonObject["game"].asJsonArray, modsDir)

        writeVersionDir(clientJson)
        updateLauncherProfiles()
    }

    @OptIn(ExperimentalPathApi::class)
    private fun installPack() {
        progressHandler.prepareNewTaskSet("Downloading mods")

        val files = packIndex["files"].asJsonArray
        modsDir.deleteRecursively()

        progressHandler.newTaskSet(files.size())

        files.asSequence().map(JsonElement::getAsJsonObject).forEach { file ->
            val path = file["path"].asString
            progressHandler.newTask("Downloading $path")
            val (destRoot, dest) = if (path.startsWith("mods/")) {
                Pair(modsDir, modsDir / path.substring(5))
            } else {
                Pair(DOT_MINECRAFT, DOT_MINECRAFT / path)
            }
            if (!dest.startsWith(destRoot)) {
                throw IllegalArgumentException("Path doesn't start with mods dir?")
            }
            dest.parent.createDirectories()
            download(file, file["downloads"].asJsonArray.first().asString, dest)
        }

        progressHandler.prepareNewTaskSet("Extracting overrides")

        val overridesDir = zfs.getPath("overrides")
        val overrides = overridesDir.walk().toList()

        progressHandler.newTaskSet(overrides.size)

        for (override in overrides) {
            val relative = override.relativeTo(overridesDir).toString()
            progressHandler.newTask("Extracting $relative")
            val dest = DOT_MINECRAFT / relative
            dest.parent.createDirectories()
            override.copyTo(dest, true)
        }
    }

    fun install() {
        downloadPack()
        installLoader()
        installPack()
        progressHandler.done()
    }

    override fun close() {
        if (this::zfs.isInitialized) {
            zfs.close()
        }
        jimfs.close()
    }
}