package cg.headpop.waypointrpg

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Max
import com.typewritermc.core.extension.annotations.Min
import com.typewritermc.engine.paper.entry.entries.AudienceDisplay
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.TickableDisplay
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
    "Sync active waypoint targets to BetterHUD compass points.",
    Colors.BLUE,
    "mdi:compass"
)
class WaypointBetterHudBridgeEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("BetterHUD compass element name to update.")
    val iconName: String = "default",

    @Help("Prefix for compass point IDs. The entry ID is appended automatically.")
    val pointNamePrefix: String = "waypoint_",

    @Help("Which targets to sync: objectives, entities, or both.")
    val targetMode: BetterHudTargetMode = BetterHudTargetMode.ANY_ACTIVE_TARGET,

    @Help("Maximum number of compass points sent at once.")
    @Min(0) @Max(32)
    val maxTargets: Int = 5,

    @Help("Sort order when trimming to maxTargets.")
    val selection: WaypointTargetSelection = WaypointTargetSelection.CLOSEST,

    @Help("Hide the compass point when closer than this distance. 0 = always show.")
    val arriveRadius: Double = 0.0,

    @Help("Entity/NPC targets to include. Requires ANY_ACTIVE_TARGET or ENTITIES_ONLY mode.")
    val entityTargets: List<EntityWaypointTarget> = emptyList(),

    @Help("Route config to show route point positions instead of the final objective.")
    val routes: List<WaypointRoute> = emptyList(),

) : AudienceEntry {
    override suspend fun display(): AudienceDisplay = WaypointBetterHudBridgeDisplay(this)
}

// Per-player state: active BetterHUD point IDs and last known positions for change detection.
private data class PlayerHudState(
    val activeIds: MutableSet<String> = mutableSetOf(),
    val knownPositions: MutableMap<String, Triple<Double, Double, Double>> = mutableMapOf(),
)

// Sync cadence is internal policy — not user-editable. 5 ticks = 0.25 s, responsive
// for both static and moving targets without per-tick overhead.
private const val HUD_UPDATE_INTERVAL_TICKS = 5

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
        if (++tickCounter % HUD_UPDATE_INTERVAL_TICKS != 0) return
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
            // Re-add only when the target moved a meaningful amount. Exact comparison made
            // every moving entity churn a remove+add each sync (compass pointer flicker);
            // half a block is below the compass angular resolution anyway.
            val posChanged = state.knownPositions[id]?.let { (px, py, pz) ->
                val dx = loc.x - px; val dy = loc.y - py; val dz = loc.z - pz
                dx * dx + dy * dy + dz * dz > 0.25
            } ?: true
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
