plugins {
    application
    kotlin("jvm") version "1.8.21"
    id("io.ktor.plugin") version "2.1.1" // It builds fat JARs
    id("net.kyori.blossom") version "1.3.1"
}

group = "io.github.gaming32"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("io.github.gaming32.additiveinstaller.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("io.github.oshai:kotlin-logging-jvm:4.0.0-beta-22")

    implementation("com.google.jimfs:jimfs:1.2")

    implementation("com.formdev:flatlaf:3.1.1")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation("io.github.z4kn4fein:semver:1.4.2")
}

blossom {
    replaceToken("<<VERSION>>", project.version, "src/main/kotlin/io/github/gaming32/additiveinstaller/versionHolder.kt")
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
}
