plugins {
    kotlin("jvm") version "2.2.10"
    id("com.typewritermc.module-plugin") version "2.1.0"
}

group = "cg.headpop"
version = "1.0.0"

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
    // BetterHUD integration — optional at runtime; entry gracefully no-ops if plugin absent.
    compileOnly("io.github.toxicity188:BetterHud-standard-api:1.14.1")
    compileOnly("io.github.toxicity188:BetterHud-bukkit-api:1.14.1")
    // BetterCommand is a runtime-scoped transitive dep of BetterHud-standard-api; needed for compile.
    compileOnly("io.github.toxicity188:BetterCommand:1.4.3")
}

typewriter {
    namespace = "waypointrpg"

    extension {
        name = "HeadPop"
        shortDescription = "Quest waypoint markers with beacon beam and hologram for tracked objectives"
        description = """
            Created by Dominik (cg.headpop).
            Adds a Typewriter AudienceEntry that shows each player a personal visual marker toward their
            currently tracked locatable quest objective. Features a vertical beacon beam that stays fixed
            at the objective when close and follows the player at a comfortable distance when far, with
            smooth thinning on approach. Hologram text supports MiniMessage, newlines, and a subtle bob
            animation. Manual route waypoints guide players along paths instead of through walls.
            All visuals use TextDisplay and BlockDisplay, visible only to the specific player.
        """.trimIndent()
        engineVersion = "0.9.0-beta-173"
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
    archiveBaseName.set("WaypointRPGExtension")
}
