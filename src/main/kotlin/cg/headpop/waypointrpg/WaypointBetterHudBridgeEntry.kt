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
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.lang.reflect.Constructor
import java.lang.reflect.Method
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

/**
 * Reflection keeps BetterHUD genuinely optional. Typewriter scans every class in an
 * extension while registering events, so even an unreachable direct API reference
 * would make the whole extension fail to load when BetterHUD is absent.
 */
private object BetterHudAccess {
    private data class Api(
        val inst: Method,
        val playerManager: Method,
        val getHudPlayer: Method,
        val pointers: Method,
        val update: Method,
        val pointName: Method,
        val worldConstructor: Constructor<*>,
        val locationConstructor: Constructor<*>,
        val pointConstructor: Constructor<*>,
        val internalSource: Any,
    )

    @Volatile private var initialized = false
    @Volatile private var api: Api? = null
    @Volatile private var warned = false

    fun available(): Boolean {
        if (Bukkit.getPluginManager().getPlugin("BetterHud")?.isEnabled != true) {
            warnOnce("BetterHUD not found. waypoint_betterhud_bridge is disabled.")
            return false
        }
        return loadApi() != null
    }

    fun hudPlayer(uuid: UUID): Any? {
        val access = loadApi() ?: return null
        return runCatching {
            val instance = access.inst.invoke(null)
            val manager = access.playerManager.invoke(instance)
            access.getHudPlayer.invoke(manager, uuid)
        }.onFailure { warnOnce("BetterHUD API access failed: ${it.message}") }.getOrNull()
    }

    @Suppress("UNCHECKED_CAST")
    fun pointers(hudPlayer: Any): MutableCollection<Any>? {
        val access = loadApi() ?: return null
        return runCatching {
            access.pointers.invoke(hudPlayer) as MutableCollection<Any>
        }.onFailure { warnOnce("BetterHUD pointer access failed: ${it.message}") }.getOrNull()
    }

    fun pointName(point: Any): String? = runCatching {
        loadApi()?.pointName?.invoke(point) as? String
    }.getOrNull()

    fun createPoint(
        name: String,
        icon: String,
        world: String,
        x: Double,
        y: Double,
        z: Double,
    ): Any? {
        val access = loadApi() ?: return null
        return runCatching {
            val worldWrapper = access.worldConstructor.newInstance(world)
            val location = access.locationConstructor.newInstance(worldWrapper, x, y, z, 0f, 0f)
            access.pointConstructor.newInstance(access.internalSource, name, icon, location)
        }.onFailure { warnOnce("BetterHUD point creation failed: ${it.message}") }.getOrNull()
    }

    fun update(hudPlayer: Any) {
        val access = loadApi() ?: return
        runCatching { access.update.invoke(hudPlayer) }
            .onFailure { warnOnce("BetterHUD update failed: ${it.message}") }
    }

    private fun loadApi(): Api? {
        if (initialized) return api
        synchronized(this) {
            if (initialized) return api
            api = runCatching {
                // Paper isolates plugin classloaders. Resolve the optional API through
                // BetterHUD's own loader instead of Typewriter's extension loader.
                val betterHudPlugin = Bukkit.getPluginManager().getPlugin("BetterHud")
                    ?: error("BetterHUD plugin is not loaded")
                val loader = betterHudPlugin.javaClass.classLoader
                fun apiClass(name: String) = Class.forName(name, true, loader)
                val apiClass = apiClass("kr.toxicity.hud.api.BetterHudAPI")
                val betterHudClass = apiClass("kr.toxicity.hud.api.BetterHud")
                val playerManagerClass = apiClass("kr.toxicity.hud.api.manager.PlayerManager")
                val hudPlayerClass = apiClass("kr.toxicity.hud.api.player.HudPlayer")
                val pointClass = apiClass("kr.toxicity.hud.api.player.PointedLocation")
                val sourceClass = apiClass("kr.toxicity.hud.api.player.PointedLocationSource")
                val locationClass = apiClass("kr.toxicity.hud.api.adapter.LocationWrapper")
                val worldClass = apiClass("kr.toxicity.hud.api.adapter.WorldWrapper")
                val internal = sourceClass.enumConstants.first { (it as Enum<*>).name == "INTERNAL" }
                Api(
                    inst = apiClass.getMethod("inst"),
                    playerManager = betterHudClass.getMethod("getPlayerManager"),
                    getHudPlayer = playerManagerClass.getMethod("getHudPlayer", UUID::class.java),
                    pointers = hudPlayerClass.getMethod("pointers"),
                    update = hudPlayerClass.getMethod("update"),
                    pointName = pointClass.getMethod("name"),
                    worldConstructor = worldClass.getConstructor(String::class.java),
                    locationConstructor = locationClass.getConstructor(
                        worldClass,
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType,
                        Float::class.javaPrimitiveType,
                        Float::class.javaPrimitiveType,
                    ),
                    pointConstructor = pointClass.getConstructor(
                        sourceClass,
                        String::class.java,
                        String::class.java,
                        locationClass,
                    ),
                    internalSource = internal,
                )
            }.onFailure { warnOnce("BetterHUD API is incompatible: ${it.message}") }.getOrNull()
            initialized = true
            return api
        }
    }

    private fun warnOnce(message: String) {
        if (warned) return
        warned = true
        Bukkit.getLogger().warning("[WaypointRPG] $message")
    }
}

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
        val hudPlayer = BetterHudAccess.hudPlayer(player.uniqueId) ?: return
        val pointers = BetterHudAccess.pointers(hudPlayer) ?: return
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
            pointers.removeIf { BetterHudAccess.pointName(it) == id }
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
                    pointers.removeIf { BetterHudAccess.pointName(it) == id }
                }
                BetterHudAccess.createPoint(
                    id, entry.iconName, worldName, loc.x, loc.y, loc.z
                )?.let(pointers::add) ?: continue
                state.activeIds.add(id)
                state.knownPositions[id] = newPos
                changed = true
            }
        }

        if (changed) BetterHudAccess.update(hudPlayer)
    }

    // --- Cleanup ---

    private fun removeAllPoints(player: Player) {
        val state = hudState[player.uniqueId] ?: return
        if (state.activeIds.isNotEmpty() && betterHudAvailable()) {
            val hudPlayer = BetterHudAccess.hudPlayer(player.uniqueId)
            if (hudPlayer != null) {
                BetterHudAccess.pointers(hudPlayer)?.let { pointers ->
                    state.activeIds.forEach { id ->
                        pointers.removeIf { BetterHudAccess.pointName(it) == id }
                    }
                    BetterHudAccess.update(hudPlayer)
                }
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
        fun betterHudAvailable(): Boolean = BetterHudAccess.available()
    }
}
