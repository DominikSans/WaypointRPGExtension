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
 * - ANY_ACTIVE_TARGET  — objectives + entity targets from [entityTargets] list.
 * - ACTIVE_ROUTE_POINT — triggers at the current route point instead of the final objective position.
 *                        Requires [routes] to be configured with matching objectiveId.
 *                        Route index advances only when the visual tracked_locatable_waypoint entry is present.
 */
enum class ZoneTriggerTargetMode {
    OBJECTIVES_ONLY,
    ANY_ACTIVE_TARGET,
    ACTIVE_ROUTE_POINT,
}

@Entry(
    "waypoint_zone_trigger",
    "Fire Typewriter triggers when a player enters or exits the radius of active waypoint targets (objectives, entity targets, Typewriter NPCs).",
    Colors.YELLOW,
    "material-symbols:radio-button-checked"
)
class WaypointZoneTriggerEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("Radius in blocks. Player within this distance of any active target fires onEnter.")
    val radius: Double = 5.0,

    @Help("Check interval in ticks. 5 = responsive; 20 = cheap. Minimum 1.")
    @Min(1) @Max(100)
    val checkIntervalTicks: Int = 5,

    @Help("Which target pool to check: OBJECTIVES_ONLY (tracked quest objectives) or ANY_ACTIVE_TARGET (objectives + entityTargets list below).")
    val targetMode: ZoneTriggerTargetMode = ZoneTriggerTargetMode.ANY_ACTIVE_TARGET,

    @Help("Maximum targets to consider simultaneously. Matches general.maxTargets in tracked_locatable_waypoint. 0 = disabled.")
    @Min(0) @Max(64)
    val maxTargets: Int = 5,

    @Help("How targets are sorted before applying maxTargets: CLOSEST picks nearest, HIGHEST_PRIORITY picks by objective/entity priority.")
    val selection: WaypointTargetSelection = WaypointTargetSelection.CLOSEST,

    @Help("If true, each target key fires its own onEnter/onExit event. If false, fires once when any target enters/exits.")
    val triggerPerTarget: Boolean = false,

    @Help("If true, onEnter fires only once per target key until the player exits and resetOnExit is true.")
    val triggerOnce: Boolean = false,

    @Help("If true, exiting a zone resets the triggerOnce guard so the player can re-trigger on next entry.")
    val resetOnExit: Boolean = true,

    @Help("Extra entity/NPC targets to monitor. Same format as integrations.entityTargets in tracked_locatable_waypoint. Requires targetMode = ANY_ACTIVE_TARGET.")
    val entityTargets: List<EntityWaypointTarget> = emptyList(),

    @Help("Route definitions for ACTIVE_ROUTE_POINT mode. Configure the same routes as in your tracked_locatable_waypoint entry so zone trigger and visual share the same route state (via globalRouteIndices).")
    val routes: List<WaypointRoute> = emptyList(),

    @Help("Trigger fired when player enters a target's radius (or any target when triggerPerTarget = false).")
    val onEnter: Ref<TriggerableEntry> = emptyRef(),

    @Help("Trigger fired when player exits a target's radius or the target disappears.")
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
        val interval = entry.checkIntervalTicks.coerceAtLeast(1)
        if (++tickCounter % interval != 0) return
        runSync {
            if (!isActive) return@runSync
            players.forEach { checkPlayer(it) }
        }
    }

    // --- Target resolution ---

    private fun resolveZoneTargets(player: Player): List<WaypointTarget> {
        val raw = resolveWaypointTargets(
            player = player,
            selection = entry.selection,
            maxTargets = entry.maxTargets,
            entityTargets = entry.entityTargets,
            includeObjectives = true,
            includeEntities = entry.targetMode == ZoneTriggerTargetMode.ANY_ACTIVE_TARGET,
        )
        if (entry.targetMode != ZoneTriggerTargetMode.ACTIVE_ROUTE_POINT || entry.routes.isEmpty()) return raw
        return raw.map { target ->
            val objectiveId = target.objective?.id ?: return@map target
            applyRouteReadOnly(player, objectiveId, entry.routes, target)
        }
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
