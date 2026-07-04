package cg.headpop.waypointrpg

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Colored
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Max
import com.typewritermc.core.extension.annotations.Min
import com.typewritermc.core.extension.annotations.Placeholder
import com.typewritermc.engine.paper.entry.entries.AudienceDisplay
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.TickableDisplay
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.plugin
import kr.toxicity.hud.api.BetterHudAPI
import kr.toxicity.hud.api.adapter.LocationWrapper
import kr.toxicity.hud.api.adapter.WorldWrapper
import kr.toxicity.hud.api.player.PointedLocation
import kr.toxicity.hud.api.player.PointedLocationSource
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Which targets the BetterHUD bridge syncs.
 * - OBJECTIVES_ONLY  — tracked quest location objectives only.
 * - ENTITIES_ONLY    — entityTargets list only (Bukkit entities + Typewriter NPCs).
 * - ANY_ACTIVE_TARGET — both combined.
 */
enum class BetterHudTargetMode {
    OBJECTIVES_ONLY,
    ENTITIES_ONLY,
    ANY_ACTIVE_TARGET,
}

@Entry(
    "waypoint_betterhud_bridge",
    "Sync active V2 waypoint targets (objectives, entities, Typewriter NPCs) to BetterHUD compass points.",
    Colors.CYAN,
    "material-symbols:assistant-navigation"
)
class WaypointBetterHudBridgeEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("BetterHUD compass element name (configured in your BetterHUD layout).")
    val iconName: String = "default",

    @Help("Prefix for BetterHUD point names. entry.id is appended automatically — no need to make this unique per entry.")
    val pointNamePrefix: String = "waypoint_",

    @Help("How often to sync targets to BetterHUD in ticks. 5 = responsive, 20 = 1 s. For moving entities use 5–10.")
    @Min(1) @Max(200)
    val updateIntervalTicks: Int = 10,

    @Help("Which targets to sync: OBJECTIVES_ONLY, ENTITIES_ONLY, or ANY_ACTIVE_TARGET (objectives + entityTargets combined).")
    val targetMode: BetterHudTargetMode = BetterHudTargetMode.ANY_ACTIVE_TARGET,

    @Help("Maximum targets to send to BetterHUD simultaneously. Should match general.maxTargets in tracked_locatable_waypoint. 0 = disabled.")
    @Min(0) @Max(32)
    val maxTargets: Int = 5,

    @Help("Sort order before applying maxTargets: CLOSEST or HIGHEST_PRIORITY.")
    val selection: WaypointTargetSelection = WaypointTargetSelection.CLOSEST,

    @Help("Hide compass point when player is within this distance of the target. 0 = never hide.")
    val arriveRadius: Double = 0.0,

    @Help("Entity and Typewriter NPC targets to include. Same format as integrations.entityTargets in tracked_locatable_waypoint. Requires targetMode = ANY_ACTIVE_TARGET or ENTITIES_ONLY.")
    val entityTargets: List<EntityWaypointTarget> = emptyList(),

    @Help("Route definitions for syncing route point positions instead of final objective positions. Configure the same routes as in your tracked_locatable_waypoint entry — the bridge reads the shared route index written by the visual entry, so both always point to the same route step.")
    val routes: List<WaypointRoute> = emptyList(),

    @Help("Label text for the compass point. Supports {name}, {distance}, {index}, {total}, {target_type}, {entity_name}, {entity_type}. Actual rendering depends on BetterHUD layout configuration.")
    @Colored @Placeholder
    val pointText: Var<String> = ConstVar(""),

    @Help("Sub-label for the compass point. BetterHUD layout must expose this field to render it.")
    @Colored @Placeholder
    val pointSubText: Var<String> = ConstVar(""),
) : AudienceEntry {
    override suspend fun display(): AudienceDisplay = WaypointBetterHudBridgeDisplay(this)
}

// Per-player state: active BetterHUD point IDs and last known positions for change detection.
private data class PlayerHudState(
    val activeIds: MutableSet<String> = mutableSetOf(),
    val knownPositions: MutableMap<String, Triple<Double, Double, Double>> = mutableMapOf(),
)

private class WaypointBetterHudBridgeDisplay(
    private val entry: WaypointBetterHudBridgeEntry,
) : AudienceDisplay(), TickableDisplay {

    private val hudState = ConcurrentHashMap<UUID, PlayerHudState>()
    private var tickCounter = 0

    override fun onPlayerAdd(player: Player) {
        hudState[player.uniqueId] = PlayerHudState()
    }

    override fun onPlayerRemove(player: Player) {
        removeAllPoints(player)
        hudState.remove(player.uniqueId)
    }

    override fun tick() {
        if (++tickCounter % entry.updateIntervalTicks.coerceAtLeast(1) != 0) return
        runSync {
            if (!isActive) return@runSync
            players.forEach { syncPoints(it) }
        }
    }

    // --- Target resolution ---

    private fun resolveTargets(player: Player): List<WaypointTarget> {
        val raw = resolveWaypointTargets(
            player = player,
            selection = entry.selection,
            maxTargets = entry.maxTargets,
            entityTargets = entry.entityTargets,
            includeObjectives = entry.targetMode != BetterHudTargetMode.ENTITIES_ONLY,
            includeEntities = entry.targetMode != BetterHudTargetMode.OBJECTIVES_ONLY,
        )
        if (entry.routes.isEmpty()) return raw
        return raw.map { target ->
            val objectiveId = target.objective?.id ?: return@map target
            applyRouteReadOnly(player, objectiveId, entry.routes, target)
        }
    }

    // Stable point ID: prefix + entry.id + target.zoneKey().
    // entry.id prevents collision when multiple bridge entries are active.
    // zoneKey() is position/uuid-based — stable even when sort order changes.
    private fun pointIdFor(target: WaypointTarget): String =
        "${entry.pointNamePrefix}${entry.id}_${target.zoneKey()}"
            .replace(Regex("[^a-zA-Z0-9_.\\-]"), "_")
            .take(96)

    // --- Sync ---

    private fun syncPoints(player: Player) {
        if (!betterHudAvailable()) return
        val hudPlayer = BetterHudAPI.inst().playerManager.getHudPlayer(player.uniqueId) ?: return
        val state = hudState.getOrPut(player.uniqueId) { PlayerHudState() }

        val targets = resolveTargets(player)

        // Build desired map: pointId → target (first occurrence per key wins)
        val desired = LinkedHashMap<String, WaypointTarget>()
        for (target in targets) {
            val id = pointIdFor(target)
            if (!desired.containsKey(id)) desired[id] = target
        }

        // Apply arrive-radius filter
        val arriveRad = entry.arriveRadius
        val visible: Map<String, WaypointTarget> = if (arriveRad > 0.0)
            desired.filter { (_, t) -> t.distance > arriveRad }
        else desired

        var changed = false

        // Remove stale or now-hidden points
        val toRemove = state.activeIds - visible.keys
        for (id in toRemove) {
            hudPlayer.pointers().removeIf { it.name == id }
            state.activeIds.remove(id)
            state.knownPositions.remove(id)
            changed = true
        }

        // Add new or update points whose position has changed
        for ((id, target) in visible) {
            val loc = target.location
            val worldName = loc.world?.name ?: continue
            val newPos = Triple(loc.x, loc.y, loc.z)
            val posChanged = state.knownPositions[id] != newPos
            val isNew = id !in state.activeIds

            if (isNew || posChanged) {
                if (!isNew) {
                    // Position changed — remove old before re-adding
                    hudPlayer.pointers().removeIf { it.name == id }
                }
                hudPlayer.pointers().add(
                    PointedLocation(
                        PointedLocationSource.INTERNAL,
                        id,
                        entry.iconName,
                        LocationWrapper(WorldWrapper(worldName), loc.x, loc.y, loc.z, 0f, 0f)
                    )
                )
                state.activeIds.add(id)
                state.knownPositions[id] = newPos
                changed = true
            }
        }

        if (changed) hudPlayer.update()
    }

    // --- Cleanup ---

    private fun removeAllPoints(player: Player) {
        val state = hudState[player.uniqueId] ?: return
        if (state.activeIds.isNotEmpty() && betterHudAvailable()) {
            val hudPlayer = BetterHudAPI.inst().playerManager.getHudPlayer(player.uniqueId)
            if (hudPlayer != null) {
                state.activeIds.forEach { id -> hudPlayer.pointers().removeIf { it.name == id } }
                hudPlayer.update()
            }
        }
        state.activeIds.clear()
        state.knownPositions.clear()
    }

    override fun dispose() {
        players.forEach { removeAllPoints(it) }
        hudState.clear()
        super.dispose()
    }

    private fun runSync(block: () -> Unit) {
        if (Bukkit.isPrimaryThread()) block()
        else Bukkit.getScheduler().runTask(plugin, Runnable { block() })
    }

    companion object {
        @Volatile private var checked = false
        @Volatile private var available = false

        fun betterHudAvailable(): Boolean {
            if (!checked) {
                available = runCatching {
                    Bukkit.getPluginManager().getPlugin("BetterHud")?.isEnabled == true
                }.getOrDefault(false)
                if (!available) {
                    Bukkit.getLogger().warning("[WaypointRPG] BetterHUD not found. waypoint_betterhud_bridge is disabled.")
                }
                checked = true
            }
            return available
        }
    }
}
