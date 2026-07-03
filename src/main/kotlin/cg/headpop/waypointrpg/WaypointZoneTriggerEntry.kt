package cg.headpop.waypointrpg

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.interaction.context
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.AudienceDisplay
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.TickableDisplay
import com.typewritermc.engine.paper.entry.triggerFor
import com.typewritermc.engine.paper.plugin
import com.typewritermc.engine.paper.utils.toBukkitLocation
import com.typewritermc.quest.entries.interfaces.LocatableObjective
import com.typewritermc.quest.entries.trackedShowingObjectives
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Entry(
    "waypoint_zone_trigger",
    "Fire Typewriter triggers when player enters or exits the current waypoint objective radius",
    Colors.YELLOW,
    "material-symbols:radio-button-checked"
)
class WaypointZoneTriggerEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("Radius in blocks. Player entering any active waypoint target within this distance fires onEnter.")
    val radius: Double = 5.0,

    @Help("Check rate in ticks. Lower = more responsive but more CPU.")
    val checkIntervalTicks: Int = 10,

    @Help("Trigger fired when player enters the waypoint radius.")
    val onEnter: Ref<TriggerableEntry> = emptyRef(),

    @Help("Trigger fired when player exits the waypoint radius (or objective changes).")
    val onExit: Ref<TriggerableEntry> = emptyRef(),
) : AudienceEntry {
    override suspend fun display(): AudienceDisplay = WaypointZoneTriggerDisplay(this)
}

private class WaypointZoneTriggerDisplay(
    private val entry: WaypointZoneTriggerEntry,
) : AudienceDisplay(), TickableDisplay {

    private val insideStates = ConcurrentHashMap<UUID, Boolean>()
    private var tickCounter = 0

    override fun onPlayerAdd(player: Player) {
        insideStates[player.uniqueId] = false
    }

    override fun onPlayerRemove(player: Player) {
        val wasInside = insideStates.remove(player.uniqueId) ?: false
        if (wasInside) {
            runSync { entry.onExit.triggerFor(player, context()) }
        }
    }

    override fun tick() {
        val interval = entry.checkIntervalTicks.coerceAtLeast(1)
        if (++tickCounter % interval != 0) return
        runSync {
            if (!isActive) return@runSync
            players.forEach { checkPlayer(it) }
        }
    }

    private fun checkPlayer(player: Player) {
        val radiusSq = entry.radius * entry.radius
        val playerLoc = player.location

        val inRadius = player.trackedShowingObjectives()
            .filterIsInstance<LocatableObjective>()
            .any { obj ->
                obj.positions(player).any { position ->
                    runCatching {
                        val loc = position.toBukkitLocation() ?: return@runCatching false
                        loc.world == playerLoc.world && playerLoc.distanceSquared(loc) <= radiusSq
                    }.getOrDefault(false)
                }
            }

        val wasInside = insideStates[player.uniqueId] ?: false
        insideStates[player.uniqueId] = inRadius

        when {
            inRadius && !wasInside -> entry.onEnter.triggerFor(player, context())
            !inRadius && wasInside -> entry.onExit.triggerFor(player, context())
        }
    }

    override fun dispose() {
        insideStates.clear()
        super.dispose()
    }

    private fun runSync(block: () -> Unit) {
        if (Bukkit.isPrimaryThread()) block()
        else Bukkit.getScheduler().runTask(plugin, Runnable { block() })
    }
}
