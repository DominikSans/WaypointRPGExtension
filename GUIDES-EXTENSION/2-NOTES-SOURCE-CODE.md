# Notes — Source Code

> **Versión:** `v0.6.0-dev`  
> **Descripción:** migración, problemas, soluciones, warnings y extractos del código.  
> **Modificado:** jueves, 3 de julio de 2026 (America/Lima).

## Arquitectura

- `TrackedLocatableWaypointEntry.kt`: modelo V2 y render PacketEvents.
- `SimpleTrackedWaypointEntry.kt`: wrapper V1 deprecado.
- `WaypointZoneTriggerEntry.kt`: triggers de radio.
- `WaypointBetterHudBridgeEntry.kt`: integración BetterHUD opcional.
- Beam, label y symbol son entidades packet-only por jugador.

## Notas de migración de V1/simple_tracked_waypoint

Las configuraciones nuevas deben usar `tracked_locatable_waypoint`.

| V1 plano | V2 agrupado |
|---|---|
| `mode`, `selection`, `maxTargets` | `general.*` |
| `targetOffset`, `verticalThreshold` | `target.*` |
| `label`, `labelScale`, etc. | `label.*` |
| `symbolEnabled`, `symbolText`, etc. | `symbol.*` |
| `beamOuter`, `beamInner`, etc. | `beam.*` |
| `bobHeight`, `bobSpeed` | `bob.*` |
| `entityTargets`, `entityGlow` | `integrations.*` |
| `lazyUpdate`, `cleanupOnJoin` | `performance.*` |

`backend` ya no existe. `showBeam` se reemplaza con `beam.enabled` y `general.mode`.

## Problemas y soluciones

### Marcadores duplicados

Cada entry consulta todos los `trackedShowingObjectives()` del jugador. Use un solo entry con `general.maxTargets > 1` o audiencias mutuamente excluyentes.

### Entry visible para todos

Un `AudienceEntry` raíz recibe a todos los jugadores. Debe ser hijo de una audiencia de quest o criteria.

### Material inválido

`outer` e `inner` deben ser bloques no-AIR. Hay fallback a `LIME_STAINED_GLASS`, `LIME_CONCRETE` y finalmente `STONE`.

```text
[WaypointRPG] Beam outer material '...' is not a valid block. Using LIME_STAINED_GLASS.
```

### CraftEngine

El índice 23 debe usar `BLOCK_STATE`, no `INT`:

```kotlin
EntityData(23, EntityDataTypes.BLOCK_STATE, blockStateId)
```

### Texto y profundidad

`seeThrough` está reservado. `safeClamp` coloca el texto delante del beam:

```kotlin
val beamFrontDepth = horizontalDist - beamMaxHalf - 0.08
val safeClamp = if (beamFrontDepth > 1.0)
    minOf(entry.label.floatDist, beamFrontDepth)
else entry.label.floatDist
```

### Tirones laterales

El strafe recibe 35 % del look-ahead:

```kotlin
val parallel = desiredXZ.clone().multiply(vel.dot(desiredXZ))
val perpendicular = vel.clone().subtract(parallel)
val velocityOffset = parallel.multiply(lookAheadTicks)
    .add(perpendicular.multiply(lookAheadTicks * 0.35))
```

### Culling del beam

```kotlin
EntityData(20, EntityDataTypes.FLOAT, maxOf(sx, sz))
EntityData(21, EntityDataTypes.FLOAT, sy)
```

## Entity Targets — diseño interno (Paso 3)

`EntityWaypointTarget` admite tres modos:
- `UUID`: `Bukkit.getEntity(uuid)` — global, sin búsqueda por mundo.
- `NAME`: `world.entities` filtrado por `entity.name` o `customName()`, dentro de `maxDistance`.
- `SCOREBOARD_TAG`: `entity.scoreboardTags.contains(tag)`, elige la más cercana dentro de `maxDistance`.

Los entity targets se incluyen en el mismo pool que los objectives y quedan sujetos a `maxTargets`. La key de slot usa el UUID tras la resolución: `entity:$uuid`. `entityPriority` alimenta el tiebreaker al ordenar con `HIGHEST_PRIORITY`.

### Typewriter NPC

Los NPCs de Typewriter son fake entities gestionadas por EntityLib (packets). No son `org.bukkit.entity.Entity` y no pueden encontrarse con `Bukkit.getEntity()` ni `world.entities`. Se resuelven con `targetType = TYPEWRITER_NPC` y el entry ID del `NpcInstance`. El sistema usa `SharedAudienceEntityDisplay.position(playerUUID)` para posición live, con fallback a `NpcInstance.spawnLocation`.

### Placeholders en `updateLabel`

```kotlin
.replace("{target_type}", if (isEntityTarget) "entity" else "objective")
.replace("{entity_name}", if (isEntityTarget) markerName else "")
.replace("{entity_type}", entityTypeName ?: "")
```

`entityTypeName` = `entity.type.name` (ej. `"ZOMBIE"`, `"PLAYER"`, `"ARMOR_STAND"`).

## Routes V2 — diseño interno (Paso 6 nuevo)

**Problema previo**: `routeIndices` era `HashMap<String, Int>` local a `PlayerWaypointState` en el visual display. Zone trigger y BetterHUD no tenían acceso → cada entry tenía estado de ruta independiente.

**`globalRouteIndices: ConcurrentHashMap<String, Int>`** (package-level `internal`): almacén compartido. Key: `"$playerUUID:$objectiveId:$effectiveRouteId"`. Solo `TrackedLocatableWaypointDisplay.applyRoute` escribe (avanza). Zone trigger y BetterHUD leen vía `applyRouteReadOnly`.

**`routeStateKey(playerUUID, objectiveId, effectiveRouteId)`**: helper package-level para construir la key. `effectiveRouteId = routeId.ifBlank { objectiveId }`.

**`applyRoute(player, directTarget)`** (TrackedLocatableWaypointDisplay): reemplaza firma anterior `(player, state, directTarget)`. Ya no usa `state.routeIndices`. Lee+escribe `globalRouteIndices`.
- `allowSkip=true` (default): mismo comportamiento anterior — avanza por cualquier punto en radio.
- `allowSkip=false`: solo avanza el punto en `index == currentIndex`.
- `resetOnComplete=false` (default): al completar ruta, devuelve `directTarget` (objective final).
- `resetOnComplete=true`: al completar, resetea a 0 y devuelve primer punto.

**`applyRouteReadOnly(player, objectiveId, routes, directTarget)`** (package-level `internal`): lee `globalRouteIndices[key]` sin modificar. Devuelve el target apuntando al route point activo. Retorna `directTarget` si no hay ruta o route completo. Usado por zone trigger y BetterHUD.

**`resetOnObjectiveChange`**: en stale slot cleanup, si `key !in activeKeys` y `!key.startsWith("entity:")`, extrae `objectiveId = key.substringBefore(":")`, busca route con ese objectiveId, y si `resetOnObjectiveChange=true` → `globalRouteIndices.remove(routeStateKey(...))`. El índice se limpia cuando el objective desaparece.

**Cleanup por jugador**: `onPlayerRemove` → `globalRouteIndices.keys.removeIf { it.startsWith("$playerUUID:") }`. Evita acumulación de claves stale.

**`PlayerWaypointState.routeIndices`**: eliminado. Toda la state está en `globalRouteIndices`.

**Zone trigger `ACTIVE_ROUTE_POINT`**: nuevo valor en `ZoneTriggerTargetMode`. En `resolveZoneTargets`, si este modo activo, aplica `applyRouteReadOnly` a cada objective target. El trigger zona se mueve con el route point activo sin avanzar el índice. Solo el visual display avanza.

**Zone trigger `routes` field**: necesario para `ACTIVE_ROUTE_POINT`. Mismos datos que el visual entry. El índice compartido via `globalRouteIndices` garantiza que ambos apunten al mismo punto aunque sean entries distintos.

**BetterHUD `routes` field**: si configurado, `resolveTargets` aplica `applyRouteReadOnly` a cada objetivo. Punto BetterHUD sigue el route point activo. Misma mecánica de índice compartido.

**Placeholders de ruta**: `{route_index}`, `{route_total}`, `{route_name}`, `{route_remaining}`. Se rellenan en `updateLabel` si `target.routePointIndex != null`. Vacíos si target es entity o no tiene ruta.

## BetterHUD Bridge — diseño interno (Paso 5)

`WaypointBetterHudBridgeDisplay` reemplaza el bridge V1 (solo objectives, index-based IDs) con soporte completo V2.

**Resolver compartido**: llama `resolveWaypointTargets(player, selection, maxTargets, entityTargets, includeObjectives, includeEntities)` — misma función package-level que zone trigger.

**`BetterHudTargetMode`**: `OBJECTIVES_ONLY` / `ENTITIES_ONLY` / `ANY_ACTIVE_TARGET`. Controla `includeObjectives` e `includeEntities` en la llamada al resolver.

**`pointIdFor(target)`**: `"${prefix}${entry.id}_${target.zoneKey()}"` sanitizado y truncado a 96 chars. Estable aunque cambie el orden de sort — no más flicker.

**`PlayerHudState(activeIds, knownPositions)`**: rastrea qué puntos están activos y su última posición `Triple<Double,Double,Double>`. Si el target se mueve (entidad Bukkit, NPC Typewriter en patrol), `posChanged = knownPositions[id] != newPos` → remove + re-add del punto → posición actualizada en BetterHUD. Cambio visible al siguiente `updateIntervalTicks`.

**`arriveRadius`**: puntos filtrados antes de enviar a BetterHUD. Si `target.distance <= arriveRadius` → remove del HUD si estaba activo.

**`betterHudAvailable()`** companion object con `@Volatile` flag: check una sola vez, warning único en consola si ausente. No spam por tick.

**Cleanup**: `onPlayerRemove` → `removeAllPoints`, `dispose()` → todos los jugadores. `toRemove = state.activeIds - visible.keys` limpia puntos obsoletos (target desapareció o fuera de arrive radius).

**`resolveWaypointTargets`** — función package-level en `TrackedLocatableWaypointEntry.kt`. Usada por TrackedLocatableWaypointDisplay (vía llamada directa interna), WaypointZoneTriggerDisplay y WaypointBetterHudBridgeDisplay. El resolver de zone trigger fue simplificado para delegarla.

## WaypointZoneTrigger — diseño interno (Paso 4)

`WaypointZoneTriggerDisplay` reemplaza el `Boolean` por jugador con `PlayerZoneState(insideKeys, triggeredOnceKeys)` para tracking per-key.

**resolveZoneTargets**: collecta objectives via `trackedShowingObjectives().filterIsInstance<LocatableObjective>()` + entity targets shared con `resolveEntityTarget` / `resolveTypewriterNpcTarget` (funciones package-level en `TrackedLocatableWaypointEntry.kt`). Aplica misma sort + `maxTargets`.

**zoneKey()**: extension `internal` en `WaypointTarget`. Prioridad: `entityUUID` → `sourceId` (NPC: `"npc:<entryId>"`) → objective+posición con precisión 0.25 bloques.

**checkPlayer logic**:
```
effectiveInRadius = { inRadiusKeys } si triggerPerTarget, o {"§any"} si alguno en radio
toExit = insideKeys - effectiveInRadius  → onExit + remove + resetOnExit
toEnter = effectiveInRadius - insideKeys → skip si triggerOnce+triggeredOnce, else onEnter + add
```

Cuando targets desaparecen mid-session: ya no están en `effectiveInRadius` → incluidos en `toExit` automáticamente (sin lógica extra).

**Funciones compartidas** (package-level, `internal`): `resolveEntityTarget`, `resolveTypewriterNpcTarget`, `findEntityByUuid`, `WaypointTarget.zoneKey()`.

`WaypointTarget` ahora `internal data class` con campo `sourceId: String?` para NPC key estable.

## Límites conocidos

- El fondo del label se suprime en modo `BOTH`.
- `label.opacity` también controla el symbol.
- `label.seeThrough` no tiene efecto actual.
- `NAME` y `SCOREBOARD_TAG` llaman `world.entities` cada resolución — evitar en listas largas con `tickRate` bajo.
- Los índices de ruta pueden conservarse al reactivar un objective.
- La escala del symbol usa distancia 3D; snap usa distancia horizontal.
- Zone trigger y BetterHUD usan `applyRouteReadOnly` (read-only). Solo el visual display avanza el índice en `globalRouteIndices`.
- BetterHUD `PointedLocation` no acepta texto vía API — `pointText`/`pointSubText` son campos de panel pero el texto real se configura en el layout BetterHUD.
- El glow escribe shared flags `0x40`/`0x00`.

## Threading, cleanup y warnings

Las llamadas Bukkit pasan por `runSync`; el estado usa `ConcurrentHashMap` y `AtomicBoolean`. Se limpian slots al remover objectives, cambiar mundo, salir del audience y hacer dispose.

No silenciar warnings de material inválido, block state, spawn metadata, teleport o usuario PacketEvents ausente. BetterHUD ausente sí es un no-op.

## Dependencias

Java 21, Kotlin 2.2.10, Typewriter `0.9.0-beta-173`, module plugin `2.1.0`, PacketEvents `2.9.4` y BetterHUD API `1.14.1` como `compileOnly`.

## Política Git

- Desarrollo: `v0.x.y-dev`.
- Tras confirmación explícita y pruebas correctas: registrar resultado, crear commit estable y hacer push a `main`.
- Compilar no equivale a aprobar una prueba manual.
