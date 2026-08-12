plugins {
    kotlin("jvm") version "2.2.10"
    id("com.typewritermc.module-plugin") version "2.1.0"
}

group = "cg.headpop"
version = "1.0.0"

val twEngineVersion = providers.gradleProperty("twEngineVersion")
    .orElse("0.9.0-beta-173")
    .get()

repositories {
    mavenCentral()
    maven("https://maven.typewritermc.com/releases")
    maven("https://maven.typewritermc.com/beta")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://maven.typewritermc.com/external/")
}

// Published Typewriter artifacts may pull a broken floating EntityLib snapshot.
// This extension does not use EntityLib directly; Typewriter provides runtime classes on the server.
configurations.configureEach {
    exclude(group = "me.tofaa.entitylib", module = "spigot")
}

dependencies {
    // Local jars are used only for compilation and IDE completion.
    compileOnly(fileTree("libs") { include("*.jar") })
    // PacketEvents is provided at runtime by Typewriter; compileOnly for beam packet API.
    compileOnly("com.github.retrooper:packetevents-spigot:2.9.4")
    // BetterHUD is accessed reflectively by its optional bridge, so it is not a load-time dependency.
}

typewriter {
    namespace = "headpop"

    extension {
        name = "WaypointRPG"
        shortDescription = "Beam and text waypoint markers for tracked quest objectives"
        description = """
            WaypointRPGExtension adds a Typewriter AudienceEntry that renders a personal waypoint for
            each player's active quest objectives. A vertical beacon beam marks the location and smoothly
            follows the player at range. A floating text label shows name, distance, and direction with
            full MiniMessage support. An optional scaling icon hovers above the text. All visuals use
            Display Entities (TextDisplay / BlockDisplay) and are visible only to the individual player.
            Includes zone triggers, BetterHUD compass integration, and guided route waypoints.
        """.trimIndent()
        engineVersion = twEngineVersion
        channel = com.typewritermc.moduleplugin.ReleaseChannel.BETA

        dependencies {
            dependency("typewritermc", "Quest")
            dependency("typewritermc", "Entity")
        }

        paper()
    }
}

kotlin {
    jvmToolchain(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    archiveBaseName.set("WaypointRPGExtension-${twEngineVersion}")
    archiveVersion.set("")
}
