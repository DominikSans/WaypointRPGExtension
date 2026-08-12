package cg.headpop.waypointrpg

import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.plugin
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

private const val RESOURCE_ROOT = "waypoint-resourcepack"
private const val MANAGED_MARKER = ".managed-by-waypoint-rpg"

private val bundledResourcePackFiles = listOf(
    "pack.mcmeta",
    "assets/minecraft/shaders/core/rendertype_text.vsh",
    "assets/minecraft/shaders/core/rendertype_text.fsh",
    "assets/minecraft/shaders/core/rendertype_text_intensity.vsh",
    "assets/minecraft/shaders/core/rendertype_text_intensity.fsh",
    "assets/minecraft/shaders/core/rendertype_text_see_through.vsh",
    "assets/minecraft/shaders/core/rendertype_text_see_through.fsh",
    "assets/minecraft/shaders/core/rendertype_text_intensity_see_through.vsh",
    "assets/minecraft/shaders/core/rendertype_text_intensity_see_through.fsh",
)

private val obsoleteManagedResourcePackFiles = listOf("README.txt", "README.md")

@Singleton
object WaypointResourcePackInitializer : Initializable {
    override suspend fun initialize() {
        val pluginsDirectory = plugin.dataFolder.toPath().parent
            ?: error("Typewriter data folder has no plugins parent directory")
        val destination = pluginsDirectory.resolve("WaypointRPGExtension").resolve("resourcepack")
        installBundledResourcePack(destination)
    }

    override suspend fun shutdown() = Unit

    private fun installBundledResourcePack(destination: Path) {
        val marker = destination.resolve(MANAGED_MARKER)
        val updateManagedFiles = Files.exists(marker)
        bundledResourcePackFiles.forEach { relativePath ->
            val target = destination.resolve(relativePath)
            if (!updateManagedFiles && Files.exists(target)) return@forEach
            val resourcePath = "$RESOURCE_ROOT/$relativePath"
            val input = javaClass.classLoader.getResourceAsStream(resourcePath)
                ?: error("Missing bundled resource: $resourcePath")
            input.use {
                Files.createDirectories(target.parent)
                Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        if (updateManagedFiles) {
            obsoleteManagedResourcePackFiles.forEach { relativePath ->
                Files.deleteIfExists(destination.resolve(relativePath))
            }
        }
        Files.createDirectories(destination)
        Files.writeString(
            marker,
            "Managed shader assets. Point your resource-pack merger at this resourcepack directory.\n"
        )
        plugin.logger.info(
            "[WaypointRPG] Core shader resource pack ready at ${destination.toAbsolutePath()}"
        )
    }
}
