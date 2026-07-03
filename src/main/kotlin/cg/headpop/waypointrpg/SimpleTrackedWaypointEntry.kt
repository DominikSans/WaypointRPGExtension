package cg.headpop.waypointrpg

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Colored
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Placeholder
import com.typewritermc.engine.paper.entry.entries.AudienceDisplay
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.Var
import org.bukkit.Material
import java.time.Duration

@Entry(
    "simple_tracked_waypoint",
    "DEPRECATED — use tracked_locatable_waypoint (V2) instead. Simple preset wrapper kept for backwards compatibility.",
    Colors.YELLOW,
    "mdi:map-marker-check"
)
class SimpleTrackedWaypointEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("DEPRECATED ENTRY — migrate to 'tracked_locatable_waypoint' (V2). This wrapper will be removed in a future release. The V2 entry exposes all settings directly with panel grouping.")
    val _deprecated_notice: String = "",

    @Help("Beam update interval. 250ms = 4 updates/sec. The label tracks every tick regardless.")
    val refreshDuration: Duration = Duration.ofMillis(250),

    @Help("Distance (blocks) at which the label hides and icon snaps to the exact waypoint position.")
    val indicatorDistance: Double = 8.0,

    @Help("Height the label and icon float above the waypoint base position.")
    val heightOffset: Double = 1.0,

    @Help("Icon shown above the waypoint. MiniMessage and custom font glyphs supported.")
    @Colored @Placeholder
    val indicatorIcon: Var<String> = ConstVar("<gold>◆</gold>"),

    @Help("Label text. Placeholders: {name}, {distance}, {direction}.")
    @Colored @Placeholder
    val indicatorText: Var<String> = ConstVar("<white>{name}</white>\n<gold>{distance}</gold>"),

    @Help("Label text scale.")
    val textScale: Double = 1.0,

    @Help("Icon scale when near the waypoint (snapped state).")
    val iconMinScale: Double = 3.0,

    @Help("Icon scale when far from the waypoint.")
    val iconMaxScale: Double = 5.0,

    @Help("Show a vertical beam at the waypoint.")
    val showBeam: Boolean = true,

    @Help("Primary (outer) beam material.")
    val beamMaterial: Material = Material.LIME_STAINED_GLASS,

    @Help("Beam width in blocks.")
    val beamWidth: Double = 0.5,

    @Help("Enable a translucent inner overlay layer on the beam.")
    val enableOverlay: Boolean = true,

    @Help("Inner overlay material (shown inside the primary beam layer).")
    val overlayMaterial: Material = Material.LIME_CONCRETE,

    @Help("Enable the floating bob animation on the label and icon.")
    val enableBob: Boolean = true,

    @Help("Vertical oscillation amplitude in blocks.")
    val bobHeight: Double = 0.06,

    @Help("Bob oscillation speed in cycles per second.")
    val bobSpeed: Double = 1.2,
) : AudienceEntry {

    override suspend fun display(): AudienceDisplay {
        val tickRate = (refreshDuration.toMillis() / 50L).toInt().coerceIn(1, 200)
        val bw = beamWidth.toFloat().coerceAtLeast(0.05f)

        val synthetic = TrackedLocatableWaypointEntry(
            id = id,
            name = name,
            general = WaypointGeneralConfig(
                mode = if (showBeam) WaypointType.BOTH else WaypointType.HOLOGRAM,
                selection = WaypointTargetSelection.HIGHEST_PRIORITY,
                maxTargets = 1,
                tickRate = tickRate,
                arriveRadius = 3.0,
                hideOnArrive = true,
            ),
            target = WaypointTargetConfig(
                offset = 0.0,
                verticalThreshold = 10.0,
            ),
            label = WaypointLabelConfig(
                text = indicatorText,
                useObjectiveName = true,
                height = heightOffset,
                floatDist = 5.0,
                hideRange = indicatorDistance,
                fov = 75.0,
                scale = textScale.toFloat(),
                billboard = "CENTER",
                align = "CENTER",
                background = true,
                bgColor = "#80000000",
                opacity = 255,
                shadow = true,
                seeThrough = false,
                lineWidth = 255,
            ),
            symbol = WaypointSymbolConfig(
                enabled = true,
                text = indicatorIcon,
                minScale = iconMinScale.toFloat().coerceAtLeast(0.1f),
                maxScale = iconMaxScale.toFloat().coerceAtLeast(0.1f),
                nearDist = 5.0,
                farDist = 150.0,
                offset = 0.5,
                snapRange = indicatorDistance,
                snapLeave = indicatorDistance + 4.0,
                snapHeight = 3.0,
                snapPosition = SymbolSnapPosition.CENTER_ON_WAYPOINT,
            ),
            beam = WaypointBeamConfig(
                outer = beamMaterial,
                inner = if (enableOverlay) overlayMaterial else beamMaterial,
                width = bw,
                coreWidth = bw * 0.5f,
                depth = bw,
                coreDepth = bw * 0.5f,
                height = 150.0f,
                dynamicHeight = true,
                staticRange = 30.0,
                followRange = 60.0,
                followDist = 55.0,
                fadeStart = 10.0,
                fadeEnd = 3.0,
            ),
            bob = WaypointBobConfig(
                enabled = enableBob,
                height = bobHeight,
                speed = bobSpeed,
            ),
            trail = WaypointTrailConfig(
                enabled = false,
            ),
            routes = emptyList(),
            integrations = WaypointIntegrationConfig(
                entityTargets = emptyList(),
                entityGlow = false,
                glowRange = 20.0,
            ),
            performance = WaypointPerformanceConfig(
                lazyUpdate = false,
                cleanupOnJoin = false,
                cleanupRadius = 50.0,
            ),
        )
        return synthetic.display()
    }
}
