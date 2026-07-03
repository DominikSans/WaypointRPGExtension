package cg.headpop.waypointrpg

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.AudienceDisplay
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.TickableDisplay
import com.typewritermc.engine.paper.plugin
import com.typewritermc.engine.paper.utils.toBukkitLocation
import com.typewritermc.quest.entries.interfaces.LocatableObjective
import com.typewritermc.quest.entries.trackedShowingObjectives
import kr.toxicity.hud.api.BetterHudAPI
import kr.toxicity.hud.api.adapter.LocationWrapper
import kr.toxicity.hud.api.adapter.WorldWrapper
import kr.toxicity.hud.api.player.PointedLocation
import kr.toxicity.hud.api.player.PointedLocationSource
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Entry(
    "waypoint_betterhud_bridge",
    "Sync active waypoint objectives to BetterHUD compass points automatically",
    Colors.CYAN,
    "material-symbols:assistant-navigation"
)
class WaypointBetterHudBridgeEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("BetterHUD icon name configured in your BetterHUD layout for compass points.")
    val iconName: String = "default",

    @Help("Prefix for point names to avoid collision with other plugins. Must be unique per entry.")
    val pointNamePrefix: String = "waypoint_",

    @Help("How often to sync points to BetterHUD in ticks. 20 = once per second.")
    val updateIntervalTicks: Int = 20,
) : AudienceEntry {
    override suspend fun display(): AudienceDisplay = WaypointBetterHudBridgeDisplay(this)
}

private class WaypointBetterHudBridgeDisplay(
    private val entry: WaypointBetterHudBridgeEntry,
) : AudienceDisplay(), TickableDisplay {

    // Maps playerUUID → set of active BetterHUD point names
    private val activePoints = ConcurrentHashMap<UUID, MutableSet<String>>()
    private var tickCounter = 0

    override fun onPlayerAdd(player: Player) {
        activePoints[player.uniqueId] = ConcurrentHashMap.newKeySet()
    }

    override fun onPlayerRemove(player: Player) {
        removeAllPoints(player)
        activePoints.remove(player.uniqueId)
    }

    override fun tick() {
        if (++tickCounter % entry.updateIntervalTicks.coerceAtLeast(1) != 0) return
        runSync {
            if (!isActive) return@runSync
            players.forEach { syncPoints(it) }
        }
    }

    private fun syncPoints(player: Player) {
        if (!isBetterHudAvailable()) return
        val hudPlayer = BetterHudAPI.inst().playerManager.getHudPlayer(player.uniqueId) ?: return

        // Build desired point set from current active objectives.
        // Point name includes entry.id and objective.id to prevent collisions when multiple
        // bridge entries share the same prefix or a player has multiple active objectives.
        val desiredPoints = mutableMapOf<String, PointedLocation>()
        var index = 0
        player.trackedShowingObjectives()
            .filterIsInstance<LocatableObjective>()
            .forEach { objective ->
                objective.positions(player).forEach { position ->
                    val loc = runCatching { position.toBukkitLocation() }.getOrNull() ?: return@forEach
                    val worldName = loc.world?.name ?: return@forEach
                    val pointName = "${entry.pointNamePrefix}${entry.id}_${objective.id}_$index"
                    desiredPoints[pointName] = PointedLocation(
                        PointedLocationSource.INTERNAL,
                        pointName,
                        entry.iconName,
                        LocationWrapper(WorldWrapper(worldName), loc.x, loc.y, loc.z, 0f, 0f)
                    )
                    index++
                }
            }

        val current = activePoints.getOrPut(player.uniqueId) { ConcurrentHashMap.newKeySet() }

        val toRemove = current.filter { it !in desiredPoints }
        val toAdd = desiredPoints.filter { (name, _) -> name !in current }

        if (toRemove.isEmpty() && toAdd.isEmpty()) return

        toRemove.forEach { name ->
            hudPlayer.pointers().removeIf { it.name == name }
            current.remove(name)
        }
        toAdd.forEach { (name, point) ->
            hudPlayer.pointers().add(point)
            current.add(name)
        }
        hudPlayer.update()
    }

    private fun removeAllPoints(player: Player) {
        if (!isBetterHudAvailable()) return
        val current = activePoints[player.uniqueId]?.takeIf { it.isNotEmpty() } ?: return
        val hudPlayer = BetterHudAPI.inst().playerManager.getHudPlayer(player.uniqueId) ?: return
        current.forEach { name -> hudPlayer.pointers().removeIf { it.name == name } }
        hudPlayer.update()
        current.clear()
    }

    override fun dispose() {
        players.forEach { removeAllPoints(it) }
        activePoints.clear()
        super.dispose()
    }

    private fun isBetterHudAvailable(): Boolean = runCatching {
        val p = Bukkit.getPluginManager().getPlugin("BetterHud") ?: return@runCatching false
        p.isEnabled
    }.getOrDefault(false)

    private fun runSync(block: () -> Unit) {
        if (Bukkit.isPrimaryThread()) block()
        else Bukkit.getScheduler().runTask(plugin, Runnable { block() })
    }
}
