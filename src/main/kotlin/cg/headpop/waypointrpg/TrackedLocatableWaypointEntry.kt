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
import com.typewritermc.core.entries.priority
import com.typewritermc.core.extension.annotations.Colored
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Placeholder
import com.typewritermc.core.extension.annotations.WithRotation
import com.typewritermc.core.utils.point.Position
import com.typewritermc.engine.paper.entry.entries.AudienceDisplay
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.TickableDisplay
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.entry.entries.get
import com.typewritermc.engine.paper.plugin
import com.typewritermc.engine.paper.utils.toBukkitLocation
import com.typewritermc.quest.entries.interfaces.LocatableObjective
import com.typewritermc.quest.entries.trackedShowingObjectives
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Entity
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

    @Help("Server ticks between beam and trail updates. 5 = 4 updates/sec. Label tracks every tick regardless.")
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

data class WaypointTrailConfig(
    @Help("Draw a ground particle trail from the player toward the objective. Direct line only — does not follow terrain obstacles.")
    val enabled: Boolean = false,

    @Help("Only draw the trail within this distance from the player.")
    val range: Double = 60.0,

    @Help("Blocks between each trail particle dot. Lower = denser. Max 100 particles per update.")
    val spacing: Double = 2.5,

    @Help("Trail particle color in hex #RRGGBB.")
    val color: String = "#00ff88",

    @Help("Trail particle size (0.1–4.0).")
    val size: Float = 0.8f,

    @Help("Animate the trail as a traveling sine wave rolling toward the objective.")
    val wave: Boolean = false,

    @Help("Wave lateral swing amplitude in blocks.")
    val waveWidth: Double = 0.4,

    @Help("Number of complete wave cycles along the full trail length.")
    val waveCycles: Double = 1.5,

    @Help("Wave travel speed in cycles per second.")
    val waveSpeed: Double = 1.0,
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

data class WaypointRoute(
    @Help("Objective ID this route applies to.")
    val objectiveId: String = "",
    @Help("Intermediate waypoints along the path to guide the player.")
    val points: List<WaypointRoutePoint> = emptyList(),
)

data class EntityWaypointTarget(
    @Help("Entity UUID (preferred) or entity name to follow.")
    val entityId: String = "",
    @Help("Label text. MiniMessage. Blank = entity's name.")
    @Colored @Placeholder
    val displayName: Var<String> = ConstVar(""),
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

    @Help("Ground particle trail from the player toward the objective.")
    val trail: WaypointTrailConfig = WaypointTrailConfig(),

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
    val isSpawned get() = id1 != -1
    fun reset() {
        id1 = -1; id2 = -1
        lastX = 0.0; lastY = 0.0; lastZ = 0.0
        sid1 = -1; sid2 = -1
        disabled = false
    }
}

private class FakeTextDisplay {
    var id: Int = -1
    var spawnX: Double = 0.0
    var spawnY: Double = 0.0
    var spawnZ: Double = 0.0
    var firstFrame: Boolean = true
    val isSpawned get() = id != -1
    fun reset() { id = -1; spawnX = 0.0; spawnY = 0.0; spawnZ = 0.0; firstFrame = true }
}

private class WaypointSlot {
    val beam = ActiveBeam()
    val label = FakeTextDisplay()
    val symbol = FakeTextDisplay()
    var lastTargetLocation: Location? = null
    var lastVisualAnchor: Vector? = null
    var lastVisualBaseY: Double? = null
    var symbolSnapped: Boolean = false
    var glowEntityId: Int = -1
}

private class PlayerWaypointState {
    val slots = LinkedHashMap<String, WaypointSlot>()
    val routeIndices = HashMap<String, Int>()
    var lastPlayerLocation: Location? = null
    var staleCleanupDone = false
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
    private var tickCounter = 0
    private var beamTickCounter = 0

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
        runSync { destroyAllSlots(player, state) }
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
        }
    }

    override fun dispose() {
        val snapshot = states.entries.toList()
        states.clear()
        runSync {
            snapshot.forEach { (uuid, state) ->
                val onlinePlayer = Bukkit.getPlayer(uuid)
                if (onlinePlayer != null) destroyAllSlots(onlinePlayer, state)
                else state.slots.values.forEach { slot -> slot.label.reset(); slot.symbol.reset() }
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
        cleanupStaleDisplaysIfNeeded(player, state)

        val playerEyes = player.eyeLocation
        val playerMoved = force || hasMeaningfullyMoved(state, playerEyes)

        val targets = resolveTargets(player, state)

        val activeKeys = targets.map { it.key() }.toSet()
        val staleKeys = state.slots.keys.filter { it !in activeKeys }
        staleKeys.forEach { key ->
            val slot = state.slots.remove(key) ?: return@forEach
            if (slot.glowEntityId != -1) { setEntityGlow(player, slot.glowEntityId, false); slot.glowEntityId = -1 }
            destroyBeamSlot(player, slot.beam)
            destroyFakeDisplay(player, slot.label)
            destroyFakeDisplay(player, slot.symbol)
        }

        if (targets.isEmpty()) {
            state.lastPlayerLocation = playerEyes.clone()
            return
        }

        val bobActive = entry.bob.enabled && entry.bob.height > 0.0
        val shouldUpdate = force || playerMoved || bobActive

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

        state.lastPlayerLocation = playerEyes.clone()

        targets.forEachIndexed { index, target ->
            val key = target.key()
            val slot = state.slots.getOrPut(key) { WaypointSlot() }
            updateSlot(player, state, slot, target, playerEyes, force || playerMoved || bobActive, fullUpdate, fullBeamUpdate, index, targets.size)
        }
    }

    // --- Per-slot update ---

    private fun updateSlot(
        player: Player,
        state: PlayerWaypointState,
        slot: WaypointSlot,
        target: WaypointTarget,
        playerEyes: Location,
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
            slot.lastVisualAnchor = null
            slot.lastVisualBaseY = null
            return
        }

        val distance = playerEyes.distance(targetLocation)
        val dx = targetLocation.x - playerEyes.x
        val dz = targetLocation.z - playerEyes.z
        val horizontalDist = sqrt(dx * dx + dz * dz)
        val verticalDelta = targetLocation.y - playerEyes.y
        val verticalColumnMode = horizontalDist <= entry.symbol.snapRange && abs(verticalDelta) > entry.target.verticalThreshold

        if (entry.symbol.enabled) {
            val snapDist = entry.symbol.snapRange.coerceAtLeast(0.0)
            val leaveDist = entry.symbol.snapLeave.coerceAtLeast(snapDist + 0.1)
            if (!slot.symbolSnapped && horizontalDist <= snapDist) slot.symbolSnapped = true
            else if (slot.symbolSnapped && horizontalDist > leaveDist) slot.symbolSnapped = false
        }

        if (entry.general.hideOnArrive && distance <= entry.general.arriveRadius) {
            if (slot.glowEntityId != -1) { setEntityGlow(player, slot.glowEntityId, false); slot.glowEntityId = -1 }
            destroyBeamSlot(player, slot.beam)
            destroyFakeDisplay(player, slot.label)
            slot.lastVisualAnchor = null
            slot.lastVisualBaseY = null
            if (entry.symbol.enabled) {
                val bobY = calculateBob()
                val arrivedPos = target.location.clone().add(0.0, entry.symbol.snapHeight + bobY, 0.0)
                updateSymbolDisplay(player, slot, arrivedPos, entry.symbol.maxScale, 1.0f)
            } else {
                destroyFakeDisplay(player, slot.symbol)
            }
            return
        }

        val slotMoved = slot.lastTargetLocation?.let { last ->
            last.world != targetLocation.world || last.distanceSquared(targetLocation) > 0.0025
        } ?: true
        slot.lastTargetLocation = targetLocation.clone()

        val doUpdate = shouldUpdate || slotMoved
        val labelInterp = if (fullUpdate) entry.general.tickRate.coerceAtLeast(1) else 1

        val markerName = target.markerName(player)
        val toTarget = targetLocation.toVector().subtract(playerEyes.toVector())
        if (toTarget.lengthSquared() <= 0.0001) {
            destroyBeamSlot(player, slot.beam)
            destroyFakeDisplay(player, slot.label)
            slot.lastVisualAnchor = null
            slot.lastVisualBaseY = null
            return
        }

        // Bob and thinFactor computed before velocity decomposition
        val bobY = calculateBob()
        val thinFactor = smoothstep(entry.beam.fadeEnd, entry.beam.fadeStart, distance).toFloat()

        // Horizontal-only direction — avoids zoom effect when waypoint is above/below player
        val horizontalToTarget = Vector(toTarget.x, 0.0, toTarget.z)
        val desiredXZ = if (horizontalToTarget.lengthSquared() > 0.0001) {
            horizontalToTarget.clone().normalize()
        } else {
            val look = playerEyes.direction.clone().setY(0.0)
            if (look.lengthSquared() > 0.0001) look.normalize() else Vector(0.0, 0.0, 1.0)
        }
        // 3D direction used only for the FOV angle check (correct elevation angle)
        val desired3D = toTarget.normalize()

        // Lateral separation for multiple simultaneous objectives.
        // Right vector = desiredXZ rotated 90° CW in the horizontal plane.
        // Lane 0 of 2 shifts left, lane 1 shifts right; center target (3 of 3) gets zero offset.
        // Beam always points at the real target — this offset only affects label and symbol.
        val lateralVec: Vector = if (total > 1 && entry.label.multiOffset > 0.0) {
            val laneIndex = index - (total - 1) / 2.0
            Vector(desiredXZ.z, 0.0, -desiredXZ.x).multiply(laneIndex * entry.label.multiOffset)
        } else {
            Vector(0.0, 0.0, 0.0)
        }

        // Velocity look-ahead with lateral damping:
        //   parallel component (toward waypoint) = full look-ahead
        //   perpendicular component (strafe)     = 35% of look-ahead
        // Prevents the label from jerking when the player strafes past the waypoint.
        val vel = player.velocity.clone().setY(0.0)
        val speed = vel.length()
        val speed01 = smoothstep(0.35, 1.50, speed).coerceIn(0.0, 1.0)
        val lookAheadTicks = 1.0 + 2.0 * speed01
        val parallelAmount = vel.dot(desiredXZ)
        val parallel = desiredXZ.clone().multiply(parallelAmount)
        val perpendicular = vel.clone().subtract(parallel)
        val velocityOffset = parallel.multiply(lookAheadTicks)
            .add(perpendicular.multiply(lookAheadTicks * 0.35))

        val beamMaxHalf = maxOf(entry.beam.width, entry.beam.depth) * thinFactor / 2.0
        val beamFrontDepth = horizontalDist - beamMaxHalf - 0.08
        val safeClamp = if (beamFrontDepth > 1.0) minOf(entry.label.floatDist, beamFrontDepth) else entry.label.floatDist

        val directionArrow = calculateDirectionArrow(player, target.location)

        val shouldBeVisible = horizontalDist > entry.label.hideRange && thinFactor > 0.01f
        val shouldUpdateTransform = !entry.performance.lazyUpdate || doUpdate

        val labelBaseVisible = shouldBeVisible
            && (entry.general.mode == WaypointType.HOLOGRAM || entry.general.mode == WaypointType.BOTH)

        val fovAlphaFactor: Double = if (!labelBaseVisible) {
            0.0
        } else if (entry.label.fov <= 0.0) {
            1.0
        } else {
            val forward = playerEyes.direction.normalize()
            val dot = forward.dot(desired3D).coerceIn(-1.0, 1.0)
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

        val finalOpacity = (entry.label.opacity.coerceIn(0, 255) * fovAlphaFactor).roundToInt().coerceIn(0, 255)
        val labelShouldBeVisible = labelBaseVisible && finalOpacity > 3

        val visualAnchor: Vector? = when {
            !shouldBeVisible -> {
                slot.lastVisualAnchor = null
                slot.lastVisualBaseY = null
                null
            }
            shouldUpdateTransform -> {
                val rawAnchor = playerEyes.toVector()
                    .add(desiredXZ.clone().multiply(safeClamp))
                    .add(velocityOffset)
                val verticalHint = (verticalDelta * 0.08).coerceIn(-1.25, 1.25)
                val rawBaseY = playerEyes.y + entry.label.height + verticalHint

                val verticalSpeed = abs(player.velocity.y)
                val baseAlphaY = if (verticalSpeed > 0.45) 0.24 else 0.18
                val maxStepY   = if (verticalSpeed > 0.45) 0.24 else 0.16
                val smoothedBaseY = slot.lastVisualBaseY?.let { last ->
                    last + ((rawBaseY - last) * baseAlphaY).coerceIn(-maxStepY, maxStepY)
                } ?: rawBaseY
                slot.lastVisualBaseY = smoothedBaseY
                rawAnchor.y = smoothedBaseY + bobY

                val rawDeltaXZ = slot.lastVisualAnchor?.let { prev ->
                    Vector(rawAnchor.x - prev.x, 0.0, rawAnchor.z - prev.z).length()
                } ?: 0.0
                var alphaXZ = when {
                    rawDeltaXZ > 1.2  -> 0.95
                    rawDeltaXZ > 0.6  -> 0.85
                    rawDeltaXZ > 0.25 -> 0.70
                    else              -> 0.55
                }
                if (rawDeltaXZ > 2.0) alphaXZ = 1.0
                else if (rawDeltaXZ > 1.0) alphaXZ = maxOf(alphaXZ, 0.90)

                val smoothed = slot.lastVisualAnchor?.let { prev ->
                    Vector(
                        lerp(prev.x, rawAnchor.x, alphaXZ),
                        rawAnchor.y,
                        lerp(prev.z, rawAnchor.z, alphaXZ),
                    )
                } ?: rawAnchor.clone()

                slot.lastVisualAnchor = smoothed.clone()
                smoothed
            }
            else -> slot.lastVisualAnchor
        }

        // --- Label ---
        if (!labelShouldBeVisible || verticalColumnMode) {
            hideFakeDisplay(player, slot.label)
            if (verticalColumnMode) slot.lastVisualBaseY = null
        } else if (shouldUpdateTransform && visualAnchor != null) {
            val labelPos = visualAnchor.clone().add(lateralVec).toLocation(player.world)
            updateLabel(player, slot, labelPos, markerName, distance, directionArrow, 1.0f, labelInterp, finalOpacity, index, total)
        }

        // --- Symbol ---
        if (entry.symbol.enabled) {
            if (slot.symbolSnapped || verticalColumnMode) {
                // Snapped/arrived: symbol sits directly on the target — no lateral offset.
                if (doUpdate) {
                    val snapPos = target.location.clone().add(0.0, entry.symbol.snapHeight + bobY, 0.0)
                    if (entry.symbol.snapPosition == SymbolSnapPosition.FRONT_OF_BEAM) {
                        val toPlayer = playerEyes.toVector().subtract(snapPos.toVector()).setY(0.0)
                        if (toPlayer.lengthSquared() > 0.001) {
                            snapPos.add(toPlayer.normalize().multiply(beamMaxHalf + 0.15))
                        }
                    }
                    updateSymbolDisplay(player, slot, snapPos, entry.symbol.minScale, 1.0f, labelInterp)
                }
            } else if (!shouldBeVisible) {
                hideFakeDisplay(player, slot.symbol)
            } else if (shouldUpdateTransform && visualAnchor != null) {
                val symbolScale = lerp(
                    entry.symbol.minScale.toDouble(), entry.symbol.maxScale.toDouble(),
                    smoothstep(entry.symbol.nearDist, entry.symbol.farDist, distance)
                ).toFloat()
                val symbolPos = visualAnchor.clone().add(lateralVec).toLocation(player.world).add(0.0, entry.symbol.offset, 0.0)
                updateSymbolDisplay(player, slot, symbolPos, symbolScale, thinFactor, labelInterp)
            }
        } else {
            destroyFakeDisplay(player, slot.symbol)
        }

        // --- Beam (independent tick rate — fullBeamUpdate defaults to every tick for smooth follow) ---
        if (fullBeamUpdate) {
            if (entry.general.mode == WaypointType.BEAM || entry.general.mode == WaypointType.BOTH) {
                if (doUpdate) {
                    if (thinFactor <= 0.01f) {
                        destroyBeamSlot(player, slot.beam)
                    } else {
                        val pointerPos = calculateBeamPosition(player.location, targetLocation)
                        updateBeam(player, slot.beam, pointerPos, thinFactor, player.location.y)
                    }
                }
            } else {
                destroyBeamSlot(player, slot.beam)
            }

            // --- Entity glow (tickRate ticks) ---
            if (entry.integrations.entityGlow && target.entityUUID != null) {
                val entity = findEntityById(target.entityUUID, player.world)
                val entityId = entity?.entityId ?: -1
                val inRange = entity != null && entity.location.world == player.world
                    && player.location.distance(entity.location) <= entry.integrations.glowRange
                if (inRange && entityId != -1) {
                    if (slot.glowEntityId != entityId) {
                        if (slot.glowEntityId != -1) setEntityGlow(player, slot.glowEntityId, false)
                        setEntityGlow(player, entityId, true)
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

        // --- Path trail (full ticks only) ---
        if (entry.trail.enabled && fullUpdate) {
            spawnPathParticles(player, playerEyes, targetLocation, distance)
        }
    }

    // --- Target resolution ---

    private fun resolveTargets(player: Player, state: PlayerWaypointState): List<WaypointTarget> {
        val playerLocation = player.location
        val raw = player.trackedShowingObjectives()
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

        val entityRaw = entry.integrations.entityTargets.mapNotNull { et ->
            val entity = findEntityById(et.entityId, player.world) ?: return@mapNotNull null
            val loc = entity.location
            val distance = if (loc.world == player.location.world) player.location.distance(loc) else Double.POSITIVE_INFINITY
            val displayName = et.displayName.get(player).ifBlank { entity.name }
            WaypointTarget(
                objective = null, position = null, location = loc,
                distance = distance, customName = displayName, entityUUID = entity.uniqueId.toString()
            )
        }

        if (raw.isEmpty() && entityRaw.isEmpty()) return emptyList()
        if (entry.general.maxTargets <= 0) return emptyList()

        val sorted = when (entry.general.selection) {
            WaypointTargetSelection.CLOSEST ->
                raw.sortedWith(
                    compareBy<WaypointTarget> { it.distance }
                        .thenByDescending { it.objective?.priority ?: 0 }
                        .thenBy { it.objective?.id ?: "" }
                )
            WaypointTargetSelection.HIGHEST_PRIORITY ->
                raw.sortedWith(
                    compareByDescending<WaypointTarget> { it.objective?.priority ?: 0 }
                        .thenBy { it.distance }
                        .thenBy { it.objective?.id ?: "" }
                )
        }

        return sorted
            .take(entry.general.maxTargets)
            .map { applyRoute(player, state, it) } + entityRaw
    }

    private fun findEntityById(entityId: String, world: org.bukkit.World): org.bukkit.entity.Entity? {
        val uuid = runCatching { UUID.fromString(entityId) }.getOrNull()
        if (uuid != null) return Bukkit.getEntity(uuid)
        return world.entities.firstOrNull { it.name == entityId }
    }

    private fun applyRoute(player: Player, state: PlayerWaypointState, directTarget: WaypointTarget): WaypointTarget {
        if (directTarget.entityUUID != null) return directTarget
        val objectiveId = directTarget.objective?.id ?: return directTarget
        val route = entry.routes.firstOrNull { it.objectiveId == objectiveId && it.points.isNotEmpty() }
            ?: return directTarget

        val resolvedPoints = route.points.mapIndexedNotNull { index, point ->
            val position = runCatching { point.position.get(player) }.getOrNull() ?: return@mapIndexedNotNull null
            val location = runCatching { position.toBukkitLocation() }.getOrNull() ?: return@mapIndexedNotNull null
            ResolvedRoutePoint(index, point, position, location)
        }
        if (resolvedPoints.isEmpty()) return directTarget

        var index = state.routeIndices.getOrDefault(objectiveId, 0).coerceIn(0, resolvedPoints.size)
        val playerLocation = player.location
        for (resolved in resolvedPoints) {
            if (resolved.index < index) continue
            if (resolved.location.world != playerLocation.world) continue
            if (playerLocation.distance(resolved.location) <= resolved.point.radius.coerceAtLeast(0.1))
                index = resolved.index + 1
        }
        state.routeIndices[objectiveId] = index.coerceIn(0, resolvedPoints.size)

        val nextPoint = resolvedPoints.firstOrNull { it.index >= state.routeIndices[objectiveId]!! }
            ?: return directTarget
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

    private data class ResolvedRoutePoint(
        val index: Int,
        val point: WaypointRoutePoint,
        val position: Position,
        val location: Location,
    )

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

    private fun WaypointTarget.key(): String {
        if (entityUUID != null) return "entity:$entityUUID"
        val worldKey = location.world?.uid?.toString() ?: location.world?.name ?: "unknown"
        val x = (location.x * 100.0).roundToInt()
        val y = (location.y * 100.0).roundToInt()
        val z = (location.z * 100.0).roundToInt()
        val route = routePointIndex?.let { "r$it/$routePointCount" } ?: "d"
        return "${objective?.id ?: "unknown"}:$route:$worldKey:$x:$y:$z"
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
            val baseY = minOf(pointerPos.y - 20.0, player.location.y - 20.0)
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
            // Metadata first (sets teleport_duration = interp so the client knows to interpolate),
            // then teleport packet (client uses the duration it just received to smooth the move).
            sendBeamTransformMeta(player, beam.id1, sx1, sz1, scaleY, interp)
            sendBeamTransformMeta(player, beam.id2, sx2, sz2, scaleY, interp)
            teleportBeamEntities(player, beam, bx, beamBaseY, bz)
            beam.lastX = bx; beam.lastY = beamBaseY; beam.lastZ = bz
        }
    }

    private fun teleportBeamEntities(player: Player, beam: ActiveBeam, x: Double, y: Double, z: Double) {
        if (!beam.isSpawned) return
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(WrapperPlayServerEntityTeleport(beam.id1, PEVec3d(x, y, z), 0f, 0f, false))
            user.sendPacket(WrapperPlayServerEntityTeleport(beam.id2, PEVec3d(x, y, z), 0f, 0f, false))
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
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] sendBeamTransformMeta(id=$id) failed: ${it.message}") }
    }

    // --- Beam cleanup ---

    private fun destroyBeamSlot(player: Player, beam: ActiveBeam) {
        if (!beam.isSpawned) return
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(WrapperPlayServerDestroyEntities(beam.id1, beam.id2))
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] destroyBeam failed: ${it.message}") }
        beam.reset()
    }

    private fun destroyAllSlots(player: Player, state: PlayerWaypointState) {
        state.slots.values.forEach { slot ->
            if (slot.glowEntityId != -1) { setEntityGlow(player, slot.glowEntityId, false); slot.glowEntityId = -1 }
            destroyBeamSlot(player, slot.beam)
            destroyFakeDisplay(player, slot.label)
            destroyFakeDisplay(player, slot.symbol)
        }
        state.slots.clear()
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
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] spawnFakeText failed: ${it.message}") }
    }

    private fun destroyFakeDisplay(player: Player, display: FakeTextDisplay) {
        if (!display.isSpawned) return
        val id = display.id
        display.reset()
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(WrapperPlayServerDestroyEntities(id))
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] destroyFakeDisplay failed: ${it.message}") }
    }

    private fun hideFakeDisplay(player: Player, display: FakeTextDisplay) {
        if (!display.isSpawned) return
        val meta = listOf(
            EntityData(8,  EntityDataTypes.INT,      0),
            EntityData(9,  EntityDataTypes.INT,      0),
            EntityData(10, EntityDataTypes.INT,      0),
            EntityData(12, EntityDataTypes.VECTOR3F, PEVec3f(0f, 0f, 0f)),
        )
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(WrapperPlayServerEntityMetadata(display.id, meta))
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] hideFakeDisplay failed: ${it.message}") }
    }

    private fun teleportFakeDisplay(player: Player, display: FakeTextDisplay, pos: Location) {
        if (!display.isSpawned) return
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
        val duration = if (display.firstFrame) { display.firstFrame = false; 0 } else interp

        val meta = listOf(
            EntityData(8,  EntityDataTypes.INT,           0),
            EntityData(9,  EntityDataTypes.INT,           duration),
            EntityData(10, EntityDataTypes.INT,           if (duration == 0) 0 else 1),
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
    ) {
        val isFirstFrame = !slot.label.isSpawned || slot.label.firstFrame
        if (!slot.label.isSpawned) spawnFakeText(player, slot.label, location.x, location.y, location.z)

        val rawText = entry.label.text.get(player)
            .replace("{name}", markerName)
            .replace("{distance}", formatDistance(distance))
            .replace("{direction}", directionArrow)
            .replace("{index}", (index + 1).toString())
            .replace("{total}", total.toString())
        val text = parseMiniMessage(rawText)
        val showBg = entry.label.background && entry.general.mode != WaypointType.BOTH
        val bgColor = if (showBg) parseColorARGB(entry.label.bgColor, 128, 0, 0, 0) else 0
        val alignBits = when (entry.label.align.trim().uppercase()) { "LEFT" -> 8; "RIGHT" -> 16; else -> 0 }
        val billboard = when (entry.label.billboard.trim().uppercase()) { "FIXED" -> 0; "VERTICAL" -> 1; "HORIZONTAL" -> 2; else -> 3 }.toByte()

        sendFakeTextMeta(
            player, slot.label, text,
            entry.label.scale, thinFactor,
            false, entry.label.shadow,
            opacity.coerceIn(0, 255).toByte(),
            bgColor, entry.label.lineWidth.coerceAtLeast(1),
            billboard, alignBits, interp,
        )
        if (!isFirstFrame) teleportFakeDisplay(player, slot.label, location)
    }

    private fun updateSymbolDisplay(
        player: Player, slot: WaypointSlot, location: Location,
        scale: Float, thinFactor: Float = 1.0f, interp: Int = entry.general.tickRate.coerceAtLeast(1),
    ) {
        val isFirstFrame = !slot.symbol.isSpawned || slot.symbol.firstFrame
        if (!slot.symbol.isSpawned) spawnFakeText(player, slot.symbol, location.x, location.y, location.z)

        val text = parseMiniMessage(entry.symbol.text.get(player))

        sendFakeTextMeta(
            player, slot.symbol, text,
            scale, thinFactor,
            false, shadow = false,
            entry.label.opacity.coerceIn(0, 255).toByte(),
            bgColor = 0, lineWidth = 1000,
            billboard = 3.toByte(), alignBits = 0, interp,
        )
        if (!isFirstFrame) teleportFakeDisplay(player, slot.symbol, location)
    }

    // --- Entity glow (client-side only) ---

    private fun setEntityGlow(player: Player, entityId: Int, glow: Boolean) {
        val flags = if (glow) 0x40.toByte() else 0x00.toByte()
        runCatching {
            val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
            user.sendPacket(WrapperPlayServerEntityMetadata(
                entityId, listOf(EntityData(0, EntityDataTypes.BYTE, flags))
            ))
        }.onFailure { Bukkit.getLogger().warning("[WaypointRPG] setEntityGlow(id=$entityId) failed: ${it.message}") }
    }

    // --- Path trail ---

    private fun spawnPathParticles(player: Player, from: Location, to: Location, distance: Double) {
        if (distance > entry.trail.range) return
        val world = from.world ?: return
        val spacing = entry.trail.spacing.coerceAtLeast(0.5)
        val steps = (distance / spacing).toInt().coerceIn(1, 100)
        val rgb = parseRGB(entry.trail.color)
        val dust = Particle.DustOptions(rgb, entry.trail.size.coerceIn(0.1f, 4f))

        val ddx = to.x - from.x
        val ddz = to.z - from.z
        val pathLen = sqrt(ddx * ddx + ddz * ddz)
        val perpX = if (pathLen > 0.01) -ddz / pathLen else 0.0
        val perpZ = if (pathLen > 0.01) ddx / pathLen else 0.0
        val time = System.currentTimeMillis() / 1000.0

        for (i in 1 until steps) {
            val t = i.toDouble() / steps
            var px = lerp(from.x, to.x, t)
            var pz = lerp(from.z, to.z, t)
            var extraY = 0.0

            if (entry.trail.wave) {
                val phase = t * 2.0 * PI * entry.trail.waveCycles - time * entry.trail.waveSpeed * 2.0 * PI
                val wave = sin(phase) * entry.trail.waveWidth
                px += perpX * wave
                pz += perpZ * wave
                extraY = abs(wave) * 0.4
            }

            val blockY = world.getHighestBlockYAt(px.toInt(), pz.toInt()).toDouble()
            player.spawnParticle(Particle.DUST, px, blockY + 1.1 + extraY, pz, 1, 0.0, 0.0, 0.0, 0.0, dust)
        }
    }

    // --- Helpers ---

    private fun calculateBob(): Double {
        if (!entry.bob.enabled || entry.bob.height <= 0.0) return 0.0
        val t = System.currentTimeMillis() / 1000.0
        return sin(t * entry.bob.speed * 2.0 * PI) * entry.bob.height
    }

    private fun calculateDirectionArrow(player: Player, rawTargetLocation: Location): String {
        val loc = player.location
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

// Internal data class — not exposed as a KSP entry
private data class WaypointTarget(
    val objective: LocatableObjective?,
    val position: Position?,
    val location: Location,
    val distance: Double,
    val routePointIndex: Int? = null,
    val routePointCount: Int = 0,
    val routePointName: String? = null,
    val customName: String? = null,
    val entityUUID: String? = null,
)
