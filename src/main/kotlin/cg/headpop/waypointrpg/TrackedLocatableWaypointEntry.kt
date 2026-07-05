package cg.headpop.waypointrpg

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes as PEEntityTypes
import com.github.retrooper.packetevents.util.Quaternion4f as PEQuat
import com.github.retrooper.packetevents.util.Vector3d as PEVec3d
import com.github.retrooper.packetevents.util.Vector3f as PEVec3f
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Query
import com.typewritermc.core.entries.priority
import com.typewritermc.core.extension.annotations.Colored
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Placeholder
import com.typewritermc.core.extension.annotations.WithRotation
import com.typewritermc.core.utils.point.Position
import com.typewritermc.engine.paper.entry.AudienceManager
import com.typewritermc.engine.paper.entry.entries.AudienceDisplay
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.TickableDisplay
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.entry.entries.get
import com.typewritermc.engine.paper.plugin
import com.typewritermc.engine.paper.utils.toBukkitLocation
import org.koin.core.component.get
import com.typewritermc.quest.entries.interfaces.LocatableObjective
import com.typewritermc.quest.entries.trackedShowingObjectives
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.util.Vector
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// =============================================================================
// V2 Config data classes — each becomes an expandable section in the Typewriter panel
// =============================================================================

data class WaypointGeneralConfig(
    @Help("What to display: HOLOGRAM (text only), BEAM (vertical column only), or BOTH.")
    val mode: WaypointType = WaypointType.BOTH,

    @Help("Which objective to show when multiple are active: HIGHEST_PRIORITY or CLOSEST.")
    val selection: WaypointTargetSelection = WaypointTargetSelection.HIGHEST_PRIORITY,

    @Help("Max objectives to show simultaneously. Set to 2+ for players with multiple active quests.")
    val maxTargets: Int = 5,

    @Help("Server ticks between beam updates. 5 = 4 updates/sec. Label tracks every tick regardless.")
    val tickRate: Int = 5,

    @Help("3D distance (blocks) at which the player is considered to have arrived at the objective.")
    val arriveRadius: Double = 1.5,

    @Help("Hide beam and label when player arrives. The symbol stays visible at the waypoint.")
    val hideOnArrive: Boolean = true,
)

data class WaypointTargetConfig(
    @Help("Extra Y added to the objective's base position. 0.0 for location objectives; 2.0 for head-height NPCs.")
    val offset: Double = 0.0,

    @Help("Y difference (blocks) between player and objective before {direction} shows ▲/▼ and verticalColumnMode activates.")
    val verticalThreshold: Double = 10.0,
)

data class WaypointLabelConfig(
    @Help("Label text. MiniMessage. Placeholders: {name}, {distance}, {direction} (↑↗→↘↓↙←↖ or ▲/▼).")
    @Colored @Placeholder
    val text: Var<String> = ConstVar("<white>{name}</white>\n<gold>{distance}</gold>"),

    @Help("Use the objective's display name as {name}. False = use this entry's name field.")
    val useObjectiveName: Boolean = true,

    @Help("Extra Y the label floats above the calculated marker position.")
    val height: Double = 1.0,

    @Help("Label floats at most this many blocks from the camera toward the objective.")
    val floatDist: Double = 5.0,

    @Help("Label hides when horizontal distance to objective is smaller than this. Symbol takes over.")
    val hideRange: Double = 8.0,

    @Help("Max angle (degrees) from player look direction before label fades. 0 = always visible.")
    val fov: Double = 55.0,

    @Help("Label text scale. 1.0 = default size.")
    val scale: Float = 1.0f,

    @Help("Billboard mode: CENTER (always faces player), VERTICAL, HORIZONTAL, FIXED.")
    val billboard: String = "CENTER",

    @Help("Text alignment within the label: CENTER, LEFT or RIGHT.")
    val align: String = "CENTER",

    @Help("Show background panel behind the text. Suppressed automatically when mode=BOTH (beam present).")
    val background: Boolean = true,

    @Help("Background color in #AARRGGBB. #80000000 = semi-transparent black.")
    val bgColor: String = "#80000000",

    @Help("Text opacity 0–255. 255 = fully opaque.")
    val opacity: Int = 255,

    @Help("Drop shadow behind the text.")
    val shadow: Boolean = true,

    @Help("Reserved for a future ghost layer. The main label always renders with depth-test normal. Changing this field has no effect on the current render.")
    val seeThrough: Boolean = false,

    @Help("Max line width in pixels before text wraps.")
    val lineWidth: Int = 255,

    @Help("Lateral spacing in blocks between labels when multiple objectives are active. 0 = no separation. Labels are spread symmetrically: with 2 targets, each shifts 0.35 left/right. Beam always points at the real target regardless of this offset.")
    val multiOffset: Double = 0.35,
)

data class WaypointSymbolConfig(
    @Help("Show a Unicode icon that scales with distance, independent of the label.")
    val enabled: Boolean = true,

    @Help("Symbol content. MiniMessage supported. Custom font glyphs work here.")
    @Colored @Placeholder
    val text: Var<String> = ConstVar("<gold>◆</gold>"),

    @Help("Symbol scale when player is close (at or below snapRange).")
    val minScale: Float = 3.0f,

    @Help("Symbol scale when far (at farDist). Also the scale on arrive.")
    val maxScale: Float = 5.0f,

    @Help("Distance where the symbol starts growing toward maxScale.")
    val nearDist: Double = 5.0,

    @Help("Distance where the symbol reaches maxScale.")
    val farDist: Double = 150.0,

    @Help("Extra Y offset for the symbol above the label position during normal follow mode.")
    val offset: Double = 0.5,

    @Help("Horizontal distance at which the symbol snaps to the exact waypoint X,Z position.")
    val snapRange: Double = 8.0,

    @Help("Horizontal distance at which the snapped symbol returns to normal follow mode. Must be > snapRange.")
    val snapLeave: Double = 12.0,

    @Help("Y height above the target position when the symbol is snapped or on arrive.")
    val snapHeight: Double = 3.0,

    @Help("Snapped symbol position: CENTER_ON_WAYPOINT (directly above waypoint, recommended) or FRONT_OF_BEAM (offset toward player, avoids beam overlap).")
    val snapPosition: SymbolSnapPosition = SymbolSnapPosition.CENTER_ON_WAYPOINT,
)

data class WaypointBeamConfig(
    @Help("Show or hide the vertical beam. When false, only label and symbol are displayed.")
    val enabled: Boolean = true,

    @Help("Force maximum brightness so the beam stays visible at night. When false, the beam uses natural world lighting. Translucent materials (stained glass) may still blend with the sky background regardless of this setting.")
    val fullBright: Boolean = true,

    @Help("Outer beam layer material. Visual block selector in the Typewriter panel. Must be a solid block — AIR and items fall back to LIME_STAINED_GLASS.")
    val outer: Material = Material.LIME_STAINED_GLASS,

    @Help("Inner beam layer material (rendered inside the outer layer). Must be a solid block — AIR and items fall back to LIME_CONCRETE.")
    val inner: Material = Material.LIME_CONCRETE,

    @Help("X/Z width of the outer beam layer in blocks.")
    val width: Float = 0.5f,

    @Help("X/Z width of the inner beam layer in blocks.")
    val coreWidth: Float = 0.25f,

    @Help("Z depth of the outer beam layer in blocks.")
    val depth: Float = 0.5f,

    @Help("Z depth of the inner beam layer in blocks.")
    val coreDepth: Float = 0.25f,

    @Help("Beam height in blocks. Extended downward dynamically if dynamicHeight is enabled.")
    val height: Float = 150.0f,

    @Help("Extend beam downward to the player's Y level so it stays visible when underground.")
    val dynamicHeight: Boolean = true,

    @Help("Within this horizontal distance the beam stays fixed at the objective.")
    val staticRange: Double = 30.0,

    @Help("Beyond this horizontal distance the beam follows the player at followDist.")
    val followRange: Double = 60.0,

    @Help("In follow mode, beam stays this many blocks ahead of the player toward the objective.")
    val followDist: Double = 55.0,

    @Help("Beam and label start fading at this distance from the objective.")
    val fadeStart: Double = 10.0,

    @Help("Beam and label fully disappear at this distance (player is very close).")
    val fadeEnd: Double = 3.0,

    @Help("Server ticks between beam position updates. 1 = every tick (smoothest follow, default). Higher values reduce server load but make the beam movement choppier. Independent of general.tickRate which controls label updates.")
    val beamTickRate: Int = 1,
)

data class WaypointBobConfig(
    @Help("Enable the floating bob animation on the label and symbol.")
    val enabled: Boolean = true,

    @Help("Vertical oscillation amplitude in blocks. 0 = no movement.")
    val height: Double = 0.06,

    @Help("Bob oscillation speed in cycles per second.")
    val speed: Double = 1.2,
)


data class WaypointIntegrationConfig(
    @Help("Live entities to track as waypoints (escort quests, moving targets). Each gets its own beam and label.")
    val entityTargets: List<EntityWaypointTarget> = emptyList(),

    @Help("Make tracked entity targets glow with a visible outline (client-side only).")
    val entityGlow: Boolean = false,

    @Help("Horizontal distance at which the entity glow activates. Only used when entityGlow is true.")
    val glowRange: Double = 20.0,
)

data class WaypointPerformanceConfig(
    @Help("Skip position packets when player and target have barely moved. Has no effect when bob is enabled.")
    val lazyUpdate: Boolean = false,

    @Help("Remove leftover display entities from old plugin versions when the player joins.")
    val cleanupOnJoin: Boolean = false,

    @Help("Search radius in blocks for stale entity cleanup on join.")
    val cleanupRadius: Double = 50.0,
)

// =============================================================================
// Enums and support types
// =============================================================================

enum class WaypointType { HOLOGRAM, BEAM, BOTH }
enum class WaypointTargetSelection { HIGHEST_PRIORITY, CLOSEST }
enum class SymbolSnapPosition {
    CENTER_ON_WAYPOINT,   // symbol directly above waypoint XZ (recommended)
    FRONT_OF_BEAM,        // offset toward player by beamMaxHalf+0.15 (avoids visual overlap with beam)
}
enum class EntityTargetType {
    UUID,            // Bukkit.getEntity(uuid) — global, fastest
    NAME,            // entity name or custom display name, nearest within maxDistance
    SCOREBOARD_TAG,  // nearest entity with scoreboard tag within maxDistance
    TYPEWRITER_NPC,  // Typewriter NpcInstance by entry ID — live position via ActivityManager, fallback to spawnLocation
}

data class WaypointRoute(
    @Help("Objective ID this route applies to.")
    val objectiveId: String = "",
    @Help("Unique ID for this route within the entry. Leave blank to use objectiveId. Useful when sharing route state across entries (zone trigger, BetterHUD bridge).")
    val routeId: String = "",
    @Help("If true, advancing past one route point can skip points the player passes through simultaneously. If false, player must reach each point in strict order.")
    val allowSkip: Boolean = true,
    @Help("Clear route progress when the objective disappears (e.g. quest step deactivated). Player restarts from point 0 when the objective reactivates.")
    val resetOnObjectiveChange: Boolean = true,
    @Help("Reset route progress back to point 0 when the last route point is reached, instead of continuing to the final objective.")
    val resetOnComplete: Boolean = false,
    @Help("Intermediate waypoints along the path to guide the player.")
    val points: List<WaypointRoutePoint> = emptyList(),
)

data class EntityWaypointTarget(
    @Help("How to find the entity: UUID (global, fastest), NAME (name or display name in world), SCOREBOARD_TAG (nearest entity with this scoreboard tag).")
    val targetType: EntityTargetType = EntityTargetType.UUID,
    @Help("Entity UUID. Required when targetType=UUID. Format: 550e8400-e29b-41d4-a716-446655440000")
    val uuid: String = "",
    @Help("Entity name or custom display name. Used when targetType=NAME.")
    val name: String = "",
    @Help("Scoreboard tag. Used when targetType=SCOREBOARD_TAG. Picks nearest tagged entity within maxDistance.")
    val tag: String = "",
    @Help("Label shown on the waypoint. MiniMessage. Leave blank to use the entity's name.")
    @Colored @Placeholder
    val displayName: Var<String> = ConstVar(""),
    @Help("Search radius in blocks for NAME and SCOREBOARD_TAG. No effect for UUID or TYPEWRITER_NPC.")
    val maxDistance: Double = 128.0,
    @Help("Sort priority for this entity target (HIGHEST_PRIORITY selection). Higher = shown first.")
    val priority: Int = 0,
    @Help("Typewriter NpcInstance entry ID. Required when targetType=TYPEWRITER_NPC. Copy from the entry's id field in the manifest.")
    val npcEntryId: String = "",
)

data class WaypointRoutePoint(
    @Help("Label for this route point. Blank = use objective display name.")
    @Colored @Placeholder
    val name: Var<String> = ConstVar(""),
    @Help("World position of this waypoint.")
    @WithRotation
    val position: Var<Position> = ConstVar(Position.ORIGIN),
    @Help("Arrival radius in blocks to advance to the next route point.")
    val radius: Double = 3.0,
)

// =============================================================================
// Entry — V2
// =============================================================================

@Entry(
    "tracked_locatable_waypoint",
    "Quest waypoint to currently tracked objectives — V2",
    Colors.GREEN,
    "material-symbols:assistant-navigation"
)
class TrackedLocatableWaypointEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("General display mode and targeting behavior.")
    val general: WaypointGeneralConfig = WaypointGeneralConfig(),

    @Help("Target position offset and vertical behavior.")
    val target: WaypointTargetConfig = WaypointTargetConfig(),

    @Help("Floating label text above the waypoint.")
    val label: WaypointLabelConfig = WaypointLabelConfig(),

    @Help("Distance-scaled icon that snaps to the waypoint when close.")
    val symbol: WaypointSymbolConfig = WaypointSymbolConfig(),

    @Help("Vertical beacon beam at the waypoint location.")
    val beam: WaypointBeamConfig = WaypointBeamConfig(),

    @Help("Floating bob animation for label and symbol.")
    val bob: WaypointBobConfig = WaypointBobConfig(),

    @Help("Manual route waypoints per objective ID. Guides the player along a path instead of a straight line.")
    val routes: List<WaypointRoute> = emptyList(),

    @Help("Entity tracking and third-party integrations.")
    val integrations: WaypointIntegrationConfig = WaypointIntegrationConfig(),

    @Help("Performance tuning — packet frequency and legacy cleanup.")
    val performance: WaypointPerformanceConfig = WaypointPerformanceConfig(),
) : AudienceEntry {
    override suspend fun display(): AudienceDisplay = TrackedLocatableWaypointDisplay(this)
}

// =============================================================================
// Internal state classes
// =============================================================================

private class ActiveBeam {
    var id1: Int = -1
    var id2: Int = -1
    var lastX: Double = 0.0
    var lastY: Double = 0.0
    var lastZ: Double = 0.0
    var sid1: Int = -1
    var sid2: Int = -1
    var disabled: Boolean = false
    var hidden: Boolean = false
    var hiddenAtTick: Long = 0L
    var lastTransformHash: Int? = null
    val isSpawned get() = id1 != -1
    fun reset() {
        id1 = -1; id2 = -1
        lastX = 0.0; lastY = 0.0; lastZ = 0.0
        sid1 = -1; sid2 = -1
        disabled = false
        hidden = false
        hiddenAtTick = 0L
        lastTransformHash = null
    }
}

private class FakeTextDisplay(val isSymbol: Boolean = false) {
    var id: Int = -1
    var spawnX: Double = 0.0
    var spawnY: Double = 0.0
    var spawnZ: Double = 0.0
    var firstFrame: Boolean = true
    var hidden: Boolean = false
    var hiddenAtTick: Long = 0L
    var lastMetadataHash: Int? = null
    var lastInputsHash: Int? = null
    var lastRawText: String? = null
    var lastComponent: Component? = null
    val isSpawned get() = id != -1
    fun reset() {
        id = -1; spawnX = 0.0; spawnY = 0.0; spawnZ = 0.0
        firstFrame = true; hidden = false; hiddenAtTick = 0L; lastMetadataHash = null
        lastInputsHash = null; lastRawText = null; lastComponent = null
    }
}

private class WaypointSlot {
    val beam = ActiveBeam()
    val label = FakeTextDisplay()
    val symbol = FakeTextDisplay(isSymbol = true)
    var lastTargetLocation: Location? = null
    var lastVisualAnchor: Vector? = null
    var lastVisualBaseY: Double? = null
    var lastBeamPointer: Location? = null
    var lastLabelLocation: Location? = null
    var lastSymbolLocation: Location? = null
    var symbolSnapped: Boolean = false
    var glowEntityId: Int = -1
    // Visibility latches — enter/leave thresholds differ so hovering exactly on a
    // boundary never causes rapid create/destroy or hide/show flicker.
    var arrivedLatch: Boolean = false
    var labelHideLatch: Boolean = false
    var beamFadeLatch: Boolean = false
    var verticalColumnLatch: Boolean = false
}

private class PlayerWaypointState {
    val slots = LinkedHashMap<String, WaypointSlot>()
    var lastPlayerLocation: Location? = null
    var staleCleanupDone = false
    var tickSerial = 0L
    val entitySelectorCache = HashMap<String, CachedEntitySelector>()
    // Objective resolution cache — the quest query + position Var resolution is the most
    // expensive per-tick work per player; static objective locations don't need re-resolving
    // every tick. Distances inside age up to the refresh interval — they only affect sort
    // order; updateSlot recomputes real distances from the locations every tick.
    var cachedObjectiveTargets: List<WaypointTarget> = emptyList()
    var objectiveTargetsRefreshTick: Long = 0L
    // Consecutive resolves without meaningful movement — exit hysteresis for the
    // adaptive resolve cadence (see resolveTargets).
    var objectiveStillResolves: Int = OBJECTIVE_STILL_RESOLVES_TO_SLOW
    // Route point resolution cache, keyed by objectiveId. Bounded by entry.routes size.
    val routePointCache = HashMap<String, CachedRoutePoints>()
}

private data class CachedEntitySelector(
    var entityUuid: UUID? = null,
    var nextRefreshTick: Long = 0L,
)

private class CachedRoutePoints(
    val points: List<ResolvedRoutePoint>,
    val refreshTick: Long,
)

private data class ResolvedRoutePoint(
    val index: Int,
    val point: WaypointRoutePoint,
    val position: Position,
    val location: Location,
)

private enum class MotionProfile {
    BEAM,
    LABEL,
    SYMBOL,
    SYMBOL_SNAP,
}

// Global route index registry — shared across all entry types so visual, zone trigger,
// and BetterHUD bridge all see the same progress for a given player+objective+route.
// Key: "$playerUUID:$objectiveId:$effectiveRouteId"
// Only TrackedLocatableWaypointDisplay writes (advances) this. Others read only.
internal val globalRouteIndices = ConcurrentHashMap<String, Int>()

internal fun routeStateKey(playerUUID: java.util.UUID, objectiveId: String, effectiveRouteId: String): String =
    "$playerUUID:$objectiveId:$effectiveRouteId"

private const val ENTITY_SELECTOR_REFRESH_TICKS = 10L
private const val OBJECTIVE_RESOLVE_INTERVAL_TICKS = 5L
private const val ROUTE_POINT_RESOLVE_INTERVAL_TICKS = 20L

// Handoff glide: deltas above START only occur when the target itself changed
// (route point advance, selector swapping to another entity, route → final objective).
// Instead of hard-snapping, converge exponentially (ALPHA per tick) with a guaranteed
// minimum catch-up speed so the glide always terminates quickly (~5-7 ticks for 30 blocks).
private const val HANDOFF_GLIDE_START = 2.5
private const val HANDOFF_GLIDE_ALPHA = 0.38
private const val HANDOFF_MIN_STEP = 2.5
// Alpha ceiling for the glide. Without it the formula degenerates as the delta
// approaches HANDOFF_MIN_STEP: a 3-block handoff (adjacent route points) got
// alpha = 2.5/3 ≈ 0.83 → visually a 1-tick hard snap. Capping at 0.65 turns short
// handoffs into a 2-3 tick sweep; deltas > ~6.6 blocks are unaffected (0.38 wins).
private const val HANDOFF_MAX_ALPHA = 0.65

// Beam-specific glide: the beam is the largest visual mass, so residual drag on a long
// handoff reads worse there than on text. Firmer parameters — 30 blocks converge in
// ~5 ticks (vs ~7 with the text parameters), 3-block handoffs in 2 ticks — while the
// exponential form keeps the sweep continuous (never a 1-tick snap: cap 0.75).
private const val BEAM_GLIDE_ALPHA = 0.50
private const val BEAM_GLIDE_MIN_STEP = 4.0
private const val BEAM_GLIDE_MAX_ALPHA = 0.75

// A display hidden longer than this is unlikely to re-show soon (player parked inside
// the arrive radius). Destroy it for real to free the client-side entity; a later
// re-show goes through the spawn path (first-frame duration 0) which places it without
// a streak — visually identical to a metadata re-show. Quick boundary crossings stay
// on the cheap hide/show path.
private const val HIDDEN_ENTITY_TTL_TICKS = 600L

// Objective movement policy: 1e-3 was below visual relevance — Var re-resolution jitter
// could pin the resolve loop at 20 Hz. 0.01 blocks between resolves is still far below
// anything visible, and real movers (≥0.05 blocks/tick) clear it trivially. The
// still-counter adds exit hysteresis: a mover that pauses for one resolve doesn't
// bounce the cadence between 1 and 5 ticks.
private const val OBJECTIVE_MOVE_EPSILON = 0.01
private const val OBJECTIVE_STILL_RESOLVES_TO_SLOW = 3

// Label and symbol receive a teleport every server tick while tracking, so both
// interpolation_duration (index 9) and teleport_duration (index 10) must be 1:
// the client then finishes exactly one interpolation window per update — firm, no lag.
// Higher values with per-tick teleports restart the window every tick, so the entity
// permanently trails N ticks behind (rubber-band). Varying the value between ticks
// changes the metadata hash and forces useless metadata resends.
private const val TEXT_INTERP = 1
private val PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText()

// =============================================================================
// Debug instrumentation — enable with the JVM flag -Dwaypointrpg.debug=true.
// All counters are mutated on the main thread only. `enabled` is a startup
// constant, so guarded increments JIT-fold to no-ops when the flag is off —
// zero cost and zero log spam in production. When enabled, one summary line
// is logged every 10 s and the window resets, giving comparable before/after
// rates (packets/s, spawns/min, resolves/s, map sizes) under real load.
// =============================================================================
internal object WaypointStats {
    val enabled: Boolean = java.lang.Boolean.getBoolean("waypointrpg.debug")

    var beamTeleports = 0L; var labelTeleports = 0L; var symbolTeleports = 0L
    var beamMeta = 0L; var labelMeta = 0L; var symbolMeta = 0L
    var spawns = 0L; var destroys = 0L; var hides = 0L; var reshows = 0L
    var objectiveResolves = 0L; var selectorScans = 0L; var routeResolves = 0L
    var playerTicks = 0L; var slotUpdates = 0L
    private var windowStartMillis = 0L

    fun maybeLog(instanceInfo: () -> String) {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (windowStartMillis == 0L) { windowStartMillis = now; return }
        val elapsedMillis = now - windowStartMillis
        if (elapsedMillis < 10_000L) return
        val s = elapsedMillis / 1000.0
        Bukkit.getLogger().info(
            "[WaypointRPG][stats %.1fs] tp/s beam=%.1f label=%.1f symbol=%.1f | meta/s beam=%.1f label=%.1f symbol=%.1f | spawn/min=%.1f destroy/min=%.1f hide/min=%.1f reshow/min=%.1f | resolve/s obj=%.1f scan=%.1f route=%.1f | slots/player-tick=%.2f | %s routeIdx=%d".format(
                s,
                beamTeleports / s, labelTeleports / s, symbolTeleports / s,
                beamMeta / s, labelMeta / s, symbolMeta / s,
                spawns * 60.0 / s, destroys * 60.0 / s, hides * 60.0 / s, reshows * 60.0 / s,
                objectiveResolves / s, selectorScans / s, routeResolves / s,
                if (playerTicks > 0) slotUpdates.toDouble() / playerTicks else 0.0,
                instanceInfo(), globalRouteIndices.size,
            )
        )
        beamTeleports = 0; labelTeleports = 0; symbolTeleports = 0
        beamMeta = 0; labelMeta = 0; symbolMeta = 0
        spawns = 0; destroys = 0; hides = 0; reshows = 0
        objectiveResolves = 0; selectorScans = 0; routeResolves = 0
        playerTicks = 0; slotUpdates = 0
        windowStartMillis = now
    }
}

// =============================================================================
// Display
// =============================================================================

private class TrackedLocatableWaypointDisplay(
    private val entry: TrackedLocatableWaypointEntry,
) : AudienceDisplay(), TickableDisplay {

    private val states = ConcurrentHashMap<UUID, PlayerWaypointState>()
    private val miniMessage = MiniMessage.miniMessage()
    private val updateQueued = AtomicBoolean(false)
    private val glowBaseFlags = ConcurrentHashMap<String, Byte>()
    private var tickCounter = 0
    private var beamTickCounter = 0

    // Static label config, parsed once — the old per-update parsing ran trim()/uppercase()
    // (two string allocs) plus a color parse per label per tick for values that are
    // immutable after the entry loads.
    private val labelBgColor: Int =
        if (entry.label.background && entry.general.mode != WaypointType.BOTH)
            parseColorARGB(entry.label.bgColor, 128, 0, 0, 0) else 0
    private val labelAlignBits: Int =
        when (entry.label.align.trim().uppercase()) { "LEFT" -> 8; "RIGHT" -> 16; else -> 0 }
    private val labelBillboard: Byte =
        when (entry.label.billboard.trim().uppercase()) { "FIXED" -> 0; "VERTICAL" -> 1; "HORIZONTAL" -> 2; else -> 3 }.toByte()

    override fun onPlayerAdd(player: Player) {
        states.computeIfAbsent(player.uniqueId) { PlayerWaypointState() }
        runSync {
            if (!isActive || player !in this) return@runSync
            val state = states.computeIfAbsent(player.uniqueId) { PlayerWaypointState() }
            cleanupStaleDisplaysIfNeeded(player, state)
            updatePlayerSync(player, force = true)
        }
    }

    override fun onPlayerRemove(player: Player) {
        val state = states.remove(player.uniqueId) ?: return
        clearOwnRouteIndices(player.uniqueId)
        runSync { destroyAllSlots(player, state) }
    }

    // Remove only THIS entry's route keys. globalRouteIndices is shared across entries
    // (zone trigger, BetterHUD, other visual entries) — a blanket uuid-prefix wipe would
    // erase route progress owned by other entries for the same player.
    private fun clearOwnRouteIndices(playerUUID: UUID) {
        for (route in entry.routes) {
            if (route.objectiveId.isBlank()) continue
            val effectiveId = route.routeId.ifBlank { route.objectiveId }
            globalRouteIndices.remove(routeStateKey(playerUUID, route.objectiveId, effectiveId))
        }
    }

    override fun tick() {
        val interval = entry.general.tickRate.coerceAtLeast(1)
        val fullUpdate = (++tickCounter % interval == 0)
        val beamInterval = entry.beam.beamTickRate.coerceAtLeast(1)
        val fullBeamUpdate = (++beamTickCounter % beamInterval == 0)
        if (!updateQueued.compareAndSet(false, true)) return
        runSync {
            updateQueued.set(false)
            if (!isActive) return@runSync
            players.forEach { updatePlayerSync(it, force = false, fullUpdate = fullUpdate, fullBeamUpdate = fullBeamUpdate) }
            WaypointStats.maybeLog {
                var slots = 0; var selCache = 0
                states.values.forEach { slots += it.slots.size; selCache += it.entitySelectorCache.size }
                "states=${states.size} slots=$slots selCache=$selCache glowFlags=${glowBaseFlags.size}"
            }
        }
    }

    override fun dispose() {
        val snapshot = states.entries.toList()
        states.clear()
        snapshot.forEach { (uuid, _) -> clearOwnRouteIndices(uuid) }
        runSync {
            snapshot.forEach { (uuid, state) ->
                val onlinePlayer = Bukkit.getPlayer(uuid)
                if (onlinePlayer != null) {
                    destroyAllSlots(onlinePlayer, state)
                } else {
                    state.slots.values.forEach { slot ->
                        slot.beam.reset()
                        slot.label.reset()
                        slot.symbol.reset()
                        slot.lastTargetLocation = null
                        clearSlotVisualState(slot)
                    }
                    state.entitySelectorCache.clear()
                    state.routePointCache.clear()
                    state.cachedObjectiveTargets = emptyList()
                }
            }
        }
        super.dispose()
    }

    private fun runSync(block: () -> Unit) {
        if (Bukkit.isPrimaryThread()) { block(); return }
        Bukkit.getScheduler().runTask(plugin, Runnable { block() })
    }

    // --- Main update ---

    private fun updatePlayerSync(player: Player, force: Boolean, fullUpdate: Boolean = true, fullBeamUpdate: Boolean = true) {
        val state = states.computeIfAbsent(player.uniqueId) { PlayerWaypointState() }
        state.tickSerial++
        // Prune periodically, not per tick — building the valid-key set every tick for
        // every player is wasted work; stale cache entries only need eventual cleanup.
        if (state.tickSerial % 40L == 0L) pruneEntitySelectorCache(state)
        cleanupStaleDisplaysIfNeeded(player, state)

        val playerEyes = player.eyeLocation
        // player.location allocates a fresh Location per call — updateSlot used to call it
        // up to 4× per slot per tick (direction arrow, beam position, beam height, glow).
        // Resolve once per player per tick and thread it through.
        val playerFeet = player.location
        val playerMoved = force || hasMeaningfullyMoved(state, playerEyes)
        if (WaypointStats.enabled) WaypointStats.playerTicks++

        val targets = resolveTargets(player, state, force)

        // Inline stale-slot sweep. The old map{key()}.toSet() + filter chain allocated
        // three collections per player per tick; targets ≤ maxTargets (~3), so a direct
        // scan over the (tiny) slot map is cheaper and allocation-free when nothing changed.
        if (state.slots.isNotEmpty()) {
            val slotIter = state.slots.entries.iterator()
            while (slotIter.hasNext()) {
                val slotEntry = slotIter.next()
                val key = slotEntry.key
                if (targets.any { it.key() == key }) continue
                val slot = slotEntry.value
                slotIter.remove()
                if (slot.glowEntityId != -1) { setEntityGlow(player, slot.glowEntityId, false); slot.glowEntityId = -1 }
                destroyBeamSlot(player, slot.beam)
                destroyFakeDisplay(player, slot.label)
                destroyFakeDisplay(player, slot.symbol)
                clearSlotVisualState(slot)
                // Clear route progress if objective disappeared and resetOnObjectiveChange is set
                if (!key.startsWith("entity:")) {
                    val objectiveId = key.substringBefore(":")
                    val route = entry.routes.firstOrNull { it.objectiveId == objectiveId }
                    if (route != null && route.resetOnObjectiveChange) {
                        val effectiveId = route.routeId.ifBlank { objectiveId }
                        globalRouteIndices.remove(routeStateKey(player.uniqueId, objectiveId, effectiveId))
                    }
                }
            }
        }

        if (targets.isEmpty()) {
            // player.eyeLocation already returns a fresh Location — no clone needed to store.
            state.lastPlayerLocation = playerEyes
            return
        }

        val bobActive = entry.bob.enabled && entry.bob.height > 0.0
        val shouldUpdate = force || playerMoved || bobActive
        // Same value for every slot this tick — compute the sine once per player.
        val bobY = calculateBob()

        if (!shouldUpdate && entry.performance.lazyUpdate) {
            val anyTargetMoved = targets.any { target ->
                val slot = state.slots[target.key()] ?: return@any true
                slot.lastTargetLocation?.let { last ->
                    val offsetLoc = target.location.clone().add(0.0, entry.target.offset, 0.0)
                    last.world != offsetLoc.world || last.distanceSquared(offsetLoc) > 0.0025
                } ?: true
            }
            if (!anyTargetMoved) return
        }

        state.lastPlayerLocation = playerEyes

        if (WaypointStats.enabled) WaypointStats.slotUpdates += targets.size
        targets.forEachIndexed { index, target ->
            val key = target.key()
            val slot = state.slots.getOrPut(key) { WaypointSlot() }
            updateSlot(player, state, slot, target, playerEyes, playerFeet, bobY, force || playerMoved || bobActive, fullUpdate, fullBeamUpdate, index, targets.size)
        }
    }

    // --- Per-slot update ---

    private fun updateSlot(
        player: Player,
        state: PlayerWaypointState,
        slot: WaypointSlot,
        target: WaypointTarget,
        playerEyes: Location,
        playerFeet: Location,
        bobY: Double,
        shouldUpdate: Boolean,
        fullUpdate: Boolean = true,
        fullBeamUpdate: Boolean = true,
        index: Int = 0,
        total: Int = 1,
    ) {
        val targetLocation = target.location.clone().add(0.0, entry.target.offset, 0.0)
        if (player.world != targetLocation.world) {
            if (slot.glowEntityId != -1) { setEntityGlow(player, slot.glowEntityId, false); slot.glowEntityId = -1 }
            destroyBeamSlot(player, slot.beam)
            destroyFakeDisplay(player, slot.label)
            destroyFakeDisplay(player, slot.symbol)
            clearSlotVisualState(slot)
            return
        }

        val distance = playerEyes.distance(targetLocation)
        val dx = targetLocation.x - playerEyes.x
        val dz = targetLocation.z - playerEyes.z
        val horizontalDist = sqrt(dx * dx + dz * dz)
        val verticalDelta = targetLocation.y - playerEyes.y
        // Vertical-column latch — the only visibility boundary that had no hysteresis after
        // phase 1. The raw condition sits on two thresholds at once (snapRange AND
        // verticalThreshold); hovering on either flipped label hide/show and the symbol's
        // snap branch every tick. Enter on the exact original condition, leave with margin.
        val absVertical = abs(verticalDelta)
        if (slot.verticalColumnLatch) {
            val leaveVertical = (entry.target.verticalThreshold - 1.0)
                .coerceAtLeast(entry.target.verticalThreshold * 0.5)
            if (horizontalDist > entry.symbol.snapRange + 0.75 || absVertical < leaveVertical)
                slot.verticalColumnLatch = false
        } else if (horizontalDist <= entry.symbol.snapRange && absVertical > entry.target.verticalThreshold) {
            slot.verticalColumnLatch = true
        }
        val verticalColumnMode = slot.verticalColumnLatch

        if (entry.symbol.enabled) {
            val snapDist = entry.symbol.snapRange.coerceAtLeast(0.0)
            val leaveDist = entry.symbol.snapLeave.coerceAtLeast(snapDist + 0.1)
            if (!slot.symbolSnapped && horizontalDist <= snapDist) slot.symbolSnapped = true
            else if (slot.symbolSnapped && horizontalDist > leaveDist) slot.symbolSnapped = false
        }

        // Arrive latch: enter at arriveRadius, leave at arriveRadius + 0.75.
        // Without hysteresis, hovering exactly on the radius boundary destroys and
        // respawns beam + label every tick.
        if (slot.arrivedLatch) {
            if (distance > entry.general.arriveRadius + 0.75) slot.arrivedLatch = false
        } else if (distance <= entry.general.arriveRadius) {
            slot.arrivedLatch = true
        }

        if (entry.general.hideOnArrive && slot.arrivedLatch) {
            if (slot.glowEntityId != -1) { setEntityGlow(player, slot.glowEntityId, false); slot.glowEntityId = -1 }
            // Hide instead of destroy: arrive is a boundary players cross constantly.
            // Destroy + respawn churned entities (spawn + full metadata + CraftEngine
            // block-state remap) on every crossing; a scale-0 hide costs one metadata
            // packet each way, and the re-show path (duration-0 meta + teleport) places
            // beam and label back without any slide streak.
            hideBeamSlot(player, slot.beam, state.tickSerial)
            hideFakeDisplay(player, slot.label, state.tickSerial)
            slot.lastBeamPointer = null
            slot.lastLabelLocation = null
            slot.lastVisualAnchor = null
            slot.lastVisualBaseY = null
            if (entry.symbol.enabled) {
                val arrivedPos = Location(
                    player.world,
                    target.location.x,
                    target.location.y + entry.symbol.snapHeight + bobY,
                    target.location.z,
                )
                val smoothedArrivedPos = smoothLocation(slot.lastSymbolLocation, arrivedPos, MotionProfile.SYMBOL_SNAP)
                slot.lastSymbolLocation = smoothedArrivedPos
                updateSymbolDisplay(player, slot, smoothedArrivedPos, entry.symbol.maxScale, 1.0f, TEXT_INTERP)
            } else {
                destroyFakeDisplay(player, slot.symbol)
                slot.lastSymbolLocation = null
            }
            return
        }

        val slotMoved = slot.lastTargetLocation?.let { last ->
            last.world != targetLocation.world || last.distanceSquared(targetLocation) > 0.0025
        } ?: true
        // targetLocation is a fresh clone built above and never mutated afterwards.
        slot.lastTargetLocation = targetLocation

        val doUpdate = shouldUpdate || slotMoved
        val labelInterp = TEXT_INTERP

        val markerName = target.markerName(player)
        // Degenerate case: eyes exactly on the target. distance is the full 3D eye→target
        // length, so this replaces the old toVector() pair + lengthSquared check.
        if (distance <= 0.01) {
            destroyBeamSlot(player, slot.beam)
            destroyFakeDisplay(player, slot.label)
            slot.lastVisualAnchor = null
            slot.lastVisualBaseY = null
            return
        }

        // thinFactor quantized to 1/32 steps: a continuously-varying float changed the
        // metadata hash every tick across the whole fade band, forcing full metadata
        // resends. 1/32 is imperceptible — the client interpolates scale between steps.
        val thinFactor = (smoothstep(entry.beam.fadeEnd, entry.beam.fadeStart, distance) * 32.0).roundToInt() / 32.0f

        // Horizontal unit direction toward the target as plain doubles. dx/dz already exist
        // above — the old code re-derived them through two toVector() allocations, a temp
        // horizontal Vector and an in-place normalize. Zero-alloc scalar math.
        val dirX: Double
        val dirZ: Double
        if (horizontalDist > 0.01) {
            dirX = dx / horizontalDist
            dirZ = dz / horizontalDist
        } else if (abs(playerEyes.pitch) < 89.9f) {
            // Target directly above/below: horizontal look direction from yaw —
            // equivalent to the old direction.setY(0).normalize() without the Vectors.
            val yawRad = Math.toRadians(playerEyes.yaw.toDouble())
            dirX = -sin(yawRad)
            dirZ = cos(yawRad)
        } else {
            dirX = 0.0
            dirZ = 1.0
        }

        // Lateral separation for multiple simultaneous objectives.
        // Right vector = direction rotated 90° CW in the horizontal plane.
        // Lane 0 of 2 shifts left, lane 1 shifts right; center target (3 of 3) gets zero offset.
        // Beam always points at the real target — this offset only affects label and symbol.
        val latX: Double
        val latZ: Double
        if (total > 1 && entry.label.multiOffset > 0.0) {
            val lane = (index - (total - 1) / 2.0) * entry.label.multiOffset
            latX = dirZ * lane
            latZ = -dirX * lane
        } else {
            latX = 0.0
            latZ = 0.0
        }

        // Velocity look-ahead with lateral damping:
        //   parallel component (toward waypoint) = full look-ahead
        //   perpendicular component (strafe)     = 35% of look-ahead
        // Prevents the label from jerking when the player strafes past the waypoint.
        // Decomposition in scalars — the vector version cost a clone plus chained mutations
        // per slot per tick. player.velocity is the only remaining alloc (Bukkit returns a
        // copy); its Y is captured here so the anchor block doesn't call it a second time.
        val vel = player.velocity
        val velX = vel.x
        val velZ = vel.z
        val velYAbs = abs(vel.y)
        val speed = sqrt(velX * velX + velZ * velZ)
        val speed01 = smoothstep(0.35, 1.50, speed).coerceIn(0.0, 1.0)
        val lookAheadTicks = 1.0 + 2.0 * speed01
        val parallelAmount = velX * dirX + velZ * dirZ
        val parX = dirX * parallelAmount
        val parZ = dirZ * parallelAmount
        val velOffX = parX * lookAheadTicks + (velX - parX) * lookAheadTicks * 0.35
        val velOffZ = parZ * lookAheadTicks + (velZ - parZ) * lookAheadTicks * 0.35

        val beamMaxHalf = maxOf(entry.beam.width, entry.beam.depth) * thinFactor / 2.0
        val beamFrontDepth = horizontalDist - beamMaxHalf - 0.08
        val safeClamp = if (beamFrontDepth > 1.0) minOf(entry.label.floatDist, beamFrontDepth) else entry.label.floatDist

        val directionArrow = calculateDirectionArrow(playerFeet, target.location)

        // Near-hide latch (label/symbol follow mode): hide at hideRange, show again at
        // hideRange + 0.75. Beam fade latch: fade out at thinFactor ≤ 0.01, respawn only
        // once it recovers past 0.05 (~half a block of hysteresis on the fade curve).
        if (slot.labelHideLatch) {
            if (horizontalDist > entry.label.hideRange + 0.75) slot.labelHideLatch = false
        } else if (horizontalDist <= entry.label.hideRange) {
            slot.labelHideLatch = true
        }
        if (slot.beamFadeLatch) {
            if (thinFactor > 0.05f) slot.beamFadeLatch = false
        } else if (thinFactor <= 0.01f) {
            slot.beamFadeLatch = true
        }

        val shouldBeVisible = !slot.labelHideLatch && !slot.beamFadeLatch
        val shouldUpdateTransform = !entry.performance.lazyUpdate || doUpdate

        val labelBaseVisible = shouldBeVisible
            && (entry.general.mode == WaypointType.HOLOGRAM || entry.general.mode == WaypointType.BOTH)

        val fovAlphaFactor: Double = if (!labelBaseVisible) {
            0.0
        } else if (entry.label.fov <= 0.0) {
            1.0
        } else {
            // Look direction from yaw/pitch in scalars — Location.direction allocates a
            // Vector per call. (dx, verticalDelta, dz) / distance is exactly the unit
            // eye→target vector the old desired3D held.
            val yawRad = Math.toRadians(playerEyes.yaw.toDouble())
            val pitchRad = Math.toRadians(playerEyes.pitch.toDouble())
            val cosPitch = cos(pitchRad)
            val dot = (((-sin(yawRad) * cosPitch) * dx - sin(pitchRad) * verticalDelta + (cos(yawRad) * cosPitch) * dz) / distance)
                .coerceIn(-1.0, 1.0)
            val angleDeg = Math.toDegrees(acos(dot))
            val fovFadeDeg = 8.0
            val fadeStart = (entry.label.fov - fovFadeDeg).coerceAtLeast(0.0)
            when {
                angleDeg <= fadeStart -> 1.0
                angleDeg >= entry.label.fov -> 0.0
                else -> {
                    val t = ((angleDeg - fadeStart) / fovFadeDeg).coerceIn(0.0, 1.0)
                    1.0 - t * t * (3.0 - 2.0 * t)
                }
            }
        }

        // Quantized to steps of 8 — otherwise every fractional head turn inside the FOV fade
        // band changes the opacity byte and forces a full metadata resend each tick.
        val finalOpacity = (((entry.label.opacity.coerceIn(0, 255) * fovAlphaFactor).roundToInt() + 4) / 8 * 8)
            .coerceIn(0, 255)
        val labelShouldBeVisible = labelBaseVisible && finalOpacity > 3

        val visualAnchor: Vector? = when {
            !shouldBeVisible -> {
                slot.lastVisualAnchor = null
                slot.lastVisualBaseY = null
                slot.lastLabelLocation = null
                if (!slot.symbolSnapped && !verticalColumnMode) slot.lastSymbolLocation = null
                null
            }
            shouldUpdateTransform -> {
                // Single Vector alloc (needed for slot storage), built from scalars —
                // replaces toVector() plus two chained adds.
                val rawAnchor = Vector(
                    playerEyes.x + dirX * safeClamp + velOffX,
                    0.0,
                    playerEyes.z + dirZ * safeClamp + velOffZ,
                )
                val verticalHint = (verticalDelta * 0.08).coerceIn(-1.25, 1.25)
                val rawBaseY = playerEyes.y + entry.label.height + verticalHint

                val verticalSpeed = velYAbs
                val baseAlphaY = if (verticalSpeed > 0.45) 0.24 else 0.18
                val maxStepY   = if (verticalSpeed > 0.45) 0.24 else 0.16
                val smoothedBaseY = slot.lastVisualBaseY?.let { last ->
                    last + ((rawBaseY - last) * baseAlphaY).coerceIn(-maxStepY, maxStepY)
                } ?: rawBaseY
                slot.lastVisualBaseY = smoothedBaseY
                rawAnchor.y = smoothedBaseY + bobY

                val rawDeltaXZ = slot.lastVisualAnchor?.let { prev ->
                    val ddx = rawAnchor.x - prev.x
                    val ddz = rawAnchor.z - prev.z
                    sqrt(ddx * ddx + ddz * ddz)
                } ?: 0.0
                var alphaXZ = when {
                    rawDeltaXZ > 1.2  -> 0.95
                    rawDeltaXZ > 0.6  -> 0.85
                    rawDeltaXZ > 0.25 -> 0.70
                    else              -> 0.55
                }
                if (rawDeltaXZ > 2.0) alphaXZ = 1.0
                else if (rawDeltaXZ > 1.0) alphaXZ = maxOf(alphaXZ, 0.90)

                // Both branches produce a fresh Vector that is never mutated afterwards
                // (label/symbol positions are built component-wise from it) — store directly.
                val smoothed = slot.lastVisualAnchor?.let { prev ->
                    Vector(
                        lerp(prev.x, rawAnchor.x, alphaXZ),
                        rawAnchor.y,
                        lerp(prev.z, rawAnchor.z, alphaXZ),
                    )
                } ?: rawAnchor

                slot.lastVisualAnchor = smoothed
                smoothed
            }
            else -> slot.lastVisualAnchor
        }

        // --- Label ---
        if (!labelShouldBeVisible || verticalColumnMode) {
            hideFakeDisplay(player, slot.label, state.tickSerial)
            slot.lastLabelLocation = null
            if (verticalColumnMode) slot.lastVisualBaseY = null
        } else if (shouldUpdateTransform && visualAnchor != null) {
            // Component-wise build: avoids Vector clone + add + toLocation (3 allocs → 1).
            val rawLabelPos = Location(
                player.world,
                visualAnchor.x + latX,
                visualAnchor.y,
                visualAnchor.z + latZ,
            )
            val labelPos = smoothLocation(slot.lastLabelLocation, rawLabelPos, MotionProfile.LABEL)
            slot.lastLabelLocation = labelPos
            updateLabel(player, slot, labelPos, markerName, distance, directionArrow, 1.0f, labelInterp, finalOpacity, index, total,
                isEntityTarget = target.entityUUID != null || target.sourceId?.startsWith("npc:") == true,
                entityTypeName = target.entityTypeName,
                routePointIndex = target.routePointIndex, routePointCount = target.routePointCount,
                routePointName = target.routePointName)
        }

        // --- Symbol ---
        if (entry.symbol.enabled) {
            if (slot.symbolSnapped || verticalColumnMode) {
                // Snapped/arrived: symbol sits directly on the target — no lateral offset.
                if (doUpdate) {
                    // Component-wise build — the old clone + toVector pair + normalize chain
                    // allocated four temporaries per tick while snapped.
                    var snapX = target.location.x
                    var snapZ = target.location.z
                    if (entry.symbol.snapPosition == SymbolSnapPosition.FRONT_OF_BEAM) {
                        val toPlayerX = playerEyes.x - snapX
                        val toPlayerZ = playerEyes.z - snapZ
                        val len2 = toPlayerX * toPlayerX + toPlayerZ * toPlayerZ
                        if (len2 > 0.001) {
                            val push = (beamMaxHalf + 0.15) / sqrt(len2)
                            snapX += toPlayerX * push
                            snapZ += toPlayerZ * push
                        }
                    }
                    val snapPos = Location(player.world, snapX, target.location.y + entry.symbol.snapHeight + bobY, snapZ)
                    val smoothedSnapPos = smoothLocation(slot.lastSymbolLocation, snapPos, MotionProfile.SYMBOL_SNAP)
                    slot.lastSymbolLocation = smoothedSnapPos
                    updateSymbolDisplay(player, slot, smoothedSnapPos, entry.symbol.minScale, 1.0f, labelInterp)
                }
            } else if (!shouldBeVisible) {
                hideFakeDisplay(player, slot.symbol, state.tickSerial)
                slot.lastSymbolLocation = null
            } else if (shouldUpdateTransform && visualAnchor != null) {
                // Scale quantized to 0.05 steps — the distance-driven lerp changed the float
                // every tick while moving, forcing a full metadata resend per tick per symbol.
                val symbolScale = (lerp(
                    entry.symbol.minScale.toDouble(), entry.symbol.maxScale.toDouble(),
                    smoothstep(entry.symbol.nearDist, entry.symbol.farDist, distance)
                ) * 20.0).roundToInt() / 20.0f
                val rawSymbolPos = Location(
                    player.world,
                    visualAnchor.x + latX,
                    visualAnchor.y + entry.symbol.offset,
                    visualAnchor.z + latZ,
                )
                val symbolPos = smoothLocation(slot.lastSymbolLocation, rawSymbolPos, MotionProfile.SYMBOL)
                slot.lastSymbolLocation = symbolPos
                updateSymbolDisplay(player, slot, symbolPos, symbolScale, thinFactor, labelInterp)
            }
        } else {
            destroyFakeDisplay(player, slot.symbol)
            slot.lastSymbolLocation = null
        }

        // --- Beam (independent tick rate — fullBeamUpdate defaults to every tick for smooth follow) ---
        if (fullBeamUpdate) {
            if (entry.general.mode == WaypointType.BEAM || entry.general.mode == WaypointType.BOTH) {
                if (doUpdate) {
                    if (slot.beamFadeLatch) {
                        // Scale-0 hide, not destroy — the fade boundary is crossed constantly
                        // near objectives; destroying churned spawn + full metadata (incl.
                        // CraftEngine block-state remap) on every re-approach.
                        hideBeamSlot(player, slot.beam, state.tickSerial)
                        slot.lastBeamPointer = null
                    } else {
                        val pointerPos = calculateBeamPosition(playerFeet, targetLocation)
                        val smoothedPointerPos = smoothLocation(slot.lastBeamPointer, pointerPos, MotionProfile.BEAM)
                        slot.lastBeamPointer = smoothedPointerPos
                        updateBeam(player, slot.beam, smoothedPointerPos, thinFactor, playerFeet.y)
                    }
                }
            } else {
                destroyBeamSlot(player, slot.beam)
                slot.lastBeamPointer = null
            }

            // --- Entity glow (tickRate ticks) ---
            if (entry.integrations.entityGlow && target.entityUUID != null) {
                val entity = findEntityByUuid(target.entityUUID)
                val entityId = entity?.entityId ?: -1
                // entity.location allocates — resolve once instead of twice.
                val entityLoc = entity?.location
                val inRange = entityLoc != null && entityLoc.world == player.world
                    && playerFeet.distance(entityLoc) <= entry.integrations.glowRange
                if (inRange && entityId != -1) {
                    if (slot.glowEntityId != entityId) {
                        if (slot.glowEntityId != -1) setEntityGlow(player, slot.glowEntityId, false)
                        setEntityGlow(player, entityId, true, entity)
                        slot.glowEntityId = entityId
                    }
                } else if (slot.glowEntityId != -1) {
                    setEntityGlow(player, slot.glowEntityId, false)
                    slot.glowEntityId = -1
                }
            } else if (!entry.integrations.entityGlow && slot.glowEntityId != -1) {
                setEntityGlow(player, slot.glowEntityId, false)
                slot.glowEntityId = -1
            }
        }

    }

    // --- Target resolution ---

    private fun resolveTargets(player: Player, state: PlayerWaypointState, force: Boolean): List<WaypointTarget> {
        if (entry.general.maxTargets <= 0) return emptyList()
        val playerLocation = player.location

        val raw: List<WaypointTarget> = if (force || state.tickSerial >= state.objectiveTargetsRefreshTick) {
            if (WaypointStats.enabled) WaypointStats.objectiveResolves++
            val previous = state.cachedObjectiveTargets
            val fresh = player.trackedShowingObjectives()
                .filterIsInstance<LocatableObjective>()
                .flatMap { objective ->
                    objective.positions(player).mapNotNull { position ->
                        val location = runCatching { position.toBukkitLocation() }.getOrNull() ?: return@mapNotNull null
                        val distance = if (location.world == playerLocation.world)
                            playerLocation.distance(location) else Double.POSITIVE_INFINITY
                        WaypointTarget(objective, position, location, distance)
                    }
                }
                .toList()
            // Adaptive cadence: static objective sets refresh every 5 ticks; a resolve that
            // shows real movement (deadband OBJECTIVE_MOVE_EPSILON — micro-jitter from Var
            // re-resolution stays below it) drops to every tick so a moving
            // LocatableObjective tracks at full rate. Exit hysteresis: only after
            // OBJECTIVE_STILL_RESOLVES_TO_SLOW consecutive still resolves does the cadence
            // return to 5 ticks, so a mover pausing briefly doesn't bounce 1↔5. Size
            // changes count as movement so appearing/disappearing objectives settle fast.
            val moved = fresh.size != previous.size || fresh.indices.any { i ->
                val a = fresh[i].location
                val b = previous[i].location
                a.world != b.world || abs(a.x - b.x) + abs(a.y - b.y) + abs(a.z - b.z) > OBJECTIVE_MOVE_EPSILON
            }
            if (moved) state.objectiveStillResolves = 0
            else if (state.objectiveStillResolves < OBJECTIVE_STILL_RESOLVES_TO_SLOW) state.objectiveStillResolves++
            state.cachedObjectiveTargets = fresh
            state.objectiveTargetsRefreshTick = state.tickSerial +
                if (state.objectiveStillResolves < OBJECTIVE_STILL_RESOLVES_TO_SLOW) 1L
                else OBJECTIVE_RESOLVE_INTERVAL_TICKS
            fresh
        } else {
            state.cachedObjectiveTargets
        }

        val entityRaw = entry.integrations.entityTargets.mapNotNull { et ->
            resolveEntityTarget(et, player, state)
        }

        if (raw.isEmpty() && entityRaw.isEmpty()) return emptyList()

        val all = raw + entityRaw
        val sorted = when (entry.general.selection) {
            WaypointTargetSelection.CLOSEST ->
                all.sortedWith(
                    compareBy<WaypointTarget> { it.distance }
                        .thenByDescending { it.objective?.priority ?: it.entityPriority }
                        .thenBy { it.objective?.id ?: it.customName ?: "" }
                )
            WaypointTargetSelection.HIGHEST_PRIORITY ->
                all.sortedWith(
                    compareByDescending<WaypointTarget> { it.objective?.priority ?: it.entityPriority }
                        .thenBy { it.distance }
                        .thenBy { it.objective?.id ?: it.customName ?: "" }
                )
        }

        return sorted
            .take(entry.general.maxTargets)
            .map { applyRoute(player, state, it) }
            // A multi-position objective with a route collapses every position onto the same
            // route point → identical slot keys → the same slot would be updated twice per tick
            // with different lateral offsets (visible jitter). Keep the first occurrence only.
            .distinctBy { it.key() }
    }

    // resolveEntityTarget, resolveTypewriterNpcTarget, findEntityByUuid — package-level below class

    private fun applyRoute(player: Player, state: PlayerWaypointState, directTarget: WaypointTarget): WaypointTarget {
        if (directTarget.entityUUID != null) return directTarget
        val objectiveId = directTarget.objective?.id ?: return directTarget
        val route = entry.routes.firstOrNull { it.objectiveId == objectiveId && it.points.isNotEmpty() }
            ?: return directTarget

        val effectiveRouteId = route.routeId.ifBlank { objectiveId }
        val key = routeStateKey(player.uniqueId, objectiveId, effectiveRouteId)

        // Route point positions are effectively static (ConstVar in practice) — resolving
        // the Var + Location conversion per point per tick is wasted work. Distances are
        // always computed fresh below, so advancement checks stay exact.
        val cached = state.routePointCache[objectiveId]
        val resolvedPoints: List<ResolvedRoutePoint> = if (cached != null && state.tickSerial < cached.refreshTick) {
            cached.points
        } else {
            if (WaypointStats.enabled) WaypointStats.routeResolves++
            val fresh = route.points.mapIndexedNotNull { idx, point ->
                val position = runCatching { point.position.get(player) }.getOrNull() ?: return@mapIndexedNotNull null
                val location = runCatching { position.toBukkitLocation() }.getOrNull() ?: return@mapIndexedNotNull null
                ResolvedRoutePoint(idx, point, position, location)
            }
            state.routePointCache[objectiveId] =
                CachedRoutePoints(fresh, state.tickSerial + ROUTE_POINT_RESOLVE_INTERVAL_TICKS)
            fresh
        }
        if (resolvedPoints.isEmpty()) return directTarget

        var index = globalRouteIndices.getOrDefault(key, 0).coerceIn(0, resolvedPoints.size)
        val playerLocation = player.location

        if (route.allowSkip) {
            for (resolved in resolvedPoints) {
                if (resolved.index < index) continue
                if (resolved.location.world != playerLocation.world) continue
                if (playerLocation.distance(resolved.location) <= resolved.point.radius.coerceAtLeast(0.1))
                    index = resolved.index + 1
            }
        } else {
            val current = resolvedPoints.firstOrNull { it.index == index }
            if (current != null && current.location.world == playerLocation.world &&
                playerLocation.distance(current.location) <= current.point.radius.coerceAtLeast(0.1)) {
                index++
            }
        }
        index = index.coerceIn(0, resolvedPoints.size)

        if (index >= resolvedPoints.size) {
            if (route.resetOnComplete) {
                globalRouteIndices[key] = 0
                val first = resolvedPoints.first()
                val dist = if (first.location.world == playerLocation.world)
                    playerLocation.distance(first.location) else Double.POSITIVE_INFINITY
                val pointName = runCatching { first.point.name.get(player) }.getOrNull().orEmpty()
                return WaypointTarget(
                    objective = directTarget.objective,
                    position = first.position,
                    location = first.location,
                    distance = dist,
                    routePointIndex = 0,
                    routePointCount = resolvedPoints.size,
                    routePointName = pointName,
                )
            }
            globalRouteIndices[key] = resolvedPoints.size
            return directTarget
        }

        globalRouteIndices[key] = index
        val nextPoint = resolvedPoints.first { it.index >= index }
        val dist = if (nextPoint.location.world == playerLocation.world)
            playerLocation.distance(nextPoint.location) else Double.POSITIVE_INFINITY
        val pointName = runCatching { nextPoint.point.name.get(player) }.getOrNull().orEmpty()
        return WaypointTarget(
            objective = directTarget.objective,
            position = nextPoint.position,
            location = nextPoint.location,
            distance = dist,
            routePointIndex = nextPoint.index,
            routePointCount = resolvedPoints.size,
            routePointName = pointName,
        )
    }

    private fun WaypointTarget.markerName(player: Player): String {
        if (!customName.isNullOrBlank()) return customName
        if (!routePointName.isNullOrBlank()) return routePointName
        if (routePointIndex != null) return "Waypoint ${routePointIndex + 1}/$routePointCount"
        if (entry.label.useObjectiveName) {
            val display = runCatching { objective?.display(player) }.getOrNull()
            if (!display.isNullOrBlank()) return display
        }
        return entry.name.ifBlank { "Objective" }
    }

    private fun WaypointTarget.key(): String =
        slotKeyCache ?: computeKey().also { slotKeyCache = it }

    private fun WaypointTarget.computeKey(): String {
        // sourceId first: selector-based targets (NAME/SCOREBOARD_TAG, NPC) keep ONE slot
        // when the matched entity changes — glide handoff instead of destroy + respawn.
        if (sourceId != null) return sourceId
        if (entityUUID != null) return "entity:$entityUUID"
        val oid = objective?.id
        // Routed objectives get ONE stable slot key for the whole journey
        // (point 0 → 1 → … → final objective). Including the route index or position
        // here would destroy + respawn beam/label/symbol on every route advance —
        // the entities must persist and glide to the next point instead.
        if (oid != null && entry.routes.any { it.objectiveId == oid && it.points.isNotEmpty() }) {
            return "$oid:route"
        }
        val worldKey = location.world?.uid?.toString() ?: location.world?.name ?: "unknown"
        val x = (location.x * 100.0).roundToInt()
        val y = (location.y * 100.0).roundToInt()
        val z = (location.z * 100.0).roundToInt()
        return "${oid ?: "unknown"}:d:$worldKey:$x:$y:$z"
    }

    // --- Beacon position ---

    private fun calculateBeamPosition(playerLocation: Location, targetLocation: Location): Location {
        val dx = targetLocation.x - playerLocation.x
        val dz = targetLocation.z - playerLocation.z
        val horizDist = sqrt(dx * dx + dz * dz)
        if (horizDist < 0.01) return Location(targetLocation.world, targetLocation.x, targetLocation.y, targetLocation.z)
        return when {
            horizDist <= entry.beam.staticRange ->
                Location(targetLocation.world, targetLocation.x, targetLocation.y, targetLocation.z)
            horizDist >= entry.beam.followRange -> {
                val ratio = entry.beam.followDist.coerceIn(1.0, horizDist - 0.5) / horizDist
                Location(playerLocation.world, playerLocation.x + dx * ratio, targetLocation.y, playerLocation.z + dz * ratio)
            }
            else -> {
                val t = smoothstep(entry.beam.staticRange, entry.beam.followRange, horizDist)
                val ratio = entry.beam.followDist.coerceIn(1.0, horizDist - 0.5) / horizDist
                Location(playerLocation.world,
                    lerp(targetLocation.x, playerLocation.x + dx * ratio, t),
                    targetLocation.y,
                    lerp(targetLocation.z, playerLocation.z + dz * ratio, t))
            }
        }
    }

    // --- PacketEvents beam ---

    private fun safeBeamMaterial(material: Material, fallback: Material, layerName: String): Material {
        return if (material.isBlock && !material.isAir) {
            material
        } else {
            Bukkit.getLogger().warning(
                "[WaypointRPG] Beam $layerName material '${material.name}' is not a valid block. Using ${fallback.name}."
            )
            fallback
        }
    }

    private fun updateBeam(player: Player, beam: ActiveBeam, pointerPos: Location, thinFactor: Float, playerY: Double) {
        if (beam.disabled) return
        if (!entry.beam.enabled) {
            destroyBeamSlot(player, beam)
            return
        }

        // interp = beamTickRate: teleport_duration and interpolation_duration both match the actual
        // update frequency so the client always has exactly one interpolation window per update.
        val interp = entry.beam.beamTickRate.coerceAtLeast(1)
        val sx1 = entry.beam.width * thinFactor
        val sz1 = entry.beam.depth * thinFactor
        val sx2 = entry.beam.coreWidth * thinFactor
        val sz2 = entry.beam.coreDepth * thinFactor

        // Legacy geometry: beam origin is 20 blocks below the pointer Y, scale = fixed height.
        // dynamicHeight=true extends the base down to cover the player when underground.
        val beamBaseY: Double
        val scaleY: Float
        if (entry.beam.dynamicHeight) {
            // playerY param — the old code called player.location here again (fresh alloc)
            // even though the caller already resolved the player position.
            val baseY = minOf(pointerPos.y - 20.0, playerY - 20.0)
            val topY = pointerPos.y + entry.beam.height
            beamBaseY = baseY
            scaleY = (topY - baseY).toFloat().coerceIn(entry.beam.height.toFloat(), 500f)
        } else {
            beamBaseY = pointerPos.y - 20.0
            scaleY = entry.beam.height.toFloat().coerceIn(1f, 500f)
        }

        val bx = pointerPos.x
        val bz = pointerPos.z

        if (!beam.isSpawned) {

            // Validate materials — warn immediately if invalid, fall back to safe defaults.
            val mat1 = safeBeamMaterial(entry.beam.outer, Material.LIME_STAINED_GLASS, "outer")
            val mat2 = safeBeamMaterial(entry.beam.inner, Material.LIME_CONCRETE, "inner")

            var sid1 = resolveBlockStateId(mat1)
            var sid2 = resolveBlockStateId(mat2)

            // Absolute fallback: STONE — used if PacketEvents cannot resolve the primary material.
            if (sid1 <= 0) {
                Bukkit.getLogger().warning("[WaypointRPG] Block state for beam outer '${mat1.name}' resolved to $sid1. Trying STONE fallback.")
                sid1 = resolveBlockStateId(Material.STONE)
            }
            if (sid2 <= 0) {
                Bukkit.getLogger().warning("[WaypointRPG] Block state for beam inner '${mat2.name}' resolved to $sid2. Trying STONE fallback.")
                sid2 = resolveBlockStateId(Material.STONE)
            }

            if (sid1 <= 0 || sid2 <= 0) {
                Bukkit.getLogger().warning(
                    "[WaypointRPG] Beam disabled — even STONE fallback failed (sid1=$sid1 sid2=$sid2). Check CraftEngine compatibility."
                )
                beam.disabled = true
                return
            }
            beam.id1 = nextEntityId()
            beam.id2 = nextEntityId()
            beam.sid1 = sid1
            beam.sid2 = sid2
            beam.lastX = bx
            beam.lastY = beamBaseY
            beam.lastZ = bz
            sendBeamSpawn(player, beam.id1, bx, beamBaseY, bz)
            sendBeamSpawn(player, beam.id2, bx, beamBaseY, bz)
            // Full metadata — includes block state (index 23). Sent ONCE per entity lifetime.
            runCatching {
                sendBeamSpawnMeta(player, beam.id1, sid1, sx1, sz1, scaleY)
                sendBeamSpawnMeta(player, beam.id2, sid2, sx2, sz2, scaleY)
            }.onFailure {
                Bukkit.getLogger().warning(
                    "[WaypointRPG] Beam spawn metadata failed. " +
                    "Beam disabled for this slot. ${it::class.simpleName}: ${it.message}"
                )
                runCatching {
                    val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return@runCatching
                    user.sendPacket(WrapperPlayServerDestroyEntities(beam.id1, beam.id2))
                }
                beam.reset()
                beam.disabled = true
            }
        } else {
            // Re-show after a fade hide: teleport FIRST — the hide meta left
            // teleport_duration = 0, so the move applies instantly while the beam is
            // still scale-0 invisible. Then fall through: lastTransformHash is null from
            // the hide, so the normal path below sends exactly one transform meta with
            // the regular interp, growing the scale back in place. One meta + one
            // teleport per entity — the previous recipe (duration-0 meta + teleport +
            // a second meta next tick to restore interp) cost an extra metadata resend.
            if (beam.hidden) {
                teleportBeamEntities(player, beam, bx, beamBaseY, bz)
                beam.lastX = bx; beam.lastY = beamBaseY; beam.lastZ = bz
                beam.hidden = false
                if (WaypointStats.enabled) WaypointStats.reshows++
            }
            // Metadata first (sets teleport_duration = interp so the client knows to interpolate),
            // then teleport packet (client uses the duration it just received to smooth the move).
            // Manual hash — listOf().hashCode() allocated a list and boxed five floats per
            // beam per tick just to compare.
            var transformHash = sx1.toRawBits()
            transformHash = 31 * transformHash + sz1.toRawBits()
            transformHash = 31 * transformHash + sx2.toRawBits()
            transformHash = 31 * transformHash + sz2.toRawBits()
            transformHash = 31 * transformHash + scaleY.toRawBits()
            transformHash = 31 * transformHash + interp
            if (beam.lastTransformHash != transformHash) {
                sendBeamTransformMeta(player, beam.id1, sx1, sz1, scaleY, interp)
                sendBeamTransformMeta(player, beam.id2, sx2, sz2, scaleY, interp)
                beam.lastTransformHash = transformHash
            }
            val positionChanged = abs(beam.lastX - bx) > 0.0001
                || abs(beam.lastY - beamBaseY) > 0.0001
                || abs(beam.lastZ - bz) > 0.0001
            if (positionChanged) {
                teleportBeamEntities(player, beam, bx, beamBaseY, bz)
                beam.lastX = bx; beam.lastY = beamBaseY; beam.lastZ = bz
            }
        }
    }

    private fun teleportBeamEntities(player: Player, beam: ActiveBeam, x: Double, y: Double, z: Double) {
        if (!beam.isSpawned) return
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(WrapperPlayServerEntityTeleport(beam.id1, PEVec3d(x, y, z), 0f, 0f, false))
            user.sendPacket(WrapperPlayServerEntityTeleport(beam.id2, PEVec3d(x, y, z), 0f, 0f, false))
            if (WaypointStats.enabled) WaypointStats.beamTeleports += 2
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] teleportBeamEntities failed: ${it.message}") }
    }

    private fun resolveBlockStateId(material: Material): Int {
        return runCatching {
            SpigotConversionUtil.fromBukkitBlockData(material.createBlockData()).globalId
        }.getOrElse { -1 }
    }

    private fun sendBeamSpawn(player: Player, id: Int, x: Double, y: Double, z: Double) {
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(WrapperPlayServerSpawnEntity(
                id, Optional.of(UUID.randomUUID()), PEEntityTypes.BLOCK_DISPLAY,
                PEVec3d(x, y, z), 0f, 0f, 0f, 0, Optional.empty()
            ))
            if (WaypointStats.enabled) WaypointStats.spawns++
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] sendBeamSpawn failed: ${it.message}") }
    }

    // Sent ONCE when the beam entity is created — includes block state (index 23).
    // DO NOT call this on position/scale updates; use sendBeamTransformMeta instead.
    //
    // Index 23 MUST use EntityDataTypes.BLOCK_STATE, not INT.
    // Both are EntityDataType<Integer> at the Kotlin level, but they have different type-tag bytes
    // in the wire protocol. CraftEngine reads the type tag to decide whether to remap block state;
    // if it sees INT (type 1) instead of BLOCK_STATE (type 13), it cannot identify the field as a
    // block state, and its remapBlockState() returns -1, causing the ArrayIndexOutOfBoundsException.
    //
    // Translation centers the block on the entity's spawn X/Z. A BlockDisplay renders from its
    // origin outward (+X +Z), so without the negative half-size offset both beams would extend
    // in the same corner direction instead of sharing a common center.
    //
    // Indices 20/21 (display_width / display_height) define the culling AABB used by the client.
    // Without these, the AABB is zero-sized at the entity's origin. When the origin is at worldMinY
    // (-64) and the player is on the surface (~118), the AABB is underground and frustum-culled —
    // the beam exists on the server but the client never renders it.
    // Setting display_height = sy ensures the culling box spans ±sy from the entity origin,
    // covering from worldMinY-sy to worldMinY+sy. The top of that range (worldMinY+sy =
    // targetY + beam.height) is well above the player, so culling is never triggered.
    private fun sendBeamSpawnMeta(
        player: Player, id: Int,
        blockStateId: Int, sx: Float, sz: Float, sy: Float,
    ) {
        val meta = listOf(
            EntityData(8,  EntityDataTypes.INT,         0),
            EntityData(9,  EntityDataTypes.INT,         0),
            EntityData(10, EntityDataTypes.INT,         0),
            EntityData(11, EntityDataTypes.VECTOR3F,    PEVec3f(-sx / 2f, 0f, -sz / 2f)),
            EntityData(12, EntityDataTypes.VECTOR3F,    PEVec3f(sx, sy, sz)),
            EntityData(13, EntityDataTypes.QUATERNION,  PEQuat(0f, 0f, 0f, 1f)),
            EntityData(14, EntityDataTypes.QUATERNION,  PEQuat(0f, 0f, 0f, 1f)),
            EntityData(15, EntityDataTypes.BYTE,        0.toByte()),
            EntityData(16, EntityDataTypes.INT,         if (entry.beam.fullBright) (15 shl 20) or (15 shl 4) else -1),
            EntityData(17, EntityDataTypes.FLOAT,       8.0f),
            EntityData(20, EntityDataTypes.FLOAT,       maxOf(sx, sz)),
            EntityData(21, EntityDataTypes.FLOAT,       sy),
            EntityData(23, EntityDataTypes.BLOCK_STATE, blockStateId),
        )
        val user = PacketEvents.getAPI().playerManager.getUser(player)
            ?: throw IllegalStateException("No PacketEvents user for ${player.name}")
        user.sendPacket(WrapperPlayServerEntityMetadata(id, meta))
        if (WaypointStats.enabled) WaypointStats.beamMeta++
    }

    // Sent every update — transform only, NO block state.
    // Block state is stable after spawn and does not need re-sending.
    // Scale/interp update sent every update tick after spawn. NO block state.
    // Translation keeps the centering offset (-sx/2, 0, -sz/2) so outer and inner share the same
    // visual center regardless of their different widths.
    //
    // Index 9  = interpolation_duration: smooths the transform (scale, translation) over N ticks.
    // Index 10 = teleport_duration: smooths the WrapperPlayServerEntityTeleport over N ticks.
    //            Setting this to `interp` eliminates the jumpy movement between update ticks.
    //            This metadata packet must be sent BEFORE the teleport packet so the client picks
    //            up the new duration before applying the position change.
    //
    // Indices 20/21 updated here too since thinFactor (and thus sx/sz) may change each tick.
    private fun sendBeamTransformMeta(
        player: Player, id: Int,
        sx: Float, sz: Float, sy: Float,
        interp: Int,
    ) {
        val meta = listOf(
            EntityData(8,  EntityDataTypes.INT,      0),
            EntityData(9,  EntityDataTypes.INT,      interp),
            EntityData(10, EntityDataTypes.INT,      interp),   // teleport_duration = smooth position
            EntityData(11, EntityDataTypes.VECTOR3F, PEVec3f(-sx / 2f, 0f, -sz / 2f)),
            EntityData(12, EntityDataTypes.VECTOR3F, PEVec3f(sx, sy, sz)),
            EntityData(20, EntityDataTypes.FLOAT,    maxOf(sx, sz)),
            EntityData(21, EntityDataTypes.FLOAT,    sy),
        )
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(WrapperPlayServerEntityMetadata(id, meta))
            if (WaypointStats.enabled) WaypointStats.beamMeta++
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] sendBeamTransformMeta(id=$id) failed: ${it.message}") }
    }

    // --- Beam cleanup ---

    // Scale-0 hide for the beam — same pattern as hideFakeDisplay. Keeps both block
    // displays (and their block state) alive so a fade-boundary crossing costs one
    // metadata packet in each direction instead of destroy + spawn + full metadata.
    // After HIDDEN_ENTITY_TTL_TICKS hidden, destroys for real (see hideFakeDisplay).
    private fun hideBeamSlot(player: Player, beam: ActiveBeam, nowTick: Long) {
        if (!beam.isSpawned) return
        if (beam.hidden) {
            if (nowTick - beam.hiddenAtTick > HIDDEN_ENTITY_TTL_TICKS) destroyBeamSlot(player, beam)
            return
        }
        sendBeamTransformMeta(player, beam.id1, 0f, 0f, 0f, 0)
        sendBeamTransformMeta(player, beam.id2, 0f, 0f, 0f, 0)
        beam.hidden = true
        beam.hiddenAtTick = nowTick
        beam.lastTransformHash = null
        if (WaypointStats.enabled) WaypointStats.hides++
    }

    private fun destroyBeamSlot(player: Player, beam: ActiveBeam) {
        if (!beam.isSpawned) return
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(WrapperPlayServerDestroyEntities(beam.id1, beam.id2))
            if (WaypointStats.enabled) WaypointStats.destroys += 2
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] destroyBeam failed: ${it.message}") }
        beam.reset()
    }

    private fun destroyAllSlots(player: Player, state: PlayerWaypointState) {
        state.slots.values.forEach { slot ->
            if (slot.glowEntityId != -1) { setEntityGlow(player, slot.glowEntityId, false); slot.glowEntityId = -1 }
            destroyBeamSlot(player, slot.beam)
            destroyFakeDisplay(player, slot.label)
            destroyFakeDisplay(player, slot.symbol)
            slot.lastTargetLocation = null
            clearSlotVisualState(slot)
        }
        state.slots.clear()
        state.entitySelectorCache.clear()
        state.routePointCache.clear()
        state.cachedObjectiveTargets = emptyList()
        state.objectiveTargetsRefreshTick = 0L
    }

    // --- PacketEvents text display ---

    private fun spawnFakeText(player: Player, display: FakeTextDisplay, x: Double, y: Double, z: Double) {
        display.id = nextEntityId()
        display.spawnX = x; display.spawnY = y; display.spawnZ = z
        display.firstFrame = true
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(WrapperPlayServerSpawnEntity(
                display.id, Optional.of(UUID.randomUUID()), PEEntityTypes.TEXT_DISPLAY,
                PEVec3d(x, y, z), 0f, 0f, 0f, 0, Optional.empty()
            ))
            if (WaypointStats.enabled) WaypointStats.spawns++
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] spawnFakeText failed: ${it.message}") }
    }

    private fun destroyFakeDisplay(player: Player, display: FakeTextDisplay) {
        if (!display.isSpawned) return
        val id = display.id
        display.reset()
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(WrapperPlayServerDestroyEntities(id))
            if (WaypointStats.enabled) WaypointStats.destroys++
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] destroyFakeDisplay failed: ${it.message}") }
    }

    private fun hideFakeDisplay(player: Player, display: FakeTextDisplay, nowTick: Long) {
        if (!display.isSpawned) return
        if (display.hidden) {
            // TTL: hidden entities are cheap but not free client-side. If the display has
            // been hidden this long the player is parked (e.g. inside arrive radius) —
            // destroy for real. A later re-show takes the spawn path, which also appears
            // in place (first-frame duration 0), so there is no visual difference.
            if (nowTick - display.hiddenAtTick > HIDDEN_ENTITY_TTL_TICKS) destroyFakeDisplay(player, display)
            return
        }
        val meta = listOf(
            EntityData(8,  EntityDataTypes.INT,      0),
            EntityData(9,  EntityDataTypes.INT,      0),
            EntityData(10, EntityDataTypes.INT,      0),
            EntityData(12, EntityDataTypes.VECTOR3F, PEVec3f(0f, 0f, 0f)),
        )
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(WrapperPlayServerEntityMetadata(display.id, meta))
            if (WaypointStats.enabled) {
                WaypointStats.hides++
                if (display.isSymbol) WaypointStats.symbolMeta++ else WaypointStats.labelMeta++
            }
            display.lastMetadataHash = null
            display.hidden = true
            display.hiddenAtTick = nowTick
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] hideFakeDisplay failed: ${it.message}") }
    }

    private fun teleportFakeDisplay(player: Player, display: FakeTextDisplay, pos: Location) {
        if (!display.isSpawned) return
        // Position dedupe — skip the packet when the entity is already there
        // (stationary player with bob disabled would otherwise teleport every tick).
        if (abs(pos.x - display.spawnX) < 1e-4 &&
            abs(pos.y - display.spawnY) < 1e-4 &&
            abs(pos.z - display.spawnZ) < 1e-4) return
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(
                WrapperPlayServerEntityTeleport(
                    display.id, PEVec3d(pos.x, pos.y, pos.z), 0f, 0f, false
                )
            )
            display.spawnX = pos.x
            display.spawnY = pos.y
            display.spawnZ = pos.z
            if (WaypointStats.enabled) {
                if (display.isSymbol) WaypointStats.symbolTeleports++ else WaypointStats.labelTeleports++
            }
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] teleportFakeDisplay failed: ${it.message}") }
    }

    private fun sendFakeTextMeta(
        player: Player,
        display: FakeTextDisplay,
        text: Component,
        scale: Float,
        thinFactor: Float,
        seeThrough: Boolean,
        shadow: Boolean,
        opacity: Byte,
        bgColor: Int,
        lineWidth: Int,
        billboard: Byte,
        alignBits: Int,
        interp: Int,
    ) {
        if (!display.isSpawned) return
        val flagsByte = ((if (shadow) 0x01 else 0) or (if (seeThrough) 0x02 else 0) or alignBits).toByte()
        val finalScale = scale * thinFactor
        // First frame: duration 0 so the entity appears at full scale instantly at its
        // spawn position. Re-shows no longer need duration 0 here — the caller teleports
        // BEFORE this meta (the hide meta left teleport_duration = 0, so the reposition is
        // instant while invisible) and the normal duration then scales the text back in
        // place. That removes the follow-up resend the duration-0 hash used to force.
        val duration = if (display.firstFrame) 0 else interp
        // identityHashCode instead of Component.hashCode(): components are cached and reused
        // (parseDisplayText / input-hash gate), so identity is stable while content is
        // unchanged — and it avoids a full component-tree hash per call. A rebuilt-but-equal
        // component only costs one redundant (harmless) metadata send.
        // Manual rolling hash — the old listOf().hashCode() allocated a list and boxed
        // every primitive per display per tick just to decide "nothing changed".
        var metadataHash = System.identityHashCode(text)
        metadataHash = 31 * metadataHash + finalScale.toRawBits()
        metadataHash = 31 * metadataHash + (if (seeThrough) 1 else 0)
        metadataHash = 31 * metadataHash + (if (shadow) 1 else 0)
        metadataHash = 31 * metadataHash + opacity.toInt()
        metadataHash = 31 * metadataHash + bgColor
        metadataHash = 31 * metadataHash + lineWidth
        metadataHash = 31 * metadataHash + billboard.toInt()
        metadataHash = 31 * metadataHash + alignBits
        metadataHash = 31 * metadataHash + duration
        if (display.lastMetadataHash == metadataHash) return

        val meta = listOf(
            EntityData(8,  EntityDataTypes.INT,           0),
            EntityData(9,  EntityDataTypes.INT,           duration),
            EntityData(10, EntityDataTypes.INT,           duration),
            EntityData(11, EntityDataTypes.VECTOR3F,      PEVec3f(0f, 0f, 0f)),
            EntityData(12, EntityDataTypes.VECTOR3F,      PEVec3f(finalScale, finalScale, finalScale)),
            EntityData(13, EntityDataTypes.QUATERNION,    PEQuat(0f, 0f, 0f, 1f)),
            EntityData(14, EntityDataTypes.QUATERNION,    PEQuat(0f, 0f, 0f, 1f)),
            EntityData(15, EntityDataTypes.BYTE,          billboard),
            EntityData(16, EntityDataTypes.INT,           (15 shl 20) or (15 shl 4)),
            EntityData(17, EntityDataTypes.FLOAT,         64.0f),
            EntityData(23, EntityDataTypes.ADV_COMPONENT, text),
            EntityData(24, EntityDataTypes.INT,           lineWidth),
            EntityData(25, EntityDataTypes.INT,           bgColor),
            EntityData(26, EntityDataTypes.BYTE,          opacity),
            EntityData(27, EntityDataTypes.BYTE,          flagsByte),
        )
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(WrapperPlayServerEntityMetadata(display.id, meta))
            if (WaypointStats.enabled) {
                if (display.hidden) WaypointStats.reshows++
                if (display.isSymbol) WaypointStats.symbolMeta++ else WaypointStats.labelMeta++
            }
            display.lastMetadataHash = metadataHash
            display.firstFrame = false
            display.hidden = false
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] sendFakeTextMeta(id=${display.id}) failed: ${it::class.simpleName}: ${it.message}") }
    }

    private fun updateLabel(
        player: Player,
        slot: WaypointSlot,
        location: Location,
        markerName: String,
        distance: Double,
        directionArrow: String,
        thinFactor: Float,
        interp: Int,
        opacity: Int = entry.label.opacity.coerceIn(0, 255),
        index: Int = 0,
        total: Int = 1,
        isEntityTarget: Boolean = false,
        entityTypeName: String? = null,
        routePointIndex: Int? = null,
        routePointCount: Int = 0,
        routePointName: String? = null,
    ) {
        val isFirstFrame = !slot.label.isSpawned || slot.label.firstFrame
        if (!slot.label.isSpawned) spawnFakeText(player, slot.label, location.x, location.y, location.z)
        val wasHidden = slot.label.hidden

        // Cheap composite hash of every placeholder input. The 12-step replace chain
        // allocates a dozen intermediate strings per slot per tick — skip it entirely when
        // nothing the label shows has changed (String.hashCode is cached by the JVM).
        // distKey has the same granularity as formatDistance (1 m / 0.1 km), so the hash
        // changes exactly when the rendered distance text would.
        val template = entry.label.text.get(player)
        var inputsHash = template.hashCode()
        inputsHash = inputsHash * 31 + markerName.hashCode()
        inputsHash = inputsHash * 31 + distKey(distance)
        inputsHash = inputsHash * 31 + directionArrow.hashCode()
        inputsHash = inputsHash * 31 + (index * 64 + total)
        inputsHash = inputsHash * 31 + (entityTypeName?.hashCode() ?: 0)
        inputsHash = inputsHash * 31 + (routePointIndex ?: -1)
        inputsHash = inputsHash * 31 + routePointCount
        inputsHash = inputsHash * 31 + (routePointName?.hashCode() ?: 0)
        inputsHash = inputsHash * 31 + (if (isEntityTarget) 1 else 0)

        val cachedComponent = slot.label.lastComponent
        val text: Component = if (slot.label.lastInputsHash == inputsHash && cachedComponent != null) {
            cachedComponent
        } else {
            val routeIndex1 = if (routePointIndex != null) (routePointIndex + 1).toString() else ""
            val routeTotal = if (routePointIndex != null) routePointCount.toString() else ""
            val routeName = routePointName.orEmpty()
            val routeRemaining = if (routePointIndex != null) (routePointCount - routePointIndex).toString() else ""
            val rawText = template
                .replace("{name}", markerName)
                .replace("{distance}", formatDistance(distance))
                .replace("{direction}", directionArrow)
                .replace("{index}", (index + 1).toString())
                .replace("{total}", total.toString())
                .replace("{target_type}", if (isEntityTarget) "entity" else "objective")
                .replace("{entity_name}", if (isEntityTarget) markerName else "")
                .replace("{entity_type}", entityTypeName ?: "")
                .replace("{route_index}", routeIndex1)
                .replace("{route_total}", routeTotal)
                .replace("{route_name}", routeName)
                .replace("{route_remaining}", routeRemaining)
            slot.label.lastInputsHash = inputsHash
            parseDisplayText(slot.label, rawText)
        }
        // Re-show: teleport first — the hide meta left teleport_duration = 0, so the move
        // is instant while still invisible; the meta below restores scale in place with
        // the normal duration. One meta + one teleport, no follow-up resend.
        if (wasHidden) teleportFakeDisplay(player, slot.label, location)
        sendFakeTextMeta(
            player, slot.label, text,
            entry.label.scale, thinFactor,
            false, entry.label.shadow,
            opacity.coerceIn(0, 255).toByte(),
            labelBgColor, entry.label.lineWidth.coerceAtLeast(1),
            labelBillboard, labelAlignBits, interp,
        )
        if (!isFirstFrame && !wasHidden) teleportFakeDisplay(player, slot.label, location)
    }

    private fun updateSymbolDisplay(
        player: Player, slot: WaypointSlot, location: Location,
        scale: Float, thinFactor: Float = 1.0f, interp: Int = TEXT_INTERP,
    ) {
        val isFirstFrame = !slot.symbol.isSpawned || slot.symbol.firstFrame
        if (!slot.symbol.isSpawned) spawnFakeText(player, slot.symbol, location.x, location.y, location.z)
        val wasHidden = slot.symbol.hidden

        val text = parseDisplayText(slot.symbol, entry.symbol.text.get(player))

        // Re-show recipe: see updateLabel — teleport while invisible, then one normal meta.
        if (wasHidden) teleportFakeDisplay(player, slot.symbol, location)
        sendFakeTextMeta(
            player, slot.symbol, text,
            scale, thinFactor,
            false, shadow = false,
            entry.label.opacity.coerceIn(0, 255).toByte(),
            bgColor = 0, lineWidth = 1000,
            billboard = 3.toByte(), alignBits = 0, interp,
        )
        if (!isFirstFrame && !wasHidden) teleportFakeDisplay(player, slot.symbol, location)
    }

    // --- Entity glow (client-side only) ---

    private fun setEntityGlow(player: Player, entityId: Int, glow: Boolean, entity: Entity? = null) {
        val key = "${player.uniqueId}:$entityId"
        val baseFlags = if (glow) {
            entity?.let(::sharedEntityFlags)?.also { glowBaseFlags[key] = it }
                ?: glowBaseFlags[key]
                ?: 0
        } else {
            glowBaseFlags.remove(key) ?: 0
        }
        val flags = if (glow) (baseFlags.toInt() or 0x40).toByte()
                    else (baseFlags.toInt() and 0x40.inv()).toByte()
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(WrapperPlayServerEntityMetadata(
                entityId, listOf(EntityData(0, EntityDataTypes.BYTE, flags))
            ))
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] setEntityGlow(id=$entityId) failed: ${it.message}") }
    }

    private fun sharedEntityFlags(entity: Entity): Byte {
        var flags = 0
        if (entity.fireTicks > 0 || entity.isVisualFire) flags = flags or 0x01
        if (entity is LivingEntity) {
            if (entity.isSneaking) flags = flags or 0x02
            if (entity.isSwimming) flags = flags or 0x10
            if (entity.isInvisible) flags = flags or 0x20
            if (entity.isGliding) flags = flags or 0x80
        }
        return flags.toByte()
    }

    // --- Helpers ---

    private fun clearSlotVisualState(slot: WaypointSlot) {
        slot.lastVisualAnchor = null
        slot.lastVisualBaseY = null
        slot.lastBeamPointer = null
        slot.lastLabelLocation = null
        slot.lastSymbolLocation = null
        slot.arrivedLatch = false
        slot.labelHideLatch = false
        slot.beamFadeLatch = false
        slot.verticalColumnLatch = false
        // symbolSnapped is a latch too — a slot recycled after world change or stale sweep
        // must not wake up believing the symbol is still snapped to the old target.
        slot.symbolSnapped = false
    }

    private fun pruneEntitySelectorCache(state: PlayerWaypointState) {
        if (state.entitySelectorCache.isEmpty()) return
        val validKeys = entry.integrations.entityTargets.mapTo(HashSet()) { buildEntitySelectorCacheKey(it) }
        val minLiveTick = state.tickSerial - (ENTITY_SELECTOR_REFRESH_TICKS * 3L)
        state.entitySelectorCache.entries.removeIf { (key, cache) ->
            key !in validKeys || cache.nextRefreshTick < minLiveTick
        }
    }

    // Motion policy per visual type. Continuous alpha curves — the old discrete brackets
    // produced visible "gear shifts" whenever the delta crossed a bracket edge:
    //   BEAM        — firmest: high convergence even on tiny corrections, zero drag.
    //   LABEL       — damped at millimetric deltas to kill lateral shimmer, full track > ~2m.
    //   SYMBOL      — firmer than label so the icon doesn't wobble while following.
    //   SYMBOL_SNAP — pinned to the waypoint, converges hard.
    // Y passes through unsmoothed in normal tracking (bob must stay crisp; vertical motion
    // is already damped upstream via smoothedBaseY). During a handoff glide Y interpolates
    // too, so the sweep to the new target is a straight diagonal, not an L-shape.
    // Deltas above HANDOFF_GLIDE_START only occur when the target itself changed (route
    // advance, selector swap, route → final objective): those glide exponentially with a
    // guaranteed minimum catch-up speed instead of hard-snapping across the screen.
    // Callers always pass a fresh Location as `target`, so returning it directly is safe.
    private fun smoothLocation(previous: Location?, target: Location, profile: MotionProfile): Location {
        if (previous == null || previous.world != target.world) return target

        val dx = target.x - previous.x
        val dy = target.y - previous.y
        val dz = target.z - previous.z
        val distance = sqrt(dx * dx + dy * dy + dz * dz)
        if (distance < 1e-5) return target

        val gliding = distance > HANDOFF_GLIDE_START
        val alpha: Double = if (gliding) {
            // MAX_ALPHA cap: without it, deltas just above GLIDE_START got alpha ≈ 1.0
            // (min step ≥ distance) — short handoffs between nearby route points were a
            // 1-tick hard snap. Capped, they sweep in 2-3 ticks; large deltas are
            // unaffected because ALPHA·d wins over the min step there.
            // The beam gets firmer glide parameters than text: it is the largest visual
            // mass, so residual drag on a long handoff reads worst on it. 30 blocks
            // converge in ~5 ticks (vs ~7 with the text parameters), still continuous.
            if (profile == MotionProfile.BEAM)
                (maxOf(distance * BEAM_GLIDE_ALPHA, BEAM_GLIDE_MIN_STEP) / distance).coerceAtMost(BEAM_GLIDE_MAX_ALPHA)
            else
                (maxOf(distance * HANDOFF_GLIDE_ALPHA, HANDOFF_MIN_STEP) / distance).coerceAtMost(HANDOFF_MAX_ALPHA)
        } else when (profile) {
            MotionProfile.BEAM        -> lerp(0.70, 1.0, smoothstep(0.05, 2.0, distance))
            MotionProfile.LABEL       -> lerp(0.36, 1.0, smoothstep(0.10, 2.0, distance))
            MotionProfile.SYMBOL      -> lerp(0.46, 1.0, smoothstep(0.10, 2.0, distance))
            MotionProfile.SYMBOL_SNAP -> lerp(0.70, 1.0, smoothstep(0.05, 1.5, distance))
        }

        return Location(
            target.world,
            lerp(previous.x, target.x, alpha),
            if (gliding) lerp(previous.y, target.y, alpha) else target.y,
            lerp(previous.z, target.z, alpha),
            target.yaw,
            target.pitch,
        )
    }

    private fun calculateBob(): Double {
        if (!entry.bob.enabled || entry.bob.height <= 0.0) return 0.0
        val t = System.currentTimeMillis() / 1000.0
        return sin(t * entry.bob.speed * 2.0 * PI) * entry.bob.height
    }

    // Takes the already-resolved feet location — player.location allocates per call and
    // this runs per slot per tick.
    private fun calculateDirectionArrow(loc: Location, rawTargetLocation: Location): String {
        if (entry.target.verticalThreshold > 0.0) {
            val dy = rawTargetLocation.y - loc.y
            if (dy > entry.target.verticalThreshold) return "▲"
            if (dy < -entry.target.verticalThreshold) return "▼"
        }
        val ddx = rawTargetLocation.x - loc.x
        val ddz = rawTargetLocation.z - loc.z
        if (ddx * ddx + ddz * ddz < 0.01) return "↑"
        val targetYaw = Math.toDegrees(atan2(-ddx, ddz)).toFloat()
        val rel = ((targetYaw - loc.yaw + 360f) % 360f)
        return when {
            rel < 22.5f || rel >= 337.5f -> "↑"
            rel < 67.5f  -> "↗"
            rel < 112.5f -> "→"
            rel < 157.5f -> "↘"
            rel < 202.5f -> "↓"
            rel < 247.5f -> "↙"
            rel < 292.5f -> "←"
            else         -> "↖"
        }
    }

    private fun smoothstep(edge0: Double, edge1: Double, x: Double): Double {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }

    private fun lerp(a: Double, b: Double, t: Double): Double = a + t * (b - a)

    private fun formatDistance(meters: Double): String = when {
        meters >= 1000.0 -> "${"%.1f".format(meters / 1000.0)}km"
        else -> "${meters.roundToInt()}m"
    }

    // Same granularity as formatDistance: 1 m below 1 km, 0.1 km above. Used by the label
    // input hash so the text is only rebuilt when the rendered distance actually changes.
    private fun distKey(meters: Double): Int = when {
        meters >= 1000.0 -> 1_000_000 + (meters / 100.0).roundToInt()
        else -> meters.roundToInt()
    }

    private fun hasMeaningfullyMoved(state: PlayerWaypointState, playerEyes: Location): Boolean {
        val last = state.lastPlayerLocation ?: return true
        if (last.world != playerEyes.world) return true
        val yawDiff = ((playerEyes.yaw - last.yaw + 540f) % 360f) - 180f
        val pitchDiff = playerEyes.pitch - last.pitch
        return last.distanceSquared(playerEyes) > 0.0025
            || yawDiff * yawDiff > 0.25f
            || pitchDiff * pitchDiff > 0.25f
    }

    // --- Parse helpers ---

    private fun parseRGB(hex: String): Color {
        val v = hex.trim().removePrefix("#")
        return runCatching {
            if (v.length == 6) { val n = v.toInt(16); Color.fromRGB((n shr 16) and 0xFF, (n shr 8) and 0xFF, n and 0xFF) }
            else Color.fromRGB(0, 255, 136)
        }.getOrElse { Color.fromRGB(0, 255, 136) }
    }

    private fun parseMiniMessage(raw: String): Component =
        runCatching { miniMessage.deserialize(raw) }.getOrElse { Component.text(raw) }

    private fun parseDisplayText(display: FakeTextDisplay, raw: String): Component {
        if (display.lastRawText == raw) return display.lastComponent ?: Component.empty()
        return parseMiniMessage(raw).also {
            display.lastRawText = raw
            display.lastComponent = it
        }
    }

    private fun parseColorARGB(raw: String, defaultAlpha: Int, defaultRed: Int, defaultGreen: Int, defaultBlue: Int): Int {
        val value = raw.trim().removePrefix("#")
        return runCatching {
            when (value.length) {
                6 -> (defaultAlpha shl 24) or value.toInt(16)
                8 -> value.toLong(16).toInt()
                else -> null
            }
        }.getOrNull() ?: ((defaultAlpha shl 24) or (defaultRed shl 16) or (defaultGreen shl 8) or defaultBlue)
    }

    // --- Stale cleanup (legacy Bukkit entities only) ---

    private fun cleanupStaleDisplaysIfNeeded(player: Player, state: PlayerWaypointState) {
        if (state.staleCleanupDone) return
        state.staleCleanupDone = true
        if (!entry.performance.cleanupOnJoin) return
        val radius = entry.performance.cleanupRadius.coerceIn(1.0, 256.0)
        player.world.getNearbyEntities(player.location, radius, radius, radius).forEach { entity ->
            if (entity.scoreboardTags.any { it == TAG || it.startsWith("$TAG:") }) {
                entity.remove(); return@forEach
            }
            if (looksLikeLegacyWaypointDisplay(entity)) entity.remove()
        }
    }

    private fun looksLikeLegacyWaypointDisplay(entity: Entity): Boolean {
        if (entity.isPersistent || entity.isVisibleByDefault) return false
        if (entity is TextDisplay) {
            val text = runCatching { miniMessage.serialize(entity.text() ?: Component.empty()) }.getOrDefault("")
            return text.contains("{distance}") || text.isBlank()
        }
        if (entity is BlockDisplay) {
            val material = runCatching { entity.block.material }.getOrNull() ?: return false
            return material in setOf(
                Material.SLIME_BLOCK, Material.GREEN_STAINED_GLASS,
                entry.beam.outer,
                entry.beam.inner,
            )
        }
        return false
    }

    companion object {
        private const val TAG = "waypointrpg"
        private val entityIdCounter = AtomicInteger(Int.MAX_VALUE / 2)
        private fun nextEntityId() = entityIdCounter.decrementAndGet()
        const val DEBUG_BEAM = false
    }
}

// Shared across WaypointZoneTriggerDisplay (same package). Not a KSP entry.
internal data class WaypointTarget(
    val objective: LocatableObjective?,
    val position: Position?,
    val location: Location,
    val distance: Double,
    val routePointIndex: Int? = null,
    val routePointCount: Int = 0,
    val routePointName: String? = null,
    val customName: String? = null,
    val entityUUID: String? = null,
    val entityPriority: Int = 0,
    val entityTypeName: String? = null,
    val sourceId: String? = null,
) {
    // Slot-key memo — targets live in cachedObjectiveTargets for several ticks and the
    // display calls key() multiple times per tick (stale sweep, slot lookup, distinctBy);
    // positional keys build a world-UID string each time. Safe: targets are never shared
    // between display instances, and the key inputs are immutable constructor fields.
    var slotKeyCache: String? = null
}

// Read-only route application for zone trigger and BetterHUD.
// Does NOT advance the route index — only the visual display (TrackedLocatableWaypointDisplay) advances.
internal fun applyRouteReadOnly(
    player: Player,
    objectiveId: String,
    routes: List<WaypointRoute>,
    directTarget: WaypointTarget,
): WaypointTarget {
    val route = routes.firstOrNull { it.objectiveId == objectiveId && it.points.isNotEmpty() }
        ?: return directTarget
    val effectiveRouteId = route.routeId.ifBlank { objectiveId }
    val key = routeStateKey(player.uniqueId, objectiveId, effectiveRouteId)
    val index = globalRouteIndices.getOrDefault(key, 0)

    val resolvedPoints = route.points.mapIndexedNotNull { idx, point ->
        val position = runCatching { point.position.get(player) }.getOrNull() ?: return@mapIndexedNotNull null
        val location = runCatching { position.toBukkitLocation() }.getOrNull() ?: return@mapIndexedNotNull null
        Triple(idx, point, Pair(position, location))
    }
    val nextPoint = resolvedPoints.firstOrNull { it.first >= index } ?: return directTarget
    val (pos, loc) = nextPoint.third
    val dist = if (loc.world == player.location.world) player.location.distance(loc) else Double.POSITIVE_INFINITY
    val pointName = runCatching { nextPoint.second.name.get(player) }.getOrNull().orEmpty()
    return WaypointTarget(
        objective = directTarget.objective,
        position = pos,
        location = loc,
        distance = dist,
        routePointIndex = nextPoint.first,
        routePointCount = resolvedPoints.size,
        routePointName = pointName,
    )
}

// Stable key for zone-trigger per-target tracking.
// Entity UUID when available; NPC entry id via sourceId; otherwise objective+position.
internal fun WaypointTarget.zoneKey(): String {
    // sourceId first (mirrors slot key): a selector that swaps to another entity keeps the
    // same zone/BetterHUD identity — position updates in place instead of exit+enter churn.
    if (sourceId != null) return sourceId
    if (entityUUID != null) return "entity:$entityUUID"
    val worldKey = location.world?.uid?.toString() ?: location.world?.name ?: "?"
    val x = (location.x * 4.0).toLong()
    val y = (location.y * 4.0).toLong()
    val z = (location.z * 4.0).toLong()
    return "${objective?.id ?: customName ?: "pos"}:$worldKey:$x:$y:$z"
}

// --- Shared entity resolution (used by TrackedLocatableWaypointDisplay and WaypointZoneTriggerDisplay) ---

internal fun resolveEntityTarget(et: EntityWaypointTarget, player: Player): WaypointTarget? =
    resolveEntityTarget(et, player, state = null)

private fun resolveEntityTarget(
    et: EntityWaypointTarget,
    player: Player,
    state: PlayerWaypointState?,
): WaypointTarget? {
    val entity = when (et.targetType) {
        EntityTargetType.UUID -> {
            val uid = runCatching { UUID.fromString(et.uuid) }.getOrNull() ?: return null
            Bukkit.getEntity(uid)
        }
        EntityTargetType.NAME -> {
            if (et.name.isBlank()) return null
            resolveCachedWorldEntity(et, player, state) { entity ->
                entityMatchesName(entity, et.name)
            }
        }
        EntityTargetType.SCOREBOARD_TAG -> {
            if (et.tag.isBlank()) return null
            resolveCachedWorldEntity(et, player, state) { entity ->
                entity.scoreboardTags.contains(et.tag)
            }
        }
        EntityTargetType.TYPEWRITER_NPC -> {
            // Typewriter NPCs are packet-only — not in world.entities. Resolved separately.
            return resolveTypewriterNpcTarget(et, player)
        }
    } ?: return null

    if (entity.world != player.world) return null
    val loc = entity.location
    val dist = player.location.distance(loc)
    val label = et.displayName.get(player).ifBlank { entity.name }
    return WaypointTarget(
        objective = null, position = null, location = loc,
        distance = dist, customName = label,
        entityUUID = entity.uniqueId.toString(),
        entityPriority = et.priority,
        entityTypeName = entity.type.name,
        // Selector-based targets carry a stable sourceId: when NAME/SCOREBOARD_TAG matches
        // a different entity (old one died, a closer one appeared), the slot key stays the
        // same — the display glides to the new entity instead of destroy + respawn.
        sourceId = when (et.targetType) {
            EntityTargetType.NAME, EntityTargetType.SCOREBOARD_TAG -> "sel:" + buildEntitySelectorCacheKey(et)
            else -> null
        },
    )
}

private fun resolveCachedWorldEntity(
    et: EntityWaypointTarget,
    player: Player,
    state: PlayerWaypointState?,
    matches: (Entity) -> Boolean,
): Entity? {
    if (state == null) return scanWorldEntity(et, player, matches)

    val cacheKey = buildEntitySelectorCacheKey(et)
    val cache = state.entitySelectorCache.getOrPut(cacheKey) { CachedEntitySelector() }
    val currentTick = state.tickSerial

    cache.entityUuid?.let { cachedUuid ->
        val cachedEntity = Bukkit.getEntity(cachedUuid)
        if (cachedEntity != null && entityMatchesSelector(cachedEntity, player, et, matches)) {
            if (currentTick < cache.nextRefreshTick) return cachedEntity
        } else {
            cache.entityUuid = null
        }
    }

    val resolved = scanWorldEntity(et, player, matches)
    cache.entityUuid = resolved?.uniqueId
    cache.nextRefreshTick = currentTick + ENTITY_SELECTOR_REFRESH_TICKS
    if (resolved == null) state.entitySelectorCache.remove(cacheKey)
    return resolved
}

private fun scanWorldEntity(
    et: EntityWaypointTarget,
    player: Player,
    matches: (Entity) -> Boolean,
): Entity? {
    if (WaypointStats.enabled) WaypointStats.selectorScans++
    val playerLocation = player.location
    val maxDistanceSquared = et.maxDistance * et.maxDistance
    return player.world.entities
        .asSequence()
        .filter { it !is Player }
        .mapNotNull { entity ->
            if (entity.world != player.world) return@mapNotNull null
            val distanceSquared = entity.location.distanceSquared(playerLocation)
            if (distanceSquared > maxDistanceSquared) return@mapNotNull null
            if (!matches(entity)) return@mapNotNull null
            entity to distanceSquared
        }
        .minByOrNull { it.second }
        ?.first
}

private fun entityMatchesSelector(
    entity: Entity,
    player: Player,
    et: EntityWaypointTarget,
    matches: (Entity) -> Boolean,
): Boolean {
    if (entity is Player) return false
    if (entity.world != player.world) return false
    if (entity.location.distanceSquared(player.location) > et.maxDistance * et.maxDistance) return false
    return matches(entity)
}

private fun entityMatchesName(entity: Entity, expectedName: String): Boolean {
    if (entity.name.equals(expectedName, ignoreCase = true)) return true
    val customName = entity.customName()?.let(PLAIN_TEXT_SERIALIZER::serialize)
    return customName?.equals(expectedName, ignoreCase = true) == true
}

private fun buildEntitySelectorCacheKey(et: EntityWaypointTarget): String = when (et.targetType) {
    EntityTargetType.UUID -> "uuid:${et.uuid.lowercase()}"
    EntityTargetType.NAME -> "name:${et.name.lowercase()}:${et.maxDistance}"
    EntityTargetType.SCOREBOARD_TAG -> "tag:${et.tag.lowercase()}:${et.maxDistance}"
    EntityTargetType.TYPEWRITER_NPC -> "npc:${et.npcEntryId.lowercase()}"
}

internal fun findEntityByUuid(uuid: String): org.bukkit.entity.Entity? {
    val uid = runCatching { UUID.fromString(uuid) }.getOrNull() ?: return null
    return Bukkit.getEntity(uid)
}

// Typewriter NPC target — EntityExtension required at runtime.
// Wrapped in runCatching: returns null if EntityExtension absent or NPC not found.
// Priority: live SharedAudienceEntityDisplay position → NpcInstance.spawnLocation fallback.
internal fun resolveTypewriterNpcTarget(et: EntityWaypointTarget, player: Player): WaypointTarget? {
    if (et.npcEntryId.isBlank()) return null
    return runCatching {
        val audienceManager = plugin.get<AudienceManager>()
        val display = audienceManager
            .findDisplays(com.typewritermc.engine.paper.entry.entity.SharedAudienceEntityDisplay::class)
            .firstOrNull { it.instanceEntryRef.id == et.npcEntryId }

        val rawLoc: Location? = if (display != null && display.isSpawnedIn(player.uniqueId)) {
            display.position(player.uniqueId)?.toBukkitLocation()
        } else {
            Query.findById<com.typewritermc.entity.entries.entity.custom.NpcInstance>(et.npcEntryId)
                ?.spawnLocation?.toBukkitLocation()
        }
        val location = rawLoc ?: return@runCatching null
        if (location.world != player.world) return@runCatching null

        val dist = player.location.distance(location)
        val label = et.displayName.get(player).ifBlank {
            Query.findById<com.typewritermc.entity.entries.entity.custom.NpcInstance>(et.npcEntryId)?.name ?: et.npcEntryId
        }
        WaypointTarget(
            objective = null, position = null, location = location,
            distance = dist, customName = label,
            entityUUID = null,
            entityPriority = et.priority,
            entityTypeName = "NPC",
            sourceId = "npc:${et.npcEntryId}",
        )
    }.getOrNull()
}

// Shared resolver — used by TrackedLocatableWaypointDisplay, WaypointZoneTriggerDisplay,
// and WaypointBetterHudBridgeDisplay so target resolution logic stays in one place.
internal fun resolveWaypointTargets(
    player: Player,
    selection: WaypointTargetSelection,
    maxTargets: Int,
    entityTargets: List<EntityWaypointTarget> = emptyList(),
    includeObjectives: Boolean = true,
    includeEntities: Boolean = true,
): List<WaypointTarget> {
    if (maxTargets <= 0) return emptyList()
    val playerLoc = player.location

    val objectives: List<WaypointTarget> = if (includeObjectives) {
        player.trackedShowingObjectives()
            .filterIsInstance<LocatableObjective>()
            .flatMap { objective ->
                objective.positions(player).mapNotNull { position ->
                    val loc = runCatching { position.toBukkitLocation() }.getOrNull() ?: return@mapNotNull null
                    if (loc.world != playerLoc.world) return@mapNotNull null
                    WaypointTarget(
                        objective = objective, position = position, location = loc,
                        distance = playerLoc.distance(loc),
                    )
                }
            }
            .toList()
    } else emptyList()

    val entities: List<WaypointTarget> = if (includeEntities) {
        entityTargets.mapNotNull { resolveEntityTarget(it, player) }
    } else emptyList()

    val all = objectives + entities
    if (all.isEmpty()) return emptyList()

    return when (selection) {
        WaypointTargetSelection.CLOSEST ->
            all.sortedWith(
                compareBy<WaypointTarget> { it.distance }
                    .thenByDescending { it.objective?.priority ?: it.entityPriority }
                    .thenBy { it.objective?.id ?: it.customName ?: "" }
            )
        WaypointTargetSelection.HIGHEST_PRIORITY ->
            all.sortedWith(
                compareByDescending<WaypointTarget> { it.objective?.priority ?: it.entityPriority }
                    .thenBy { it.distance }
                    .thenBy { it.objective?.id ?: it.customName ?: "" }
            )
    }.take(maxTargets)
}
