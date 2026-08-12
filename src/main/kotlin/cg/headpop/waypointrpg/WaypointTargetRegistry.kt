package cg.headpop.waypointrpg

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Colored
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Placeholder
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.ManifestEntry
import com.typewritermc.engine.paper.entry.entries.AudienceDisplay
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.entry.entity.SimpleEntityInstance
import com.typewritermc.quest.entries.interfaces.LocatableObjective
import com.typewritermc.quest.entries.QuestEntry
import com.typewritermc.core.entries.Query
import org.bukkit.entity.Player
import org.bukkit.Material
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class WaypointStyleBeamConfig(
    @Help("Override the static_waypoint beam materials for this target.")
    val overrideMaterials: Boolean = true,
    @Help("Override inner beam rotation for this target.")
    val overrideRotation: Boolean = false,
    @Help("Override full-bright rendering for this target.")
    val overrideFullBright: Boolean = false,
    @Help("Outer beam material.")
    val outer: Material = Material.PURPLE_STAINED_GLASS,
    @Help("Inner beam material.")
    val inner: Material = Material.PURPLE_CONCRETE_POWDER,
    @Help("Rotate the inner beam core.")
    val rotateInner: Boolean = false,
    @Help("Render the beam at full brightness.")
    val fullBright: Boolean = true,
)

data class WaypointStyleLabelConfig(
    @Help("Replace the static_waypoint label template for this target.")
    val overrideText: Boolean = false,
    @Help("Override label shadow for this target.")
    val overrideShadow: Boolean = false,
    @Help("Per-target label template. Supports waypoint placeholders.")
    @Colored @Placeholder
    val text: Var<String> = ConstVar("<white>{name}</white><newline><gold>{distance}</gold>"),
    @Help("Draw a shadow behind this label.")
    val shadow: Boolean = true,
)

data class WaypointStyleSymbolConfig(
    @Help("Replace the static_waypoint symbol for this target.")
    val overrideText: Boolean = false,
    @Help("Override symbol shadow for this target.")
    val overrideShadow: Boolean = false,
    @Help("Per-target symbol text or resource-pack glyph.")
    @Colored @Placeholder
    val text: Var<String> = ConstVar("<light_purple>◆</light_purple>"),
    @Help("Draw a shadow behind this symbol.")
    val shadow: Boolean = false,
)

@Entry(
    "waypoint_theme",
    "Advanced reusable waypoint appearance.",
    Colors.PURPLE,
    "mdi:palette"
)
@Tags("waypoint_theme")
class WaypointThemeEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("Beam appearance overrides.")
    val beam: WaypointStyleBeamConfig = WaypointStyleBeamConfig(),

    @Help("Label appearance overrides.")
    val label: WaypointStyleLabelConfig = WaypointStyleLabelConfig(),

    @Help("Symbol appearance overrides.")
    val symbol: WaypointStyleSymbolConfig = WaypointStyleSymbolConfig(),
) : ManifestEntry

data class WaypointThemeSnapshot(
    val id: String,
    val beam: WaypointStyleBeamConfig,
    val label: WaypointStyleLabelConfig,
    val symbol: WaypointStyleSymbolConfig,
)

private fun Ref<WaypointThemeEntry>.snapshotOrNull(): WaypointThemeSnapshot? {
    if (!isSet) return null
    val selected = runCatching { get() }.getOrNull() ?: return null
    return WaypointThemeSnapshot(selected.id, selected.beam, selected.label, selected.symbol)
}

enum class WaypointPreset {
    INHERIT,
    PURPLE,
    GREEN,
    RED,
    GOLD,
    BLUE,
    CUSTOM,
}

@Entry(
    "quest_waypoint",
    "Choose the waypoint appearance for a quest.",
    Colors.GREEN,
    "mdi:book-marker"
)
class QuestWaypointEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("Quest whose location objectives inherit this appearance.")
    val quest: Ref<QuestEntry> = emptyRef(),

    @Help("Simple built-in beam color. CUSTOM uses customTheme; INHERIT uses static_waypoint.")
    val preset: WaypointPreset = WaypointPreset.INHERIT,

    @Help("Advanced theme used only when preset is CUSTOM.")
    val customTheme: Ref<WaypointThemeEntry> = emptyRef(),

) : ManifestEntry

private data class CompiledQuestWaypoint(
    val entryId: String,
    val questId: String,
    val theme: WaypointThemeSnapshot?,
)

private object QuestWaypointCatalog {
    private val profilesByQuestId: Map<String, CompiledQuestWaypoint> by lazy(::compile)

    fun themeFor(questId: String?): WaypointThemeSnapshot? =
        questId?.let(profilesByQuestId::get)?.theme

    private fun compile(): Map<String, CompiledQuestWaypoint> {
        val candidates = Query.find<QuestWaypointEntry>()
            .filter { it.quest.isSet }
            .map { profile ->
                CompiledQuestWaypoint(
                    entryId = profile.id,
                    questId = profile.quest.id,
                    theme = when (profile.preset) {
                        WaypointPreset.INHERIT -> null
                        WaypointPreset.CUSTOM -> profile.customTheme.snapshotOrNull().also { theme ->
                            if (theme == null) {
                                org.bukkit.Bukkit.getLogger().warning(
                                    "[WaypointRPG] quest_waypoint '${profile.id}' uses CUSTOM without a valid customTheme; static_waypoint fallback will be used."
                                )
                            }
                        }
                        else -> builtInTheme(profile.preset)
                    },
                )
            }
            .toList()

        candidates.groupBy { it.questId }
            .filterValues { it.size > 1 }
            .forEach { (questId, duplicates) ->
                org.bukkit.Bukkit.getLogger().warning(
                    "[WaypointRPG] Multiple quest_waypoint entries select quest '$questId': " +
                        duplicates.joinToString { it.entryId } + ". Lowest stable entry ID wins."
                )
            }

        return candidates
            .groupBy { it.questId }
            .mapValues { (_, values) ->
                values.minBy { it.entryId }
            }
    }

    private fun builtInTheme(preset: WaypointPreset): WaypointThemeSnapshot {
        val (outer, inner) = when (preset) {
            WaypointPreset.PURPLE -> Material.PURPLE_STAINED_GLASS to Material.PURPLE_CONCRETE
            WaypointPreset.GREEN -> Material.GREEN_STAINED_GLASS to Material.GREEN_CONCRETE
            WaypointPreset.RED -> Material.RED_STAINED_GLASS to Material.RED_CONCRETE
            WaypointPreset.GOLD -> Material.YELLOW_STAINED_GLASS to Material.YELLOW_CONCRETE
            WaypointPreset.BLUE -> Material.LIGHT_BLUE_STAINED_GLASS to Material.LIGHT_BLUE_CONCRETE
            else -> error("Preset $preset has no built-in theme")
        }
        return WaypointThemeSnapshot(
            id = "preset:${preset.name.lowercase()}",
            beam = WaypointStyleBeamConfig(
                overrideMaterials = true,
                outer = outer,
                inner = inner,
            ),
            label = WaypointStyleLabelConfig(),
            symbol = WaypointStyleSymbolConfig(),
        )
    }
}

/**
 * Active waypoint definitions for each player.
 *
 * Entries register here while their Typewriter audience is active. Renderers and
 * integrations read the same definitions, so a route/entity is configured once.
 */
internal object WaypointTargetRegistry {
    private val routes = ConcurrentHashMap<UUID, ConcurrentHashMap<String, WaypointRoute>>()
    private val entities = ConcurrentHashMap<UUID, ConcurrentHashMap<String, EntityWaypointTarget>>()

    fun registerRoute(playerId: UUID, entryId: String, route: WaypointRoute) {
        routes.computeIfAbsent(playerId) { ConcurrentHashMap() }[entryId] = route
    }

    fun unregisterRoute(playerId: UUID, entryId: String) {
        routes[playerId]?.let { playerRoutes ->
            playerRoutes.remove(entryId)
            if (playerRoutes.isEmpty()) routes.remove(playerId, playerRoutes)
        }
    }

    fun registerEntity(playerId: UUID, entryId: String, target: EntityWaypointTarget) {
        entities.computeIfAbsent(playerId) { ConcurrentHashMap() }[entryId] = target
    }

    fun unregisterEntity(playerId: UUID, entryId: String) {
        entities[playerId]?.let { playerEntities ->
            playerEntities.remove(entryId)
            if (playerEntities.isEmpty()) entities.remove(playerId, playerEntities)
        }
    }

    fun routesFor(playerId: UUID): List<WaypointRoute> = routes[playerId]
        ?.entries
        ?.sortedWith(compareByDescending<Map.Entry<String, WaypointRoute>> { it.value.priority }.thenBy { it.key })
        ?.map { it.value }
        .orEmpty()

    fun entitiesFor(playerId: UUID): List<EntityWaypointTarget> = entities[playerId]
        ?.entries
        ?.sortedBy { it.key }
        ?.map { it.value }
        .orEmpty()

    fun resolve(
        player: Player,
        selection: WaypointTargetSelection,
        maxTargets: Int,
        includeObjectives: Boolean = true,
        includeEntities: Boolean = true,
    ): List<WaypointTarget> = resolveWaypointTargets(
        player = player,
        selection = selection,
        maxTargets = maxTargets,
        entityTargets = entitiesFor(player.uniqueId),
        includeObjectives = includeObjectives,
        includeEntities = includeEntities,
    ).map { target ->
        val questId = target.objective?.quest?.id ?: target.questId
        target.copy(style = target.style ?: QuestWaypointCatalog.themeFor(questId))
    }
}

@Entry(
    "waypoint_path",
    "Define a guided route for a selectable location objective.",
    Colors.BLUE,
    "mdi:map-marker-path"
)
class WaypointPathEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("Location objective that uses this route. Select it from the Typewriter panel.")
    val objective: Ref<LocatableObjective> = emptyRef(),

    @Help("Higher priority wins if several active routes select the same objective.")
    val priority: Int = 0,

    @Help("Allow advancing by reaching a later point before the current point.")
    val allowSkip: Boolean = true,

    @Help("Reset progress when this route leaves the player's active audience.")
    val resetOnObjectiveChange: Boolean = true,

    @Help("Loop to the first point after completing the route.")
    val resetOnComplete: Boolean = false,

    @Help("Ordered path points. Add and reorder them directly in the panel.")
    val points: List<WaypointRoutePoint> = emptyList(),
) : AudienceEntry {
    override suspend fun display(): AudienceDisplay = WaypointRouteDisplay(this)

    internal fun route(): WaypointRoute = WaypointRoute(
        objectiveId = objective.id,
        routeId = id,
        priority = priority,
        allowSkip = allowSkip,
        resetOnObjectiveChange = resetOnObjectiveChange,
        resetOnComplete = resetOnComplete,
        points = points,
    )
}

private class WaypointRouteDisplay(
    private val entry: WaypointPathEntry,
) : AudienceDisplay() {
    private val registeredPlayers = ConcurrentHashMap.newKeySet<UUID>()

    override fun onPlayerAdd(player: Player) {
        if (!entry.objective.isSet) return
        registeredPlayers += player.uniqueId
        WaypointTargetRegistry.registerRoute(player.uniqueId, entry.id, entry.route())
    }

    override fun onPlayerRemove(player: Player) {
        unregister(player.uniqueId)
    }

    override fun dispose() {
        registeredPlayers.toList().forEach(::unregister)
        super.dispose()
    }

    private fun unregister(playerId: UUID) {
        if (!registeredPlayers.remove(playerId)) return
        WaypointTargetRegistry.unregisterRoute(playerId, entry.id)
        if (entry.resetOnObjectiveChange) {
            val objectiveId = entry.objective.id
            globalRouteIndices.remove(routeStateKey(playerId, objectiveId, entry.id))
        }
    }
}

@Entry(
    "entity_waypoint",
    "Register an entity or Typewriter NPC as a waypoint target for this audience.",
    Colors.PURPLE,
    "mdi:crosshairs-gps"
)
class EntityWaypointEntry(
    override val id: String = "",
    override val name: String = "",

    @Help("How this entity is located.")
    val targetType: EntityTargetType = EntityTargetType.TYPEWRITER_NPC,

    @Help("Typewriter entity instance. Used when targetType is TYPEWRITER_NPC.")
    val npc: Ref<SimpleEntityInstance> = emptyRef(),

    @Help("Optional owning quest. Its quest_waypoint appearance is inherited.")
    val quest: Ref<QuestEntry> = emptyRef(),

    @Help("Optional advanced theme override for this entity.")
    val themeOverride: Ref<WaypointThemeEntry> = emptyRef(),

    @Help("Bukkit entity UUID. Used when targetType is UUID.")
    val uuid: String = "",

    @Help("Bukkit entity or custom name. Used when targetType is NAME.")
    val entityName: String = "",

    @Help("Scoreboard tag. Used when targetType is SCOREBOARD_TAG.")
    val scoreboardTag: String = "",

    @Help("Waypoint label. Blank uses the resolved entity or NPC name.")
    @Colored @Placeholder
    val displayName: Var<String> = ConstVar(""),

    @Help("Search radius for NAME and SCOREBOARD_TAG targets.")
    val maxDistance: Double = 128.0,

    @Help("Higher values are preferred by HIGHEST_PRIORITY selection.")
    val priority: Int = 0,
) : AudienceEntry {
    override suspend fun display(): AudienceDisplay = WaypointEntityTargetDisplay(this)

    internal fun target(): EntityWaypointTarget = EntityWaypointTarget(
        targetType = targetType,
        uuid = uuid,
        name = entityName,
        tag = scoreboardTag,
        displayName = displayName,
        maxDistance = maxDistance,
        priority = priority,
        npcEntryId = npc.id,
        registryId = id,
        questId = quest.id.takeIf { quest.isSet },
        themeOverride = themeOverride.snapshotOrNull(),
    )
}

private class WaypointEntityTargetDisplay(
    private val entry: EntityWaypointEntry,
) : AudienceDisplay() {
    private val registeredPlayers = ConcurrentHashMap.newKeySet<UUID>()

    override fun onPlayerAdd(player: Player) {
        registeredPlayers += player.uniqueId
        WaypointTargetRegistry.registerEntity(player.uniqueId, entry.id, entry.target())
    }

    override fun onPlayerRemove(player: Player) {
        unregister(player.uniqueId)
    }

    override fun dispose() {
        registeredPlayers.toList().forEach(::unregister)
        super.dispose()
    }

    private fun unregister(playerId: UUID) {
        if (!registeredPlayers.remove(playerId)) return
        WaypointTargetRegistry.unregisterEntity(playerId, entry.id)
    }
}
