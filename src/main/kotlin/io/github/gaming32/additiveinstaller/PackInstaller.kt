package io.github.gaming32.additiveinstaller

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.oshai.KotlinLogging
import io.github.z4kn4fein.semver.toVersion
import java.io.IOException
import java.net.URI
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

class PackInstaller(
    private val packVersion: PackVersion, private val destination: Path, private  val progressHandler: ProgressHandler
) : AutoCloseable {
    companion object {
        const val YOSBR_ID = "WwbubTsV"
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

    private val modsDir = DOT_MINECRAFT / packVersion.launcherFolderPath

    @OptIn(ExperimentalPathApi::class)
    private  fun writeVersionDir(clientJson: JsonObject) {
        progressHandler.newTask(I18N.getString("creating.version.folder"))
        val versionDir = VERSIONS / packVersion.launcherVersionId
        versionDir.deleteRecursively()
        versionDir.createDirectories()

        progressHandler.newTask(I18N.getString("writing.client.json"))
        versionDir.resolve("${packVersion.launcherVersionId}.json").writer().use(clientJson::writeTo)

        progressHandler.newTask(I18N.getString("writing.placeholder.client.jar"))
        versionDir.resolve("${packVersion.launcherVersionId}.jar").createFile()
    }

    private  fun updateLauncherProfiles() {
        progressHandler.newTask(I18N.getString("reading.launcher.profiles.json"))
        val launcherProfiles = LAUNCHER_PROFILES.reader().use(JsonParser::parseReader).asJsonObject

        progressHandler.newTask(I18N.getString("patching.launcher.profiles.json"))
        val profile = launcherProfiles["profiles"]
            .asJsonObject[packVersion.launcherProfileId]
            ?.asJsonObject
            ?: JsonObject()
        if ("created" !in profile) {
            profile["created"] = isoTime()
        }
        if (destination != DOT_MINECRAFT) {
            profile["gameDir"] = destination.toString()
        }
        if ("icon" !in profile) {
            packVersion.modpack.launcherIcon?.let { profile["icon"] = it }
        }
        profile["lastUsed"] = isoTime()
        if ("name" !in profile) {
            profile["name"] = I18N.getString("profile.name", packVersion.modpack.name, packVersion.gameVersion)
        }
        profile["lastVersionId"] = packVersion.launcherVersionId
        if ("type" !in profile) {
            profile["type"] = "custom"
        }
        launcherProfiles["profiles"].asJsonObject.add(packVersion.launcherProfileId, profile)

        progressHandler.newTask(I18N.getString("writing.launcher.profiles.json"))
        LAUNCHER_PROFILES.writer().use(launcherProfiles::writeTo)
    }

    private fun downloadPack() {
        progressHandler.newTaskSet(3)

        progressHandler.newTask(I18N.getString("downloading.pack"))
        val files = packVersion.data["files"].asJsonArray
        val file = files.asSequence()
            .map { it.asJsonObject }
            .firstOrNull { it["primary"].asBoolean }
            ?: files[0].asJsonObject
        val jfsPath = jimfs.getPath(file["filename"].asString)
        download(file, file["url"].asString, jfsPath)

        progressHandler.newTask(I18N.getString("opening.pack"))
        zfs = FileSystems.newFileSystem(URI("jar:${jfsPath.toUri()}!/"), mapOf<String, String>())

        progressHandler.newTask(I18N.getString("reading.index"))
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

        progressHandler.newTask(I18N.getString("downloading.client.json"))
        val clientJson = requestJson(
            "${packVersion.loader.apiRoot}/versions/loader/$gameVersion/$loaderVersion/profile/json"
        ).asJsonObject

        progressHandler.newTask(I18N.getString("patching.client.json"))
        clientJson["id"] = packVersion.launcherVersionId
        packVersion.loader.addMods(loaderVersion.toVersion()).let { (prefix, suffix) ->
            clientJson["arguments"]
                .asJsonObject
                .asMap()
                .getOrPut("jvm", ::JsonArray)
                .asJsonArray
                .add("-D$prefix=$modsDir$suffix")
        }

        writeVersionDir(clientJson)
        updateLauncherProfiles()
    }

    @OptIn(ExperimentalPathApi::class)
    private fun installPack() {
        progressHandler.prepareNewTaskSet(I18N.getString("downloading.mods"))

        val files = packIndex["files"].asJsonArray
        modsDir.deleteRecursively()

        progressHandler.newTaskSet(files.size())

        files.asSequence().map(JsonElement::getAsJsonObject).forEach { file ->
            val path = file["path"].asString
            progressHandler.newTask(I18N.getString("downloading.file", path))
            val (destRoot, dest) = if (path.startsWith("mods/")) {
                Pair(modsDir, modsDir / path.substring(5))
            } else {
                Pair(destination, destination / path)
            }
            if (!dest.startsWith(destRoot)) {
                throw IllegalArgumentException("Path doesn't start with mods dir?")
            }
            dest.parent.createDirectories()
            val downloadUrl = file["downloads"].asJsonArray.first().asString
            if ("/$YOSBR_ID/" in downloadUrl && "yosbr" in file["path"].asString) {
                logger.info("Skipping yosbr")
                return@forEach
            }
            download(file, downloadUrl, dest)
        }

        progressHandler.prepareNewTaskSet(I18N.getString("extracting.overrides"))

        val overridesDir = zfs.getPath("/overrides")
        val overrides = overridesDir.walk().toList()

        progressHandler.newTaskSet(overrides.size)

        for (override in overrides) {
            var relative = override.relativeTo(overridesDir).toString()
            var overwrite = true
            if (relative.startsWith("config/yosbr/")) {
                relative = relative.substring(13)
                overwrite = false
                logger.info("Override $relative is in yosbr")
            }
            progressHandler.newTask(I18N.getString("extracting.override", relative))
            val dest = destination / relative
            try {
                dest.parent.createDirectories()
            } catch (_: IOException) {
            }
            try {
                override.copyTo(dest, overwrite)
            } catch (_: FileAlreadyExistsException) {
                logger.info("Skipping override $relative because it was in yosbr and the file already exists")
            }
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
