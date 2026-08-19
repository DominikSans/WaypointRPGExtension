package cg.headpop.waypointrpg

import com.typewritermc.engine.paper.entry.entries.get
import com.typewritermc.engine.paper.utils.toBukkitLocation
import com.typewritermc.quest.entries.interfaces.LocatableObjective
import com.typewritermc.quest.entries.trackedShowingObjectives
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Single owner for path selection, advancement and reset across every integration. */
internal object WaypointPathProgress {
    private val activeObjectives = ConcurrentHashMap<UUID, Set<String>>()

    fun apply(player: Player, routes: List<WaypointRoute>, targets: List<WaypointTarget>): List<WaypointTarget> {
        if (routes.isEmpty()) {
            activeObjectives.remove(player.uniqueId)
            return targets
        }
        val activeNow = player.trackedShowingObjectives()
            .filterIsInstance<LocatableObjective>()
            .mapTo(mutableSetOf()) { it.id }
        val inactive = activeObjectives.put(player.uniqueId, activeNow).orEmpty() - activeNow
        inactive.forEach { objectiveId ->
            routes.filter { it.objectiveId == objectiveId }.forEach { route ->
                reset(player.uniqueId, objectiveId, route.routeId.ifBlank { objectiveId })
            }
        }
        return targets.map { target ->
            val objectiveId = target.objective?.id ?: return@map target
            resolve(player, objectiveId, routes, target)
        }
    }

    fun resolve(
        player: Player,
        objectiveId: String,
        routes: List<WaypointRoute>,
        directTarget: WaypointTarget,
    ): WaypointTarget {
        val route = routes.firstOrNull { it.objectiveId == objectiveId } ?: return directTarget
        val resolved = route.points.mapNotNull { point ->
            val position = runCatching { point.position.get(player) }.getOrNull() ?: return@mapNotNull null
            val location = runCatching { position.toBukkitLocation() }.getOrNull() ?: return@mapNotNull null
            Triple(point, position, location)
        }
        if (resolved.isEmpty()) return directTarget

        val routeId = route.routeId.ifBlank { objectiveId }
        val key = routeStateKey(player.uniqueId, objectiveId, routeId)
        var index = globalRouteIndices.getOrDefault(key, 0).coerceIn(0, resolved.size)
        val playerLocation = player.location

        // Skipping ahead is intentional and hardcoded: arriving at a later checkpoint
        // must never force a player to walk backwards through an older point.
        resolved.forEachIndexed { pointIndex, (point, _, location) ->
            val radius = point.radius.coerceAtLeast(0.1)
            if (pointIndex >= index && location.world == playerLocation.world &&
                playerLocation.distanceSquared(location) <= radius * radius
            ) index = pointIndex + 1
        }

        if (index >= resolved.size) {
            if (!route.resetOnComplete) {
                globalRouteIndices[key] = resolved.size
                return directTarget
            }
            index = 0
        }
        globalRouteIndices[key] = index
        val (point, position, location) = resolved[index]
        return directTarget.copy(
            position = position,
            location = location,
            distance = if (location.world == playerLocation.world) playerLocation.distance(location)
                else Double.POSITIVE_INFINITY,
            routePointIndex = index,
            routePointCount = resolved.size,
            routePointName = runCatching { point.name.get(player) }.getOrNull().orEmpty(),
        )
    }

    fun reset(playerId: UUID, objectiveId: String, routeId: String) {
        globalRouteIndices.remove(routeStateKey(playerId, objectiveId, routeId.ifBlank { objectiveId }))
    }
}
