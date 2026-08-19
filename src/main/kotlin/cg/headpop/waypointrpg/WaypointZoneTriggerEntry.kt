package cg.headpop.waypointrpg

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.entries.priority
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Max
import com.typewritermc.core.extension.annotations.Min
import com.typewritermc.core.interaction.context
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.AudienceDisplay
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.TickableDisplay
import com.typewritermc.engine.paper.entry.triggerFor
import com.typewritermc.engine.paper.utils.toBukkitLocation
import com.typewritermc.quest.entries.interfaces.LocatableObjective
import com.typewritermc.quest.entries.trackedShowingObjectives
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Which targets the zone trigger monitors.
 *
 * - OBJECTIVES_ONLY    — only location objectives (fastest).
 * - ANY_ACTIVE_TARGET  — objectives + active entity_waypoint entries.
 * - ACTIVE_ROUTE_POINT — triggers at the current route point instead of the final objective position.
 *                        Requires an active waypoint_path for the objective.
 *                        Progress is shared and advances without requiring static_waypoint.
 */
enum class ZoneTriggerTargetMode {
    OBJECTIVES_ONLY,
    ANY_ACTIVE_TARGET,
    ACTIVE_ROUTE_POINT,
}

@Entry(
    "waypoint_zone_trigger",
    "Fire triggers when a player enters or exits a waypoint target's area.",
    Colors.RED,
    "mdi:map-marker-radius"
)
class WaypointZoneTriggerEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("Detection radius in blocks.")
    val radius: Double = 5.0,

    @Help("Which targets to monitor: objectives, entities, or the active route point.")
    val targetMode: ZoneTriggerTargetMode = ZoneTriggerTargetMode.ANY_ACTIVE_TARGET,

    @Help("Maximum number of targets checked at once.")
    @Min(0) @Max(64)
    val maxTargets: Int = 5,

    @Help("Sort order when trimming to maxTargets.")
    val selection: WaypointTargetSelection = WaypointTargetSelection.CLOSEST,

    @Help("Fire a separate event for each target instead of one combined event.")
    val triggerPerTarget: Boolean = false,

    @Help("Fire only once per entry and hold until the player leaves.")
    val triggerOnce: Boolean = false,

    @Help("Reset triggerOnce when the player exits the zone.")
    val resetOnExit: Boolean = true,

    @Help("Trigger on zone entry.")
    val onEnter: Ref<TriggerableEntry> = emptyRef(),

    @Help("Trigger on zone exit or when the target disappears.")
    val onExit: Ref<TriggerableEntry> = emptyRef(),
) : AudienceEntry {
    override suspend fun display(): AudienceDisplay = WaypointZoneTriggerDisplay(this)
}

// --- Per-player state ---

private data class PlayerZoneState(
    val insideKeys: MutableSet<String> = mutableSetOf(),
    val triggeredOnceKeys: MutableSet<String> = mutableSetOf(),
)

// --- Display ---

// Check cadence is internal policy — not user-editable. 5 ticks = 0.25 s, responsive
// without per-tick overhead. Zone checks are cheaper than visual updates but still do
// a distance comparison per target per player; per-tick would waste cycles.
private const val ZONE_CHECK_INTERVAL_TICKS = 5

private class WaypointZoneTriggerDisplay(
    private val entry: WaypointZoneTriggerEntry,
) : AudienceDisplay(), TickableDisplay {

    private val states = ConcurrentHashMap<UUID, PlayerZoneState>()
    private var tickCounter = 0

    override fun onPlayerAdd(player: Player) {
        states[player.uniqueId] = PlayerZoneState()
    }

    override fun onPlayerRemove(player: Player) {
        val state = states.remove(player.uniqueId) ?: return
        if (state.insideKeys.isNotEmpty()) {
            runSync { entry.onExit.triggerFor(player, context()) }
        }
    }

    override fun tick() {
        if (++tickCounter % ZONE_CHECK_INTERVAL_TICKS != 0) return
        runSync {
            if (!isActive) return@runSync
            players.forEach { checkPlayer(it) }
        }
    }

    // --- Target resolution ---

    private fun resolveZoneTargets(player: Player): List<WaypointTarget> {
        return WaypointTargetRegistry.resolve(
            player = player,
            selection = entry.selection,
            maxTargets = entry.maxTargets,
            includeObjectives = true,
            includeEntities = entry.targetMode == ZoneTriggerTargetMode.ANY_ACTIVE_TARGET,
            includePaths = entry.targetMode == ZoneTriggerTargetMode.ACTIVE_ROUTE_POINT,
        )
    }

    // --- Zone check ---

    private fun checkPlayer(player: Player) {
        val state = states.getOrPut(player.uniqueId) { PlayerZoneState() }
        val radiusSq = entry.radius * entry.radius
        val playerLoc = player.location
        val targets = resolveZoneTargets(player)

        // Keys currently inside radius
        val inRadiusKeys: Set<String> = targets
            .filter { it.location.world == playerLoc.world && playerLoc.distanceSquared(it.location) <= radiusSq }
            .map { it.zoneKey() }
            .toSet()

        // Collapse to virtual "§any" key when per-target mode is off
        val effectiveInRadius: Set<String> = if (entry.triggerPerTarget) inRadiusKeys
                                             else if (inRadiusKeys.isNotEmpty()) setOf("§any") else emptySet()

        // Exit: was tracked as inside AND now left radius OR target disappeared
        val toExit = state.insideKeys - effectiveInRadius
        for (key in toExit.toList()) {
            state.insideKeys.remove(key)
            entry.onExit.triggerFor(player, context())
            if (entry.resetOnExit) state.triggeredOnceKeys.remove(key)
        }

        // Enter: now in radius AND not already tracked
        for (key in effectiveInRadius - state.insideKeys) {
            if (entry.triggerOnce && key in state.triggeredOnceKeys) continue
            state.insideKeys.add(key)
            entry.onEnter.triggerFor(player, context())
            if (entry.triggerOnce) state.triggeredOnceKeys.add(key)
        }
    }

    override fun dispose() {
        states.clear()
        super.dispose()
    }

    private fun runSync(block: () -> Unit) {
        if (Bukkit.isPrimaryThread()) block()
        else Bukkit.getScheduler().runTask(
            com.typewritermc.engine.paper.plugin,
            Runnable { block() }
        )
    }
}
