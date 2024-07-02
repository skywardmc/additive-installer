import de.undercouch.gradle.tasks.download.Download

plugins {
    application
    kotlin("jvm") version "2.0.0"
    id("net.raphimc.class-token-replacer") version "1.1.2"
    id("de.undercouch.download") version "5.6.0"
}

group = "io.github.gaming32"
version = "1.0.3"

application {
    mainClass.set("io.github.gaming32.additiveinstaller.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")

    implementation("com.google.jimfs:jimfs:1.3.0")

    implementation("com.formdev:flatlaf:3.4.1")

    implementation("com.google.code.gson:gson:2.11.0")

    implementation("io.github.z4kn4fein:semver-jvm:2.0.0")
}

sourceSets.main {
    classTokenReplacer {
        property("<<VERSION>>", version)
    }
}

kotlin {
    jvmToolchain(8)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Multi-Release"] = true
        attributes["SplashScreen-Image"] = "splash.png"
    }
    from(configurations.runtimeClasspath.get().files.map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.WARN
}

val bootstrapVersion = "0.5.2"
val bootstrapArch = "i686"

val downloadBootstrap by tasks.registering(Download::class) {
    src("https://maven.fabricmc.net/net/fabricmc/fabric-installer-native-bootstrap/windows-$bootstrapArch/$bootstrapVersion/windows-$bootstrapArch-$bootstrapVersion.exe")
    dest(project.layout.buildDirectory.get().dir("native-bootstrap"))
}

abstract class NativeExeTask : DefaultTask() {
    @get:InputFile
    abstract val bootstrapFile: RegularFileProperty

    @get:InputFile
    abstract val jarFile: RegularFileProperty

    @get:OutputFile
    abstract val output: RegularFileProperty
}

val nativeExe by tasks.registering(NativeExeTask::class) {
    dependsOn(downloadBootstrap)
    dependsOn(tasks.jar)

    bootstrapFile = downloadBootstrap.get().outputFiles.first()
    jarFile = tasks.jar.get().archiveFile
    output = file("$projectDir/build/libs/${base.archivesName.get()}-${project.version}.exe")

    doFirst {
        output.get().asFile.delete()
    }

    doLast {
        val outputFile = output.get().asFile
        outputFile.createNewFile()
        outputFile.writeBytes(downloadBootstrap.get().outputFiles.first().readBytes())
        outputFile.appendBytes(tasks.jar.get().archiveFile.get().asFile.readBytes())
    }
}

tasks.assemble.get().dependsOn(nativeExe)
