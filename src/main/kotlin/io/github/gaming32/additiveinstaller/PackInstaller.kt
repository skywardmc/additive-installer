package io.github.gaming32.additiveinstaller

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.oshai.KotlinLogging
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.security.DigestInputStream
import java.security.MessageDigest
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
        progressHandler.newTaskSet(1)
        progressHandler.newTask("Downloading pack")

        val files = packVersion.data["files"].asJsonArray
        val file = files.asSequence()
            .map { it.asJsonObject }
            .firstOrNull { it["primary"].asBoolean }
            ?: files[0].asJsonObject
        val jfsPath = jimfs.getPath(file["filename"].asString)

        val digest = MessageDigest.getInstance("SHA-512")
        Files.copy(DigestInputStream(request(file["url"].asString), digest), jfsPath)
        if (!digest.digest().contentEquals(file["hashes"].asJsonObject["sha512"].asString.hexToByteArray())) {
            throw IllegalStateException("Hash mismatch!")
        }

        zfs = FileSystems.newFileSystem(URI("jar:${jfsPath.toUri()}!/"), mapOf<String, String>())

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

    private fun installPack() {
        progressHandler.prepareNewTaskSet("Installing pack")
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