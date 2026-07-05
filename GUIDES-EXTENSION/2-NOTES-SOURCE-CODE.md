# Notes — Source Code

> **Versión:** `v0.7.0-dev`  
> **Descripción:** migración, problemas, soluciones, warnings y extractos del código.  
> **Modificado:** domingo, 5 de julio de 2026, 11:35 `(-05:00)` (America/Lima).

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

### Glow y metadata compartida

El glow client-side ya no pisa ciegamente el byte completo de metadata. Ahora reconstruye flags compartidos y solo añade o quita el bit `0x40` del outline.

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

### Cache de resolución para `NAME` y `SCOREBOARD_TAG`

`NAME` y `SCOREBOARD_TAG` ya no recorren `world.entities` en cada tick forzosamente. El display guarda un cache corto por jugador y selector, valida por UUID y solo reescanea si el target dejó de coincidir o al refresco periódico.

`NAME` también cambió de `firstOrNull` a objetivo más cercano dentro de `maxDistance`, para evitar resultados inestables según el orden interno de entidades.

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

## Hardening visual — lifecycle, interpolación y cleanup (auditoría v0.7)

**Key de slot estable para objectives con ruta**: `key()` incluía `routePointIndex` y la posición → al avanzar de punto 0 a 1, la key cambiaba y el slot completo (beam + label + symbol) se destruía y recreaba (pop brusco en cada avance). Ahora un objective con ruta configurada usa la key fija `"$objectiveId:route"` durante todo el recorrido, incluido el traspaso al objetivo final: las entidades persisten y se deslizan al siguiente punto vía teleport interpolado.

**Dedupe de targets por key**: un objective multi-posición con ruta colapsaba todas sus posiciones al mismo route point → misma key → el mismo slot se actualizaba 2× por tick con `lateralVec` distinto (jitter). `resolveTargets` aplica `distinctBy { it.key() }` tras `applyRoute`.

**`TEXT_INTERP = 1` constante**: label y symbol reciben teleport cada tick; `interpolation_duration`/`teleport_duration` deben ser 1 para que el cliente complete exactamente una ventana de interpolación por update. El valor anterior alternaba 2/5 según la fase de tick: cada cambio alteraba el hash de metadata (resends inútiles) y un duration > cadencia real reinicia la ventana en cada teleport → la entidad arrastra N ticks por detrás (rubber-band). El path de arrive usaba `interp = tickRate` (5) con bob teleportando cada tick — mismo defecto, corregido.

**Flag `hidden` en `FakeTextDisplay`**:
- `hideFakeDisplay` era llamado cada tick mientras el label estaba oculto (hideRange/FOV) y enviaba el metadata scale-0 cada vez (`lastMetadataHash = null` impedía dedupe). Ahora si `hidden == true` no envía nada.
- Re-aparición tras hide: metadata con `duration = 0` + teleport inmediato → aparece instantáneo en la posición nueva. Antes interpolaba scale/posición desde el estado oculto viejo → streak deslizante por la pantalla.

**Histéresis en 3 fronteras (latches por slot)**:
- `arrivedLatch`: entra a `arriveRadius`, sale a `arriveRadius + 0.75`.
- `labelHideLatch`: oculta a `hideRange`, muestra a `hideRange + 0.75`.
- `beamFadeLatch`: beam se destruye a `thinFactor ≤ 0.01`, reaparece a `> 0.05`.
Sin histéresis, quedarse parado en la frontera exacta causaba create/destroy o hide/show cada tick. Los latches se resetean en `clearSlotVisualState`.

**Dedupe de teleport en TextDisplay**: `teleportFakeDisplay` omite el packet si la posición no cambió (> 1e-4). Jugador quieto con bob desactivado ya no teleporta cada tick.

**`globalRouteIndices` scoped por entry**: `onPlayerRemove` borraba TODAS las keys `"$uuid:*"` — un entry visual borraba el progreso de rutas de otros entries para ese jugador. Ahora `clearOwnRouteIndices` borra solo las keys de las rutas definidas en este entry (`uuid:objectiveId:effectiveRouteId`). `dispose()` también limpia sus propias keys para todos los jugadores registrados.

**Prune del `entitySelectorCache` cada 40 ticks** en vez de cada tick (construir el set de keys válidas por tick por jugador era trabajo desperdiciado).

**BetterHUD — epsilon de movimiento**: `syncPoints` comparaba la posición exacta → cualquier entidad móvil provocaba remove+add del pointer en cada sync (flicker de brújula). Ahora re-agrega solo si el target se movió > 0.5 bloques (por debajo de la resolución angular de la brújula).

## Hardening fase 2 — micro-perf, packet discipline y handoffs (auditoría v0.7)

**Caches de resolución por jugador** (`PlayerWaypointState`):
- `cachedObjectiveTargets` + `objectiveTargetsRefreshTick`: `trackedShowingObjectives()` + resolución de position Vars se ejecuta cada 5 ticks (`OBJECTIVE_RESOLVE_INTERVAL_TICKS`), no cada tick. Las distancias del cache envejecen ≤5 ticks — solo afectan orden de sort; `updateSlot` recalcula distancias reales desde las locations cada tick. `force=true` (onPlayerAdd) refresca inmediato.
- `routePointCache`: puntos de ruta (Var + conversión a Location) resueltos cada 20 ticks (`ROUTE_POINT_RESOLVE_INTERVAL_TICKS`). Distancias de avance siempre frescas. Limpieza en `destroyAllSlots` y `dispose`.

**Cuantización anti-resend** (el hash de metadata cambiaba cada tick por floats continuos → resend completo de metadata por tick):
- `thinFactor` → pasos de 1/32.
- `symbolScale` → pasos de 0.05.
- `finalOpacity` (fade FOV) → pasos de 8.
El cliente interpola entre pasos; visualmente indistinguible.

**Hash de inputs del label** (`lastInputsHash` en `FakeTextDisplay`): la cadena de 12 `.replace()` creaba ~12 strings intermedios por slot por tick. Ahora se calcula un hash compuesto barato (template + name + distKey + arrow + índices + campos entity/route) y solo se reconstruye el texto cuando algo visible cambió. `distKey` tiene la misma granularidad que `formatDistance` (1 m / 0.1 km).

**`System.identityHashCode(text)`** en el hash de metadata: los components están cacheados/reutilizados, identity estable = contenido estable; evita hashear el árbol del Component en cada llamada.

**Menos allocs por tick**: eliminados clones redundantes de Location/Vector (resultados de `smoothLocation` y `eyeLocation`/`velocity` ya son instancias frescas; posiciones de label/symbol construidas componente a componente en un solo `Location`). `smoothLocation` devuelve `target` directo cuando no hay previous (callers siempre pasan Location fresco).

**`smoothLocation` — política por tipo con curvas continuas**: los brackets discretos de alpha producían "cambios de marcha" visibles al cruzar un borde. Ahora cada perfil es una curva continua (`lerp(alphaMin, 1.0, smoothstep(...))`): BEAM el más firme (0.70→1.0), LABEL amortigua deltas milimétricos (0.36→1.0), SYMBOL intermedio (0.46→1.0), SYMBOL_SNAP clavado (0.70→1.0). Y sigue pasando directo (sin lerp) en tracking normal — el bob se mantiene crisp.

**Glide de handoff**: deltas > 2.5 bloques solo ocurren cuando el target cambió (avance de route point, swap de selector, route → objetivo final). En vez de snap duro: convergencia exponencial (38 %/tick) con paso mínimo garantizado de 2.5 bloques/tick → un handoff de 30 bloques se recorre en ~5-7 ticks y siempre termina. Durante el glide, Y también interpola (barrido diagonal, no en L).

**Slots estables para selectores**: targets `NAME`/`SCOREBOARD_TAG` llevan `sourceId = "sel:<cacheKey>"`. Si el selector cambia de entidad (la vieja murió, apareció una más cercana), la key de slot no cambia → las entidades visuales deslizan a la nueva entidad en vez de destroy+respawn. `zoneKey()` prioriza `sourceId` igual → zone trigger y BetterHUD mantienen identidad en el swap (sin exit+enter ni remove+add del pointer).

## Hardening fase 3 — instrumentación, hide/show de beam y scalarización (auditoría v0.7)

**Instrumentación (`WaypointStats`)**: objeto file-level activable con `-Dwaypointrpg.debug=true` (flag JVM al arrancar el server). Cuando está apagado, `enabled` es constante de arranque → los incrementos guardados se JIT-fold a no-ops (costo cero, cero spam). Encendido, loguea una línea cada 10 s con: teleports/s y metadata/s por tipo (beam/label/symbol), spawn/destroy/hide/reshow por minuto, resolves/s (objectives, scans de selector, route points), slots promedio por player-tick, y tamaños de `states`/slots/`entitySelectorCache`/`glowBaseFlags`/`globalRouteIndices`. Los counters se resetean por ventana → comparaciones antes/después directas. Solo mutados en main thread.

**Beam hide/re-show sin destroy** (`ActiveBeam.hidden` + `hideBeamSlot`): el `beamFadeLatch` y el arrive destruían y respawneaban los 2 BlockDisplay (spawn + metadata completo + remap de CraftEngine) cada vez que el jugador cruzaba la frontera de fade o de arrive. Ahora: hide = 1 metadata scale-0 por entidad; re-show = metadata duration-0 + teleport en el mismo tick (misma receta que el texto) → aparece en posición sin slide. El label en arrive también pasa de destroy a `hideFakeDisplay`.

**Latch de `verticalColumnMode`**: era la única frontera de visibilidad sin histéresis tras fase 1 — la condición cruda toca dos umbrales a la vez (`snapRange` y `verticalThreshold`); flotar sobre cualquiera alternaba hide/show del label y la rama snap del símbolo cada tick. Entra con la condición original exacta, sale con margen (snapRange+0.75 / threshold−1).

**Cadencia adaptativa de objectives** (refuerzo del riesgo de fase 2): en cada resolve fresco se compara la posición nueva con la cacheada; si algo se movió (>1e-3) el próximo refresh es en 1 tick (tracking a 20 Hz para LocatableObjectives móviles), si no, vuelve a 5 ticks. Cambios de tamaño del set también fuerzan follow-up rápido.

**`HANDOFF_MAX_ALPHA = 0.65`**: la fórmula del glide degeneraba con deltas cercanos a `HANDOFF_MIN_STEP` (2.5–3.85 bloques → alpha ≈ 1.0 = snap duro de 1 tick, exactamente el caso de route points contiguos). Con el cap, los handoffs cortos barren en 2-3 ticks; deltas > ~6.6 bloques no cambian (gana 0.38·d).

**Scalarización de `updateSlot`** (~10 allocs menos por slot por tick): dirección horizontal como `dirX/dirZ` (dx/dz ya existían — fuera `toVector()`×2 + Vector temporal + normalize), lateral como `latX/latZ` (fuera el `Vector(0,0,0)` del caso común), descomposición de velocity en escalares (fuera el `clone()` y la 2ª llamada a `player.velocity` — `velYAbs` capturado), FOV con forward derivado de yaw/pitch (fuera `direction` que aloca Vector), `rawAnchor` construido en un solo Vector, snap del símbolo componente a componente (fuera clone + toVector×2 + normalize). `playerFeet = player.location` se resuelve una vez por jugador por tick y se enhebra (antes hasta 4 allocs por slot: arrow, beam position, beam height, glow — `updateBeam` ahora usa el param `playerY` que recibía y antes ignoraba). `bobY` calculado una vez por jugador.

**Hashes sin boxing**: `sendFakeTextMeta` y `updateBeam` usaban `listOf(...).hashCode()` — lista + boxing de cada primitivo por display por tick solo para deduplicar. Ahora rolling hash manual con `toRawBits()`.

**Memo de slot key** (`WaypointTarget.slotKeyCache`): `key()` se llama varias veces por tick (stale sweep, lookup de slot, distinctBy) y las keys posicionales construían el string del UID del mundo cada vez; los targets viven ≥5 ticks en `cachedObjectiveTargets`. Seguro: los targets nunca se comparten entre displays y los inputs de la key son inmutables.

**Stale sweep sin colecciones**: el barrido de slots muertos usaba `map{key()}.toSet()` + `filter` (3 colecciones por jugador por tick); ahora es un scan directo con iterator sobre el mapa de slots (≤ maxTargets entradas), cero allocs cuando no hay cambios.

**Config estática del label parseada una vez**: `bgColor` (parse de color), `alignBits` y `billboard` (`trim().uppercase()` = 2 strings) se calculaban por label por tick para valores inmutables tras cargar el entry → ahora son `val` del display.

**`clearSlotVisualState` resetea `symbolSnapped` y `verticalColumnLatch`**: un slot reciclado tras world change o stale sweep no debe despertar creyendo que el símbolo sigue snapeado al target viejo.

### Cierre de riesgos (fase 3b)

- **TTL de entidades ocultas** (`HIDDEN_ENTITY_TTL_TICKS = 600`): beam/label/symbol ocultos más de 30 s se destruyen de verdad (`hiddenAtTick` en `ActiveBeam`/`FakeTextDisplay`). El re-show tardío pasa por el spawn path (first-frame duration 0) — visualmente idéntico al re-show por metadata. Cruces rápidos de frontera siguen en el path barato hide/show.
- **Re-show con teleport-first** (beam, label y symbol): el hide deja `teleport_duration=0` en el cliente → el teleport de re-show reposiciona instantáneo mientras la entidad sigue invisible; luego UN metadata con duration normal restaura la escala en sitio. Antes: meta duration-0 + teleport + meta extra al tick siguiente (el hash incluye duration). Label 3→2 packets por re-show; beam 6→4. Sin streak, sin pop.
- **Glide propio del beam** (`BEAM_GLIDE_ALPHA=0.50`, `MIN_STEP=4.0`, `MAX_ALPHA=0.75`): la masa visual grande converge en ~5 ticks para 30 bloques (texto mantiene 0.38/2.5/0.65). Siempre continuo — el cap impide snap de 1 tick.
- **Política de cadencia adaptativa endurecida**: deadband `OBJECTIVE_MOVE_EPSILON=0.01` (10× el epsilon anterior — el micro-ruido de re-resolución de Vars queda debajo) + histéresis de salida `objectiveStillResolves` (3 resolves quietos consecutivos antes de volver a 5 ticks; un mover que pausa un resolve no rebota la cadencia 1↔5).
- **Pasada de lifecycle**: verificado sin retención — `glowBaseFlags` se limpia antes del intento de packet en todos los paths de unglow (muerte de entidad, quit, dispose); `entitySelectorCache` expira por prune de 40 ticks + refresh; `routePointCache` acotado por config y limpiado en `destroyAllSlots`/`dispose`; latches (arrive/hide/fade/vertical/snapped) todos en `clearSlotVisualState`; TTL cierra el único estado que podía vivir indefinidamente.

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
- `NAME` y `SCOREBOARD_TAG` siguen pudiendo reescanear `world.entities`, pero ya no en cada tick si el target sigue siendo válido.
- Los índices de ruta pueden conservarse al reactivar un objective.
- La escala del symbol usa distancia 3D; snap usa distancia horizontal.
- Zone trigger y BetterHUD usan `applyRouteReadOnly` (read-only). Solo el visual display avanza el índice en `globalRouteIndices`.
- BetterHUD `PointedLocation` no acepta texto vía API. `pointText` y `pointSubText` fueron eliminados; el texto se configura en el layout BetterHUD.
- `entity.isVisualFire` compila con warning deprecado en esta API; se mantiene hasta definir reemplazo compatible.

## Threading, cleanup y warnings

Las llamadas Bukkit pasan por `runSync`; el estado usa `ConcurrentHashMap` y `AtomicBoolean`. Se limpian slots al remover objectives, cambiar mundo, salir del audience y hacer dispose.

No silenciar warnings de material inválido, block state, spawn metadata, teleport o usuario PacketEvents ausente. BetterHUD ausente sí es un no-op.

## Dependencias

Java 21, Kotlin 2.2.10, Typewriter `0.9.0-beta-173`, module plugin `2.1.0`, PacketEvents `2.9.4` y BetterHUD API `1.14.1` como `compileOnly`.

## Política Git

- Desarrollo: `v0.x.y-dev`.
- Tras confirmación explícita y pruebas correctas: registrar resultado, crear commit estable y hacer push a `main`.
- Compilar no equivale a aprobar una prueba manual.
