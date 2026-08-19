package cg.headpop.waypointrpg

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Colored
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Placeholder
import com.typewritermc.engine.paper.entry.entries.AudienceDisplay
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.TickableDisplay
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.entry.entries.get
import com.typewritermc.engine.paper.entry.PlaceholderEntry
import com.typewritermc.engine.paper.entry.PlaceholderParser
import com.typewritermc.engine.paper.entry.literal
import com.typewritermc.engine.paper.entry.placeholderParser
import com.typewritermc.engine.paper.entry.supplyPlayer
import com.typewritermc.engine.paper.plugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.FluidCollisionMode
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.util.Vector
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin

private const val WAYPOINT_SHADER_OPACITY = 252
private const val WAYPOINT_SEE_THROUGH = true
private const val WAYPOINT_SHADER_FIX = true
private const val WAYPOINT_SMART_SEE_THROUGH = true
private const val WAYPOINT_VISIBILITY_CHECK_INTERVAL = 4L
private const val WAYPOINT_SEE_THROUGH_DEBUG = false
private const val WAYPOINT_SEE_THROUGH_DEBUG_INTERVAL = 20L
private const val WAYPOINT_OCCLUSION_BLOCKED_STREAK = 2
private const val WAYPOINT_OCCLUSION_CLEAR_STREAK = 3
private const val WAYPOINT_OCCLUSION_BLOCKED_COVERAGE = 0.45
private const val WAYPOINT_OCCLUSION_WEAK_COVERAGE = 0.35
private const val WAYPOINT_OCCLUSION_CLEAR_COVERAGE = 0.10
private const val ACQUISITION_TOTAL_TICKS = 11L
private const val ACQUISITION_START_SCALE = 1.90f
private const val ACQUISITION_FIRST_LOCK_SCALE = 1.55f
private const val ACQUISITION_SECOND_LOCK_SCALE = 1.25f
private const val BEAM_ROTATION_DEGREES_PER_TICK = 2.25
private const val ENTITY_GLOW_CHECK_INTERVAL = 5L
private const val AUTOMATIC_LANE_SPACING = 0.35

data class WynncraftGeneralConfig(
    @Help("Hide label, beam, and HUD on arrival. Symbol stays visible.")
    val hideOnArrive: Boolean = true,
    @Help("Target sorting mode.")
    val selection: WaypointTargetSelection = WaypointTargetSelection.HIGHEST_PRIORITY,
    @Help("Maximum visible targets.")
    val maxTargets: Int = 5,
    @Help("Arrival distance.")
    val arriveRadius: Double = 1.5,
)

data class WynncraftTargetConfig(
    @Help("Target height offset.")
    val offset: Double = 0.0,
)

data class WynncraftLabelConfig(
    @Help("Show a shadow behind the label text.")
    val shadow: Boolean = true,
    @Help("Label text.")
    @Colored @Placeholder
    val text: Var<String> = ConstVar("<white>{name}</white>\n<gold>{distance}</gold>"),
    @Help("Height above target.")
    val height: Double = 1.0,
    @Help("Maximum distance from the camera to the label and symbol.")
    val floatDist: Double = 5.0,
    @Help("Hide within distance.")
    val hideRange: Double = 8.0,
    @Help("Minimum text scale.")
    val minScale: Float = 1.0f,
    @Help("Maximum text scale.")
    val maxScale: Float = 8.0f,
    @Help("Scale start distance.")
    val nearDist: Double = 5.0,
    @Help("Scale end distance.")
    val farDist: Double = 50.0,
    @Help("Text opacity.")
    val opacity: Int = 255,
    @Help("Maximum line width.")
    val lineWidth: Int = 255,
)

data class WynncraftSymbolConfig(
    @Help("Show the symbol.")
    val enabled: Boolean = true,
    @Help("Show a shadow behind the symbol.")
    val shadow: Boolean = false,
    @Help("Symbol text.")
    @Colored @Placeholder
    val text: Var<String> = ConstVar("<gold>◆</gold>"),
    @Help("Minimum symbol scale.")
    val minScale: Float = 3.0f,
    @Help("Maximum symbol scale.")
    val maxScale: Float = 25.0f,
    @Help("Scale start distance.")
    val nearDist: Double = 5.0,
    @Help("Scale end distance.")
    val farDist: Double = 150.0,
    @Help("Vertical symbol offset.")
    val offset: Double = 0.5,
    @Help("Snap enter distance.")
    val snapRange: Double = 8.0,
    @Help("Snap leave distance.")
    val snapLeave: Double = 12.0,
    @Help("Snapped symbol height.")
    val snapHeight: Double = 3.0,
    @Help("Scale-based spacing.")
    val scaleSpacing: Double = 0.16,
)

data class WynncraftBeamConfig(
    @Help("Show a private vertical beam.")
    val enabled: Boolean = true,
    @Help("Render the beam at full brightness.")
    val fullBright: Boolean = true,
    @Help("Rotate the inner beam core while the outer layer remains fixed.")
    val rotateInner: Boolean = false,
    @Help("Outer beam material (solid blocks only).")
    val outer: Material = Material.LIME_STAINED_GLASS,
    @Help("Inner beam material (solid blocks only).")
    val inner: Material = Material.LIME_CONCRETE,
    @Help("Outer beam width in blocks.")
    val width: Float = 0.5f,
    @Help("Outer beam depth in blocks.")
    val depth: Float = 0.5f,
    @Help("Inner beam width in blocks.")
    val coreWidth: Float = 0.25f,
    @Help("Inner beam depth in blocks.")
    val coreDepth: Float = 0.25f,
    @Help("Beam height above the highest point between player and waypoint.")
    val height: Float = 150.0f,
    @Help("Extend the beam below the lowest point between player and waypoint.")
    val depthBelow: Float = 20.0f,
    @Help("Extra horizontal gap between the beam edge and label/symbol.")
    val labelClearance: Double = 0.12,
)

data class WynncraftBobConfig(
    @Help("Enable floating motion.")
    val enabled: Boolean = true,
    @Help("Floating motion height.")
    val height: Double = 0.06,
    @Help("Floating motion speed.")
    val speed: Double = 1.2,
)

@Entry(
    "static_waypoint",
    "Waypoint",
    Colors.GREEN,
    "mdi:navigation-variant"
)
class WynncraftWaypointEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("Display mode, targeting, and arrival settings.")
    val general: WynncraftGeneralConfig = WynncraftGeneralConfig(),

    @Help("Height offset and vertical detection.")
    val target: WynncraftTargetConfig = WynncraftTargetConfig(),

    @Help("Floating text label.")
    val label: WynncraftLabelConfig = WynncraftLabelConfig(),

    @Help("Scaling icon above the text.")
    val symbol: WynncraftSymbolConfig = WynncraftSymbolConfig(),

    @Help("Private vertical beam rendered behind the label and symbol.")
    val beam: WynncraftBeamConfig = WynncraftBeamConfig(),

    @Help("Float motion shared by text and icon.")
    val bob: WynncraftBobConfig = WynncraftBobConfig(),

) : AudienceEntry, PlaceholderEntry {
    override suspend fun display(): AudienceDisplay = WynncraftWaypointDisplay(this)

    override fun parser(): PlaceholderParser = placeholderParser {
        literal("direction") {
            literal("up") { supplyPlayer { _: Player -> DirectionGlyphs.get("up") } }
            literal("down") { supplyPlayer { _: Player -> DirectionGlyphs.get("down") } }
            literal("north") { supplyPlayer { _: Player -> DirectionGlyphs.get("north") } }
            literal("northeast") { supplyPlayer { _: Player -> DirectionGlyphs.get("northeast") } }
            literal("east") { supplyPlayer { _: Player -> DirectionGlyphs.get("east") } }
            literal("southeast") { supplyPlayer { _: Player -> DirectionGlyphs.get("southeast") } }
            literal("south") { supplyPlayer { _: Player -> DirectionGlyphs.get("south") } }
            literal("southwest") { supplyPlayer { _: Player -> DirectionGlyphs.get("southwest") } }
            literal("west") { supplyPlayer { _: Player -> DirectionGlyphs.get("west") } }
            literal("northwest") { supplyPlayer { _: Player -> DirectionGlyphs.get("northwest") } }
        }
    }
}

private class WynncraftPlayerState {
    val markers = LinkedHashMap<String, WynncraftMarkerPair>()
    val trackedTargetKeys = mutableSetOf<String>()
    var worldId: UUID? = null
    var tick: Long = 0
}

private data class WynncraftMarkerPair(
    val label: TextDisplay,
    val icon: TextDisplay,
    val beam: WynncraftBeamPair?,
    val styleId: String?,
    val acquisitionStartTick: Long? = null,
    var snapped: Boolean = false,
    val labelDepth: WaypointDepthState = WaypointDepthState(),
    val symbolDepth: WaypointDepthState = WaypointDepthState(),
    var lastVisibilityCheckTick: Long = Long.MIN_VALUE,
    var lastSeeThroughDebugTick: Long = Long.MIN_VALUE,
    var glowEntityId: Int = -1,
    var glowBaseFlags: Byte = 0,
)

private data class WaypointDepthState(
    var seeThrough: Boolean = false,
    var blockedStreak: Int = 0,
    var clearStreak: Int = 0,
)

private enum class WaypointOcclusionDecision {
    BLOCKED,
    AMBIGUOUS,
    CLEAR,
}

private enum class WaypointBlockClass {
    OPAQUE,
    TRANSLUCENT,
    PARTIAL,
}

private data class WaypointOcclusionSample(
    val name: String,
    val position: Location,
    val weight: Double,
    val center: Boolean = false,
)

private data class WaypointOcclusionHit(
    val sample: String,
    val samplePosition: Location,
    val blockDescription: String,
    val hitPosition: Location?,
    val remainingDistance: Double?,
    val blockClass: WaypointBlockClass,
    val strength: Double,
)

private data class WaypointDisplayOcclusion(
    val decision: WaypointOcclusionDecision,
    val coverage: Double,
    val strong: Boolean,
    val centerHit: Boolean,
    val testedSamples: Int,
    val hit: WaypointOcclusionHit? = null,
)

private data class WaypointOcclusionResult(
    val label: WaypointDisplayOcclusion?,
    val symbol: WaypointDisplayOcclusion?,
)

private data class WynncraftBeamPair(
    val outer: BlockDisplay,
    val inner: BlockDisplay,
)

private class WynncraftWaypointDisplay(
    private val entry: WynncraftWaypointEntry,
) : AudienceDisplay(), TickableDisplay {

    private val states = ConcurrentHashMap<UUID, WynncraftPlayerState>()
    private val miniMessage = MiniMessage.miniMessage()
    private val plainText = PlainTextComponentSerializer.plainText()

    override fun onPlayerAdd(player: Player) {
        states.computeIfAbsent(player.uniqueId) { WynncraftPlayerState() }
        runSync { if (isActive && player in this) updatePlayer(player, force = true) }
    }

    override fun onPlayerRemove(player: Player) {
        val state = states.remove(player.uniqueId) ?: return
        runSync { clearPlayer(player, state) }
    }

    override fun tick() {
        runSync {
            if (!isActive) return@runSync
            players.forEach { updatePlayer(it, force = false) }
        }
    }

    override fun dispose() {
        val snapshot = states.entries.toList()
        states.clear()
        runSync {
            snapshot.forEach { (uuid, state) ->
                Bukkit.getPlayer(uuid)?.let { clearPlayer(it, state) }
                    ?: run {
                        state.markers.values.forEach(::removeMarker)
                        WaypointGlowController.clearPlayer(uuid)
                    }
            }
        }
        super.dispose()
    }

    private fun updatePlayer(player: Player, force: Boolean) {
        val state = states.computeIfAbsent(player.uniqueId) { WynncraftPlayerState() }
        if (state.worldId != null && state.worldId != player.world.uid) {
            state.trackedTargetKeys.clear()
        }
        state.worldId = player.world.uid
        state.tick++
        val targets = resolveTargets(player, state)
        syncMarkers(player, state, targets, force)
    }

    private fun resolveTargets(player: Player, state: WynncraftPlayerState): List<WaypointTarget> {
        return WaypointTargetRegistry.resolve(
            player = player,
            selection = entry.general.selection,
            maxTargets = entry.general.maxTargets.coerceIn(0, 16),
            includeObjectives = true,
            includeEntities = true,
        )
    }

    private fun syncMarkers(
        player: Player,
        state: WynncraftPlayerState,
        targets: List<WaypointTarget>,
        force: Boolean,
    ) {
        val visible = targets.filter { target ->
            target.location.world == player.world
        }
        val keys = visible.mapTo(HashSet<String>()) { it.zoneKey() }
        val newlyTrackedKeys = visible.asSequence()
            .map { it.zoneKey() }
            .filter { it !in state.trackedTargetKeys }
            .toSet()
        val newlyTracked = visible.firstOrNull { it.zoneKey() in newlyTrackedKeys }
        state.trackedTargetKeys.retainAll(keys)
        state.trackedTargetKeys.addAll(keys)
        newlyTracked?.let { faceTargetYaw(player, it.location) }
        val stale = state.markers.keys.filter { it !in keys }
        stale.forEach { key ->
            state.markers.remove(key)?.let { removeMarker(player, it) }
        }

        visible.forEachIndexed { index, target ->
            val key = target.zoneKey()
            val style = target.style
            val arrived = entry.general.arriveRadius > 0.0 &&
                target.distance <= entry.general.arriveRadius
            val labelVisible = target.distance > entry.label.hideRange.coerceAtLeast(0.0) &&
                !(entry.general.hideOnArrive && arrived)
            val symbolVisible = entry.symbol.enabled
            val beamVisible = entry.beam.enabled && !(entry.general.hideOnArrive && arrived)
            if (!labelVisible && !symbolVisible && !beamVisible) {
                state.markers.remove(key)?.let { removeMarker(player, it) }
                return@forEachIndexed
            }

            val bobY = if (entry.bob.enabled && entry.bob.height > 0.0) {
                kotlin.math.sin(state.tick * entry.bob.speed * Math.PI / 10.0) * entry.bob.height
            } else 0.0
            val marker = state.markers[key]
            val snapped = arrived || if (marker?.snapped == true) {
                target.distance <= entry.symbol.snapLeave.coerceAtLeast(entry.symbol.snapRange + 0.01)
            } else {
                target.distance <= entry.symbol.snapRange.coerceAtLeast(0.0)
            }
            val beamVisualAnchor = beamAnchor(player, target)
            val beamPointer = beamVisualAnchor.clone().subtract(0.0, entry.label.height, 0.0)
            // Keep the occlusion anchor stable. Bob is a visual-only Y offset and must
            // not make smart see-through alternate when the ray grazes a nearby block.
            val stableV3TargetLoc = labelAnchor(player, target)
            val stableTargetLoc = if (entry.beam.enabled) {
                offsetMarkerInFrontOfBeam(player, stableV3TargetLoc)
            } else {
                stableV3TargetLoc
            }.also {
                applyAutomaticLaneOffset(player, it, index, visible.size, 1.0f)
                // VERTICAL billboard uses camera yaw but retains the display entity's
                // own pitch. Route/objective positions may carry a non-zero pitch,
                // so normalize it to keep label and symbol perfectly upright.
                it.yaw = 0.0f
                it.pitch = 0.0f
            }
            val targetLoc = stableTargetLoc.clone().add(0.0, bobY, 0.0)
            val baseIconScale = if (snapped) entry.symbol.minScale.coerceAtLeast(0.01f)
                else symbolScale(target.distance)
            val iconScale = baseIconScale
            val baseIconOffset = if (snapped) entry.symbol.snapHeight - entry.label.height
                else entry.symbol.offset + baseIconScale * entry.symbol.scaleSpacing
            val iconOffset = baseIconOffset
            val textScale = labelScale(target.distance)
            val labelComponent = if (labelVisible)
                markerComponent(
                    if (style?.label?.overrideText == true) style.label.text.get(player)
                    else entry.label.text.get(player),
                    player, target, index, visible.size,
                )
            else Component.empty()
            val iconComponent = if (symbolVisible)
                markerComponent(
                    if (style?.symbol?.overrideText == true) style.symbol.text.get(player)
                    else entry.symbol.text.get(player),
                    player, target, index, visible.size,
                )
            else Component.empty()
            val refreshVisibility = force || marker == null ||
                state.tick - marker.lastVisibilityCheckTick >= WAYPOINT_VISIBILITY_CHECK_INTERVAL
            val occlusionResult = when {
                !WAYPOINT_SEE_THROUGH || !WAYPOINT_SMART_SEE_THROUGH -> null
                refreshVisibility -> inspectMarkerOcclusion(
                    player.eyeLocation,
                    stableTargetLoc,
                    labelComponent,
                    iconComponent,
                    iconOffset,
                    textScale,
                    iconScale,
                )
                else -> null
            }
            val labelDepth = marker?.labelDepth ?: WaypointDepthState()
            val symbolDepth = marker?.symbolDepth ?: WaypointDepthState()
            updateDepthState(labelDepth, occlusionResult?.label)
            updateDepthState(symbolDepth, occlusionResult?.symbol)
            val labelSeeThrough = configuredSeeThrough(labelDepth)
            val symbolSeeThrough = configuredSeeThrough(symbolDepth)
            val live = if (marker == null ||
                !marker.label.isValid || !marker.icon.isValid ||
                !beamStateIsValid(marker.beam) ||
                marker.styleId != style?.id ||
                marker.label.world != targetLoc.world
            ) {
                marker?.let { removeMarker(player, it) }
                spawnMarker(
                    player, targetLoc, beamPointer, textScale, iconScale, iconOffset,
                    beamVisible, labelSeeThrough, symbolSeeThrough,
                    style = style,
                    acquisition = symbolVisible && key in newlyTrackedKeys,
                    currentTick = state.tick,
                    labelDepth = labelDepth,
                    symbolDepth = symbolDepth,
                )
                    .also { state.markers[key] = it }
            } else marker
            live.snapped = snapped
            val seeThroughChanged = live.label.isSeeThrough != labelSeeThrough ||
                live.icon.isSeeThrough != symbolSeeThrough
            if (refreshVisibility) {
                live.lastVisibilityCheckTick = state.tick
            }
            applyWaypointTextDepth(live, labelSeeThrough, symbolSeeThrough)
            if (WAYPOINT_SEE_THROUGH_DEBUG && occlusionResult != null &&
                (marker == null || seeThroughChanged ||
                    state.tick - live.lastSeeThroughDebugTick >= WAYPOINT_SEE_THROUGH_DEBUG_INTERVAL)
            ) {
                logSeeThroughDebug(
                    player = player,
                    target = target,
                    stableAnchor = stableTargetLoc,
                    renderedAnchor = targetLoc,
                    iconOffset = iconOffset,
                    result = occlusionResult,
                    marker = live,
                    tick = state.tick,
                )
            }
            updateEntityGlow(player, live, target, state.tick, force || marker == null)

            if (beamVisible) {
                live.beam?.let { beam ->
                    updateBeam(player, beam, beamPointer, target.distance, state.tick, style)
                    if (!player.canSee(beam.outer)) player.showEntity(plugin, beam.outer)
                    if (!player.canSee(beam.inner)) player.showEntity(plugin, beam.inner)
                }
            } else {
                live.beam?.let { beam ->
                    if (player.canSee(beam.outer)) player.hideEntity(plugin, beam.outer)
                    if (player.canSee(beam.inner)) player.hideEntity(plugin, beam.inner)
                }
            }

            if (force || live.label.text() != labelComponent) live.label.text(labelComponent)
            if (force || live.icon.text() != iconComponent) live.icon.text(iconComponent)

            val acquisitionIconScale = acquisitionScaleForTick(
                live,
                state.tick,
                iconScale,
                symbolVisible,
            )

            val labelTransform = live.label.transformation
            if (kotlin.math.abs(labelTransform.scale.x - textScale) > 0.001f) {
                labelTransform.scale.set(textScale, textScale, textScale)
                live.label.interpolationDelay = 0
                live.label.interpolationDuration = V3_DISPLAY_INTERPOLATION_TICKS
                live.label.transformation = labelTransform
            }
            val iconTransform = live.icon.transformation
            if (kotlin.math.abs(iconTransform.scale.x - acquisitionIconScale) > 0.001f ||
                kotlin.math.abs(iconTransform.translation.y - iconOffset.toFloat()) > 0.001f
            ) {
                iconTransform.translation.set(0f, iconOffset.toFloat(), 0f)
                iconTransform.scale.set(acquisitionIconScale, acquisitionIconScale, acquisitionIconScale)
                live.icon.interpolationDelay = 0
                live.icon.interpolationDuration =
                    if (isAcquisitionActive(live, state.tick)) 1 else V3_DISPLAY_INTERPOLATION_TICKS
                live.icon.transformation = iconTransform
            }
            if (live.label.location.distanceSquared(targetLoc) > 0.0025) {
                live.label.teleportAsync(targetLoc)
            }
            if (!player.canSee(live.label)) player.showEntity(plugin, live.label)
            if (!player.canSee(live.icon)) player.showEntity(plugin, live.icon)
        }
    }

    private fun faceTargetYaw(player: Player, target: Location) {
        val dx = target.x - player.location.x
        val dz = target.z - player.location.z
        if (dx * dx + dz * dz < 0.0001) return
        val yaw = Math.toDegrees(kotlin.math.atan2(-dx, dz)).toFloat()
        runCatching { player.setRotation(yaw, player.location.pitch) }
    }

    private fun spawnMarker(
        player: Player,
        location: Location,
        beamPointer: Location,
        textScale: Float,
        iconScale: Float,
        iconOffset: Double,
        beamVisible: Boolean,
        labelSeeThrough: Boolean,
        symbolSeeThrough: Boolean,
        style: WaypointThemeSnapshot?,
        acquisition: Boolean,
        currentTick: Long,
        labelDepth: WaypointDepthState,
        symbolDepth: WaypointDepthState,
    ): WynncraftMarkerPair {
        // Spawn the beam first so the text entities are inserted later on the client.
        // The physical camera-facing offset remains the primary anti-intersection rule.
        val beam = if (entry.beam.enabled) spawnBeam(player, beamPointer, beamVisible, style) else null
        val billboard = Display.Billboard.VERTICAL
        val label = location.world!!.spawn(location, TextDisplay::class.java) {
            it.isPersistent = false
            it.isVisibleByDefault = false
            it.billboard = billboard
            it.isSeeThrough = labelSeeThrough
            it.brightness = Display.Brightness(15, 15)
            it.textOpacity = shaderOpacity()
            it.isDefaultBackground = false
            it.backgroundColor = Color.fromARGB(0, 0, 0, 0)
            it.isShadowed = if (style?.label?.overrideShadow == true) style.label.shadow else entry.label.shadow
            it.lineWidth = entry.label.lineWidth.coerceAtLeast(1)
            it.alignment = TextDisplay.TextAlignment.CENTER
            it.teleportDuration = V3_DISPLAY_INTERPOLATION_TICKS
            it.interpolationDuration = V3_DISPLAY_INTERPOLATION_TICKS
            it.transformation = it.transformation.apply {
                this.scale.set(textScale, textScale, textScale)
            }
        }
        val icon = location.world!!.spawn(location, TextDisplay::class.java) {
            it.isPersistent = false
            it.isVisibleByDefault = false
            it.billboard = billboard
            it.isSeeThrough = symbolSeeThrough
            it.brightness = Display.Brightness(15, 15)
            it.textOpacity = shaderOpacity()
            it.isDefaultBackground = false
            it.backgroundColor = Color.fromARGB(0, 0, 0, 0)
            it.isShadowed = if (style?.symbol?.overrideShadow == true) style.symbol.shadow else entry.symbol.shadow
            it.lineWidth = 1000
            it.alignment = TextDisplay.TextAlignment.CENTER
            configureSymbolTransform(it, iconScale, iconOffset, acquisition)
        }
        label.addPassenger(icon)
        player.showEntity(plugin, label)
        player.showEntity(plugin, icon)
        return WynncraftMarkerPair(
            label = label,
            icon = icon,
            beam = beam,
            styleId = style?.id,
            acquisitionStartTick = if (acquisition) currentTick else null,
            labelDepth = labelDepth,
            symbolDepth = symbolDepth,
        )
    }

    private fun applyWaypointTextDepth(
        marker: WynncraftMarkerPair,
        labelSeeThrough: Boolean,
        symbolSeeThrough: Boolean,
    ) {
        // The opacity marker is understood by both regular and see-through core
        // shaders. Smart see-through selects the latter only while blocks occlude
        // the marker, keeping the normal depth-writing pass when the view is clear.
        applyTextDepthMode(marker.label, labelSeeThrough)
        applyTextDepthMode(marker.icon, symbolSeeThrough)
    }

    private fun configureSymbolTransform(
        display: Display,
        iconScale: Float,
        iconOffset: Double,
        acquisition: Boolean,
    ) {
        display.interpolationDuration = V3_DISPLAY_INTERPOLATION_TICKS
        display.transformation = display.transformation.apply {
            val initialScale = if (acquisition) iconScale * ACQUISITION_START_SCALE else iconScale
            translation.set(0f, iconOffset.toFloat(), 0f)
            scale.set(initialScale, initialScale, initialScale)
        }
    }

    private fun acquisitionScaleForTick(
        marker: WynncraftMarkerPair,
        currentTick: Long,
        baseIconScale: Float,
        symbolVisible: Boolean,
    ): Float {
        val startTick = marker.acquisitionStartTick ?: return baseIconScale
        val age = (currentTick - startTick).coerceAtLeast(0L)
        if (!symbolVisible || age >= ACQUISITION_TOTAL_TICKS) return baseIconScale

        // A one-way focus lock: every stage continues inward from the previous
        // scale. The short holds make each acquisition step readable without
        // resetting the symbol outward and creating a bounce effect.
        val iconFactor = when (age) {
            in 0L..2L -> acquisitionScale(age, 0L, 2L, ACQUISITION_START_SCALE, ACQUISITION_FIRST_LOCK_SCALE)
            3L -> ACQUISITION_FIRST_LOCK_SCALE
            in 4L..6L -> acquisitionScale(age, 4L, 6L, ACQUISITION_FIRST_LOCK_SCALE, ACQUISITION_SECOND_LOCK_SCALE)
            7L -> ACQUISITION_SECOND_LOCK_SCALE
            else -> acquisitionScale(age, 8L, 10L, ACQUISITION_SECOND_LOCK_SCALE, 1.0f)
        }
        return baseIconScale * iconFactor
    }

    private fun acquisitionScale(
        tick: Long,
        startTick: Long,
        endTick: Long,
        startScale: Float,
        endScale: Float,
    ): Float {
        val progress = ((tick - startTick).toDouble() / (endTick - startTick).toDouble())
            .coerceIn(0.0, 1.0)
        val eased = 1.0 - (1.0 - progress) * (1.0 - progress) * (1.0 - progress)
        return startScale + (endScale - startScale) * eased.toFloat()
    }

    private fun isAcquisitionActive(marker: WynncraftMarkerPair, currentTick: Long): Boolean =
        marker.acquisitionStartTick?.let { start ->
            currentTick - start < ACQUISITION_TOTAL_TICKS
        } == true

    private fun updateEntityGlow(
        player: Player,
        marker: WynncraftMarkerPair,
        target: WaypointTarget,
        tick: Long,
        force: Boolean,
    ) {
        if (!force && tick % ENTITY_GLOW_CHECK_INTERVAL != 0L) return
        val desiredId = target.packetEntityId?.takeIf {
            target.glowEnabled && target.distance <= target.glowRange.coerceAtLeast(0.0)
        } ?: -1
        if (marker.glowEntityId == desiredId) return
        clearEntityGlow(player, marker)
        if (desiredId != -1) {
            val entity = target.entityUUID?.let(::findEntityByUuid)
            marker.glowEntityId = desiredId
            marker.glowBaseFlags = entity?.let(::sharedEntityFlags) ?: 0
            WaypointGlowController.activate(player, desiredId, marker.glowBaseFlags)
        }
    }

    private fun clearEntityGlow(player: Player, marker: WynncraftMarkerPair) {
        if (marker.glowEntityId == -1) return
        WaypointGlowController.deactivate(player, marker.glowEntityId)
        marker.glowEntityId = -1
        marker.glowBaseFlags = 0
    }

    private fun sharedEntityFlags(entity: Entity): Byte {
        var flags = if (entity.fireTicks > 0) 0x01 else 0
        if (entity.isGlowing) flags = flags or 0x40
        if (entity is LivingEntity) {
            if (entity.isSneaking) flags = flags or 0x02
            if (entity.isSwimming) flags = flags or 0x10
            if (entity.isInvisible) flags = flags or 0x20
            if (entity.isGliding) flags = flags or 0x80
        }
        return flags.toByte()
    }

    private fun applyTextDepthMode(display: TextDisplay, seeThrough: Boolean) {
        if (display.isSeeThrough != seeThrough) display.isSeeThrough = seeThrough
        val opacity = shaderOpacity()
        if (display.textOpacity != opacity) display.textOpacity = opacity
    }

    private fun traceOcclusion(
        from: Location,
        to: Location,
        sample: String,
    ): WaypointOcclusionHit? {
        if (from.world == null || from.world != to.world) return null
        val delta = to.toVector().subtract(from.toVector())
        val distance = delta.length()
        if (distance <= 0.2) return null
        val traceDistance = (distance - 0.15).coerceAtLeast(0.01)
        val direction = delta.multiply(1.0 / distance)
        var rayOrigin = from.clone()
        var remaining = traceDistance
        var skippedGlass = 0
        while (remaining > 0.01 && skippedGlass < 16) {
            val trace = from.world!!.rayTraceBlocks(
                rayOrigin,
                direction,
                remaining,
                FluidCollisionMode.NEVER,
                true,
            ) ?: return null
            val block = trace.hitBlock ?: return null
            if (block.isPassable) return null
            val hitVector = trace.hitPosition ?: return null
            if (isTransparentGlass(block.type)) {
                val advance = distancePastBlock(hitVector, direction, block.boundingBox)
                val nextOrigin = Location(
                    from.world,
                    hitVector.x + direction.x * advance,
                    hitVector.y + direction.y * advance,
                    hitVector.z + direction.z * advance,
                )
                remaining -= rayOrigin.distance(nextOrigin)
                rayOrigin = nextOrigin
                skippedGlass++
                continue
            }
            val hitPosition = Location(from.world, hitVector.x, hitVector.y, hitVector.z)
            val collisionVolume = runCatching {
                block.collisionShape.boundingBoxes.sumOf { box ->
                    (box.maxX - box.minX).coerceAtLeast(0.0) *
                        (box.maxY - box.minY).coerceAtLeast(0.0) *
                        (box.maxZ - box.minZ).coerceAtLeast(0.0)
                }.coerceIn(0.0, 1.0)
            }.getOrDefault(1.0)
            val (blockClass, strength) = when {
                block.type.isOccluding -> WaypointBlockClass.OPAQUE to 1.0
                collisionVolume >= 0.75 -> WaypointBlockClass.TRANSLUCENT to 0.55
                else -> WaypointBlockClass.PARTIAL to collisionVolume.coerceIn(0.20, 0.65)
            }
            return WaypointOcclusionHit(
                sample = sample,
                samplePosition = to.clone(),
                blockDescription = "${block.type.name}@${block.x},${block.y},${block.z}" +
                    if (skippedGlass > 0) "/glassSkipped=$skippedGlass" else "",
                hitPosition = hitPosition,
                remainingDistance = hitPosition.distance(to),
                blockClass = blockClass,
                strength = strength,
            )
        }
        return null
    }

    private fun isTransparentGlass(material: Material): Boolean {
        val name = material.name
        return material == Material.GLASS || material == Material.GLASS_PANE ||
            material == Material.TINTED_GLASS ||
            name.endsWith("_STAINED_GLASS") || name.endsWith("_STAINED_GLASS_PANE")
    }

    private fun distancePastBlock(
        hit: Vector,
        direction: Vector,
        bounds: org.bukkit.util.BoundingBox,
    ): Double {
        fun axisDistance(position: Double, component: Double, min: Double, max: Double): Double =
            when {
                component > 0.000001 -> (max - position) / component
                component < -0.000001 -> (min - position) / component
                else -> Double.POSITIVE_INFINITY
            }
        val exitDistance = minOf(
            axisDistance(hit.x, direction.x, bounds.minX, bounds.maxX),
            axisDistance(hit.y, direction.y, bounds.minY, bounds.maxY),
            axisDistance(hit.z, direction.z, bounds.minZ, bounds.maxZ),
        ).coerceAtLeast(0.0)
        return exitDistance + 0.03
    }

    private fun inspectMarkerOcclusion(
        from: Location,
        stableAnchor: Location,
        label: Component,
        icon: Component,
        iconOffset: Double,
        textScale: Float,
        iconScale: Float,
    ): WaypointOcclusionResult {
        val right = horizontalRightVector(from, stableAnchor)
        val labelProbe = if (label != Component.empty()) {
            val (halfWidth, halfHeight) = labelProbeSize(label, textScale)
            evaluateOcclusionSamples(
                from,
                listOf(
                    WaypointOcclusionSample("label:center", stableAnchor.clone(), 0.40, center = true),
                    WaypointOcclusionSample("label:left", stableAnchor.clone().add(right.clone().multiply(-halfWidth)), 0.20),
                    WaypointOcclusionSample("label:right", stableAnchor.clone().add(right.clone().multiply(halfWidth)), 0.20),
                    WaypointOcclusionSample("label:top", stableAnchor.clone().add(0.0, halfHeight, 0.0), 0.10),
                    WaypointOcclusionSample("label:bottom", stableAnchor.clone().add(0.0, -halfHeight, 0.0), 0.10),
                )
            )
        } else null
        val symbolProbe = if (icon != Component.empty()) {
            val center = stableAnchor.clone().add(0.0, iconOffset, 0.0)
            val halfWidth = (iconScale * 0.04).toDouble().coerceIn(0.18, 0.90)
            evaluateOcclusionSamples(
                from,
                listOf(
                    WaypointOcclusionSample("symbol:center", center, 0.60, center = true),
                    WaypointOcclusionSample("symbol:left", center.clone().add(right.clone().multiply(-halfWidth)), 0.20),
                    WaypointOcclusionSample("symbol:right", center.clone().add(right.clone().multiply(halfWidth)), 0.20),
                )
            )
        } else null
        return WaypointOcclusionResult(labelProbe, symbolProbe)
    }

    private fun horizontalRightVector(from: Location, target: Location): Vector {
        val dx = target.x - from.x
        val dz = target.z - from.z
        val length = kotlin.math.sqrt(dx * dx + dz * dz)
        return if (length < 0.0001) Vector(1.0, 0.0, 0.0)
        else Vector(-dz / length, 0.0, dx / length)
    }

    private fun labelProbeSize(component: Component, scale: Float): Pair<Double, Double> {
        val lines = plainText.serialize(component).split('\n')
        val maxCharacters = lines.maxOfOrNull { it.length }?.coerceAtLeast(1) ?: 1
        val safeScale = scale.coerceAtLeast(0.01f).toDouble()
        val halfWidth = (maxCharacters * safeScale * 0.01).coerceIn(0.18, 1.35)
        val halfHeight = (lines.size.coerceAtLeast(1) * safeScale * 0.035).coerceIn(0.12, 0.65)
        return halfWidth to halfHeight
    }

    private fun evaluateOcclusionSamples(
        from: Location,
        samples: List<WaypointOcclusionSample>,
    ): WaypointDisplayOcclusion {
        var coverage = 0.0
        var tested = 0
        var centerHit = false
        var representative: WaypointOcclusionHit? = null
        var representativeScore = -1.0
        samples.forEach { sample ->
            tested++
            val hit = traceOcclusion(from, sample.position, sample.name) ?: return@forEach
            val score = sample.weight * hit.strength
            coverage += score
            if (sample.center) centerHit = true
            if (score > representativeScore || sample.center) {
                representative = hit
                representativeScore = score
            }
            // A full opaque block over the visual center is conclusive. Remaining
            // edge rays cannot change the decision, so avoid unnecessary traces.
            if (sample.center && hit.blockClass == WaypointBlockClass.OPAQUE) {
                return WaypointDisplayOcclusion(
                    WaypointOcclusionDecision.BLOCKED,
                    coverage = 1.0,
                    strong = true,
                    centerHit = true,
                    testedSamples = tested,
                    hit = hit,
                )
            }
        }
        val decision = when {
            coverage >= WAYPOINT_OCCLUSION_BLOCKED_COVERAGE -> WaypointOcclusionDecision.BLOCKED
            centerHit || coverage >= WAYPOINT_OCCLUSION_CLEAR_COVERAGE -> WaypointOcclusionDecision.AMBIGUOUS
            else -> WaypointOcclusionDecision.CLEAR
        }
        return WaypointDisplayOcclusion(
            decision = decision,
            coverage = coverage.coerceIn(0.0, 1.0),
            strong = false,
            centerHit = centerHit,
            testedSamples = tested,
            hit = representative,
        )
    }

    private fun updateDepthState(
        state: WaypointDepthState,
        probe: WaypointDisplayOcclusion?,
    ) {
        if (probe == null) return
        if (probe.strong) {
            state.seeThrough = true
            state.blockedStreak = WAYPOINT_OCCLUSION_BLOCKED_STREAK
            state.clearStreak = 0
            return
        }
        val weaklyBlocked = probe.decision == WaypointOcclusionDecision.BLOCKED ||
            probe.centerHit || probe.coverage >= WAYPOINT_OCCLUSION_WEAK_COVERAGE
        when {
            weaklyBlocked -> {
                state.blockedStreak++
                state.clearStreak = 0
                if (state.blockedStreak >= WAYPOINT_OCCLUSION_BLOCKED_STREAK) {
                    state.seeThrough = true
                }
            }
            probe.decision == WaypointOcclusionDecision.CLEAR -> {
                state.clearStreak++
                state.blockedStreak = 0
                if (state.clearStreak >= WAYPOINT_OCCLUSION_CLEAR_STREAK) {
                    state.seeThrough = false
                }
            }
            else -> {
                // Ambiguous edge contact keeps the current state without flicker.
                state.blockedStreak = 0
                state.clearStreak = 0
            }
        }
    }

    private fun configuredSeeThrough(state: WaypointDepthState): Boolean = when {
        !WAYPOINT_SEE_THROUGH -> false
        !WAYPOINT_SMART_SEE_THROUGH -> true
        else -> state.seeThrough
    }

    private fun logSeeThroughDebug(
        player: Player,
        target: WaypointTarget,
        stableAnchor: Location,
        renderedAnchor: Location,
        iconOffset: Double,
        result: WaypointOcclusionResult,
        marker: WynncraftMarkerPair,
        tick: Long,
    ) {
        marker.lastSeeThroughDebugTick = tick
        val hit = result.label?.hit ?: result.symbol?.hit
        val hitPosition = hit?.hitPosition
        val labelOpacity = marker.label.textOpacity.toInt() and 0xff
        val symbolOpacity = marker.icon.textOpacity.toInt() and 0xff
        Bukkit.getLogger().info(
            "[Typewriter] [WaypointRPG][SeeThroughDebug] " +
                "player=${player.name} target=${target.zoneKey()} " +
                "label=${formatProbe(result.label, marker.labelDepth)} " +
                "symbol=${formatProbe(result.symbol, marker.symbolDepth)} " +
                "sample=${hit?.sample ?: "none"} " +
                "eye=${formatDebugLocation(player.eyeLocation)} " +
                "stable=${formatDebugLocation(stableAnchor)} " +
                "rendered=${formatDebugLocation(renderedAnchor)} " +
                "symbolPos=${formatDebugLocation(stableAnchor.clone().add(0.0, iconOffset, 0.0))} " +
                "hit=${hit?.blockDescription ?: "none"} " +
                "blockClass=${hit?.blockClass ?: "none"} " +
                "hitPos=${hitPosition?.let(::formatDebugLocation) ?: "none"} " +
                "remaining=${hit?.remainingDistance?.let { "%.3f".format(it) } ?: "n/a"} " +
                "labelFlag=${marker.label.isSeeThrough} symbolFlag=${marker.icon.isSeeThrough} " +
                "labelOpacity=$labelOpacity symbolOpacity=$symbolOpacity"
        )
    }

    private fun formatProbe(
        probe: WaypointDisplayOcclusion?,
        state: WaypointDepthState,
    ): String = if (probe == null) {
        "hidden/applied=${state.seeThrough}"
    } else {
        "${probe.decision}/coverage=${"%.2f".format(probe.coverage)}" +
            "/strong=${probe.strong}/center=${probe.centerHit}" +
            "/tested=${probe.testedSamples}/applied=${state.seeThrough}" +
            "/streak=${state.blockedStreak}:${state.clearStreak}"
    }

    private fun formatDebugLocation(location: Location): String =
        "(%.3f,%.3f,%.3f)".format(location.x, location.y, location.z)

    private fun shaderOpacity(): Byte {
        val configured = entry.label.opacity.coerceIn(0, 255)
        return if (WAYPOINT_SHADER_FIX && configured >= 250) {
            WAYPOINT_SHADER_OPACITY.toByte()
        } else {
            configured.toByte()
        }
    }

    private fun beamStateIsValid(beam: WynncraftBeamPair?): Boolean =
        if (entry.beam.enabled) beam != null && beam.outer.isValid && beam.inner.isValid else beam == null

    private fun spawnBeam(
        player: Player,
        pointer: Location,
        visible: Boolean,
        style: WaypointThemeSnapshot?,
    ): WynncraftBeamPair {
        val geometry = beamGeometry(player, pointer)
        val outer = spawnBeamLayer(
            geometry.first,
            safeBeamMaterial(effectiveBeamOuter(style), Material.LIME_STAINED_GLASS),
            entry.beam.width,
            entry.beam.depth,
            geometry.second,
            0.0,
            effectiveBeamFullBright(style),
        )
        val inner = spawnBeamLayer(
            geometry.first,
            safeBeamMaterial(effectiveBeamInner(style), Material.LIME_CONCRETE),
            entry.beam.coreWidth,
            entry.beam.coreDepth,
            geometry.second,
            0.0,
            effectiveBeamFullBright(style),
        )
        if (visible) {
            player.showEntity(plugin, outer)
            player.showEntity(plugin, inner)
        }
        return WynncraftBeamPair(outer, inner)
    }

    private fun spawnBeamLayer(
        location: Location,
        material: Material,
        rawWidth: Float,
        rawDepth: Float,
        height: Float,
        angleRadians: Double,
        fullBright: Boolean,
    ): BlockDisplay {
        val width = rawWidth.coerceAtLeast(0.01f)
        val depth = rawDepth.coerceAtLeast(0.01f)
        return location.world!!.spawn(location, BlockDisplay::class.java) {
            it.isPersistent = false
            it.isVisibleByDefault = false
            it.block = material.createBlockData()
            it.brightness = if (fullBright) Display.Brightness(15, 15) else null
            it.teleportDuration = DISPLAY_INTERPOLATION_TICKS
            it.interpolationDuration = DISPLAY_INTERPOLATION_TICKS
            it.displayWidth = maxOf(width, depth) * 2.0f
            it.displayHeight = height
            it.transformation = it.transformation.apply {
                applyCenteredBeamRotation(this, width, depth, angleRadians)
                scale.set(width, height, depth)
            }
        }
    }

    private fun updateBeam(
        player: Player,
        beam: WynncraftBeamPair,
        pointer: Location,
        distance: Double,
        tick: Long,
        style: WaypointThemeSnapshot?,
    ) {
        val (location, height) = beamGeometry(player, pointer)
        val fade = beamFadeFactor(distance)
        updateBeamLayer(
            beam.outer, location,
            entry.beam.width * fade, entry.beam.depth * fade,
            height, 0.0, false,
        )
        updateBeamLayer(
            beam.inner, location,
            entry.beam.coreWidth * fade, entry.beam.coreDepth * fade,
            height,
            if (effectiveBeamRotation(style)) Math.toRadians(tick * BEAM_ROTATION_DEGREES_PER_TICK) else 0.0,
            effectiveBeamRotation(style),
        )
    }

    private fun effectiveBeamOuter(style: WaypointThemeSnapshot?): Material =
        if (style?.beam?.overrideMaterials == true) style.beam.outer else entry.beam.outer

    private fun effectiveBeamInner(style: WaypointThemeSnapshot?): Material =
        if (style?.beam?.overrideMaterials == true) style.beam.inner else entry.beam.inner

    private fun effectiveBeamRotation(style: WaypointThemeSnapshot?): Boolean =
        if (style?.beam?.overrideRotation == true) style.beam.rotateInner else entry.beam.rotateInner

    private fun effectiveBeamFullBright(style: WaypointThemeSnapshot?): Boolean =
        if (style?.beam?.overrideFullBright == true) style.beam.fullBright else entry.beam.fullBright

    private fun updateBeamLayer(
        display: BlockDisplay,
        location: Location,
        rawWidth: Float,
        rawDepth: Float,
        height: Float,
        angleRadians: Double,
        forceTransform: Boolean,
    ) {
        val width = rawWidth.coerceAtLeast(0.01f)
        val depth = rawDepth.coerceAtLeast(0.01f)
        if (display.world != location.world || display.location.distanceSquared(location) > 0.0025) {
            display.teleport(location)
        }
        val transform = display.transformation
        if (kotlin.math.abs(transform.scale.x - width) > 0.001f ||
            kotlin.math.abs(transform.scale.y - height) > 0.001f ||
            kotlin.math.abs(transform.scale.z - depth) > 0.001f ||
            forceTransform
        ) {
            applyCenteredBeamRotation(transform, width, depth, angleRadians)
            transform.scale.set(width, height, depth)
            display.interpolationDelay = 0
            display.interpolationDuration = DISPLAY_INTERPOLATION_TICKS
            display.transformation = transform
            display.displayWidth = maxOf(width, depth) * 2.0f
            display.displayHeight = height
        }
    }

    private fun applyCenteredBeamRotation(
        transform: org.bukkit.util.Transformation,
        width: Float,
        depth: Float,
        angleRadians: Double,
    ) {
        val c = cos(angleRadians).toFloat()
        val s = sin(angleRadians).toFloat()
        val halfWidth = width / 2.0f
        val halfDepth = depth / 2.0f
        transform.translation.set(
            -(c * halfWidth + s * halfDepth),
            0.0f,
            s * halfWidth - c * halfDepth,
        )
        transform.leftRotation.rotationY(angleRadians.toFloat())
    }

    private fun beamFadeFactor(distance: Double): Float {
        val end = entry.general.arriveRadius.coerceAtLeast(0.1)
        val start = maxOf(10.0, end + 0.1)
        return smoothStep(end, start, distance).toFloat().coerceIn(0.05f, 1.0f)
    }

    private fun beamGeometry(player: Player, pointer: Location): Pair<Location, Float> {
        val below = entry.beam.depthBelow.toDouble().coerceAtLeast(0.0)
        val above = entry.beam.height.toDouble().coerceAtLeast(1.0)
        val baseY = minOf(pointer.y, player.location.y) - below
        val topY = maxOf(pointer.y, player.location.y) + above
        return Location(pointer.world, pointer.x, baseY, pointer.z) to
            (topY - baseY).toFloat().coerceIn(1.0f, 500.0f)
    }

    private fun safeBeamMaterial(material: Material, fallback: Material): Material {
        if (material.isBlock && !material.isAir) return material
        Bukkit.getLogger().warning(
            "[WaypointRPG] static_waypoint beam material '${material.name}' is invalid; using ${fallback.name}."
        )
        return fallback
    }

    private fun offsetMarkerInFrontOfBeam(player: Player, marker: Location): Location {
        val clearance = beamClearanceRadius()
        var dx = player.eyeLocation.x - marker.x
        var dz = player.eyeLocation.z - marker.z
        var length = kotlin.math.sqrt(dx * dx + dz * dz)
        if (length < 0.001) {
            val direction = player.eyeLocation.direction
            dx = -direction.x
            dz = -direction.z
            length = kotlin.math.sqrt(dx * dx + dz * dz).coerceAtLeast(0.001)
        }
        return marker.clone().add(dx / length * clearance, 0.0, dz / length * clearance)
    }

    private fun applyAutomaticLaneOffset(
        player: Player,
        marker: Location,
        index: Int,
        total: Int,
        scaleFactor: Float,
    ) {
        if (total <= 1) return
        val lane = (index - (total - 1) / 2.0) * AUTOMATIC_LANE_SPACING * scaleFactor
        var dx = marker.x - player.eyeLocation.x
        var dz = marker.z - player.eyeLocation.z
        val length = kotlin.math.sqrt(dx * dx + dz * dz)
        if (length <= 0.001) return
        dx /= length
        dz /= length
        marker.add(-dz * lane, 0.0, dx * lane)
    }

    private fun beamClearanceRadius(): Double {
        val halfWidth = maxOf(entry.beam.width, entry.beam.coreWidth).toDouble().coerceAtLeast(0.01) / 2.0
        val halfDepth = maxOf(entry.beam.depth, entry.beam.coreDepth).toDouble().coerceAtLeast(0.01) / 2.0
        return kotlin.math.sqrt(halfWidth * halfWidth + halfDepth * halfDepth) +
            entry.beam.labelClearance.coerceAtLeast(0.0)
    }

    private fun markerComponent(
        template: String,
        player: Player,
        target: WaypointTarget,
        index: Int,
        total: Int,
    ): Component {
        val raw = template
            .replace("{name}", target.markerNameV3(player))
            .replace("{distance}", formatDistanceV3(target.distance))
            .replace("{direction}", directionGlyph(player.location, target.location))
            .replace("{index}", (index + 1).toString())
            .replace("{total}", total.toString())
        return runCatching { miniMessage.deserialize(raw) }
            .getOrElse { Component.text(raw) }
    }

    private fun labelScale(distance: Double): Float {
        val near = entry.label.nearDist.coerceAtLeast(0.0)
        val far = entry.label.farDist.coerceAtLeast(near + 0.01)
        val t = ((distance - near) / (far - near)).coerceIn(0.0, 1.0)
        val smooth = t * t * (3.0 - 2.0 * t)
        return (entry.label.minScale +
            (entry.label.maxScale - entry.label.minScale) * smooth.toFloat())
            .coerceAtLeast(0.01f)
    }

    private fun symbolScale(distance: Double): Float {
        val near = entry.symbol.nearDist.coerceAtLeast(0.0)
        val far = entry.symbol.farDist.coerceAtLeast(near + 0.01)
        val t = ((distance - near) / (far - near)).coerceIn(0.0, 1.0)
        val smooth = t * t * (3.0 - 2.0 * t)
        return (entry.symbol.minScale +
            (entry.symbol.maxScale - entry.symbol.minScale) * smooth.toFloat())
            .coerceAtLeast(0.01f)
    }

    /** Known-good V3 label anchor, kept separate from all beam calculations. */
    private fun labelAnchor(player: Player, target: WaypointTarget): Location {
        val fixedLocation = target.location.clone()
            .add(0.0, entry.target.offset + entry.label.height, 0.0)
        if (target.distance <= MARKER_CULL_TRANSITION_START) return fixedLocation

        val eyes = player.eyeLocation
        val dx = fixedLocation.x - eyes.x
        val dy = fixedLocation.y - eyes.y
        val dz = fixedLocation.z - eyes.z
        val length = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.001)
        val cameraLocation = Location(
            player.world,
            eyes.x + dx / length * MARKER_CAMERA_DISTANCE,
            eyes.y + dy / length * MARKER_CAMERA_DISTANCE,
            eyes.z + dz / length * MARKER_CAMERA_DISTANCE,
        )
        val transition = smoothStep(
            MARKER_CULL_TRANSITION_START,
            MARKER_CULL_TRANSITION_END,
            target.distance,
        )
        return Location(
            player.world,
            fixedLocation.x + (cameraLocation.x - fixedLocation.x) * transition,
            fixedLocation.y + (cameraLocation.y - fixedLocation.y) * transition,
            fixedLocation.z + (cameraLocation.z - fixedLocation.z) * transition,
        )
    }

    /** Beam-only anchor; changes here cannot alter the label/symbol anchor. */
    private fun beamAnchor(player: Player, target: WaypointTarget): Location {
        val fixedLocation = target.location.clone()
            .add(0.0, entry.target.offset + entry.label.height, 0.0)
        if (target.distance <= MARKER_CULL_TRANSITION_START) return fixedLocation

        val eyes = player.eyeLocation
        val dx = fixedLocation.x - eyes.x
        val dy = fixedLocation.y - eyes.y
        val dz = fixedLocation.z - eyes.z
        val length = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.001)
        val cameraLocation = Location(
            player.world,
            eyes.x + dx / length * MARKER_CAMERA_DISTANCE,
            eyes.y + dy / length * MARKER_CAMERA_DISTANCE,
            eyes.z + dz / length * MARKER_CAMERA_DISTANCE,
        )
        val transition = smoothStep(
            MARKER_CULL_TRANSITION_START,
            MARKER_CULL_TRANSITION_END,
            target.distance,
        )
        return Location(
            player.world,
            fixedLocation.x + (cameraLocation.x - fixedLocation.x) * transition,
            fixedLocation.y + (cameraLocation.y - fixedLocation.y) * transition,
            fixedLocation.z + (cameraLocation.z - fixedLocation.z) * transition,
        )
    }

    private fun smoothStep(edge0: Double, edge1: Double, value: Double): Double {
        if (edge1 <= edge0) return if (value >= edge1) 1.0 else 0.0
        val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }

    private companion object {
        const val MARKER_CULL_TRANSITION_START = 70.0
        const val MARKER_CULL_TRANSITION_END = 72.0
        const val MARKER_CAMERA_DISTANCE = 70.0
        const val DISPLAY_INTERPOLATION_TICKS = 1
        const val V3_DISPLAY_INTERPOLATION_TICKS = 2
    }

    private fun clearPlayer(player: Player, state: WynncraftPlayerState) {
        state.trackedTargetKeys.clear()
        state.markers.values.forEach { removeMarker(player, it) }
        state.markers.clear()
    }

    private fun removeMarker(player: Player, marker: WynncraftMarkerPair) {
        clearEntityGlow(player, marker)
        removeMarker(marker)
    }

    private fun removeMarker(marker: WynncraftMarkerPair) {
        runCatching { marker.label.removePassenger(marker.icon) }
        runCatching { marker.icon.remove() }
        runCatching { marker.label.remove() }
        marker.beam?.let { beam ->
            runCatching { beam.inner.remove() }
            runCatching { beam.outer.remove() }
        }
    }

    private fun runSync(block: () -> Unit) {
        if (Bukkit.isPrimaryThread()) block()
        else Bukkit.getScheduler().runTask(plugin, Runnable(block))
    }

    private fun WaypointTarget.markerNameV3(player: Player): String {
        if (!customName.isNullOrBlank()) return customName
        if (!routePointName.isNullOrBlank()) return routePointName
        if (routePointIndex != null) return "Waypoint ${routePointIndex + 1}/$routePointCount"
        val objectiveName = runCatching { objective?.display(player) }.getOrNull()
        if (!objectiveName.isNullOrBlank()) return objectiveName
        return entry.name.ifBlank { "Objective" }
    }
}

private fun formatDistanceV3(distance: Double): String =
    if (distance >= 1000.0) "%.1f km".format(distance / 1000.0) else "${distance.toInt()} m"

private fun directionGlyph(from: Location, to: Location): String {
    val dy = to.y - from.y
    if (kotlin.math.abs(dy) > 10.0) {
        return DirectionGlyphs.get(if (dy > 0.0) "up" else "down")
    }
    val angle = (Math.toDegrees(kotlin.math.atan2(to.z - from.z, to.x - from.x)) + 360.0) % 360.0
    val key = when (((angle + 22.5) / 45.0).toInt() % 8) {
        0 -> "east"
        1 -> "southeast"
        2 -> "south"
        3 -> "southwest"
        4 -> "west"
        5 -> "northwest"
        6 -> "north"
        else -> "northeast"
    }
    return DirectionGlyphs.get(key)
}
