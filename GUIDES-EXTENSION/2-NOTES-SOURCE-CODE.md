# Notes — Source Code

> **Versión:** `v0.7.0-dev`  
> **Descripción:** migración, problemas, soluciones, warnings y extractos del código.  
> **Modificado:** domingo, 5 de julio de 2026, 18:30 `(-05:00)` (America/Lima).

## Arquitectura

- `TrackedLocatableWaypointEntry.kt`: entry principal V2, render PacketEvents.
- `WaypointZoneTriggerEntry.kt`: triggers de radio.
- `WaypointBetterHudBridgeEntry.kt`: integración BetterHUD opcional.
- Beam, label y symbol son entidades packet-only por jugador.

> `simple_tracked_waypoint` (SimpleTrackedWaypointEntry) fue eliminado en fase 7. No existe adaptador de compatibilidad. Usa `tracked_locatable_waypoint` directamente.

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

**Glyphs de `{direction}` — snippets nativos de Typewriter**: los 10 símbolos (`up/down/north/northeast/east/southeast/south/southwest/west/northwest`) son snippets del engine bajo las claves `waypoint.direction.*` en `plugins/Typewriter/snippets.yml`. Defaults: `▲ ▼ ↑ ↗ → ↘ ↓ ↙ ← ↖`. Mecánica verificada en el bytecode de `engine-paper-0.9.0` (`SnippetDatabaseImpl`): el engine escribe cada default en el yml la primera vez que se usa, sirve toda lectura desde un cache en memoria (un lookup de mapa, sin I/O de disco ni allocations por tick) y refresca con `/tw reload`. Los delegates `by snippet(...)` DEBEN declararse a nivel de archivo — `Snippet<T>` implementa `ReadOnlyProperty<Nothing?, T>`, no compila dentro de un `object`. `DirectionGlyphs.get(key)` queda como único punto de resolución (when sobre los 10 delegates). Mismo patrón que `RoadNetworkExtension` oficial (misma versión). El archivo custom anterior `direction-glyphs.yml` (en `plugins/Typewriter/`) ya no se lee; si existe, se loguea un warning una sola vez al cargar la clase.

**Placeholders nativos de Typewriter para glyphs**: el entry `tracked_locatable_waypoint` expone `%typewriter_<entry-id>:direction:up%`, `%typewriter_<entry-id>:direction:north%`, etc. vía `PlaceholderEntry.parser()`. Snippets ≠ placeholders: el snippet es un valor de configuración server-side que el código lee por delegate; el placeholder es texto expandido para chat/PAPI. Ambos conviven: el placeholder resuelve leyendo el mismo `DirectionGlyphs.get`, así que un cambio en `snippets.yml` se refleja en los dos.

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

### Clean animation final (fase 4)

Commit base: `c9b8163` (fases 1–3b). Tres cambios, todos guiados por artefactos concretos en código:

- **Curva continua para `alphaXZ` del anchor**: la tabla de brackets (0.55/0.70/0.85/0.90/0.95 con bordes en 0.25/0.6/1.0/1.2/2.0) cambiaba la rigidez del follow a saltos cada vez que el delta por tick cruzaba un borde — el mismo "gear shift" que ya se había eliminado de los perfiles de `smoothLocation` en fase 2. Reemplazada por `lerp(lerp(0.55, 0.95, smoothstep(0.05, 1.2, d)), 1.0, smoothstep(1.2, 2.0, d))`. Endpoints idénticos (0.55 en reposo, 0.95 en 1.2, 1.0 en 2.0) → misma firmeza, sin discontinuidades.
- **Blend continuo para el damping vertical del anchor**: el switch binario en `|velY| > 0.45` volteaba `baseAlphaY`/`maxStepY` 4 veces por arco de salto (subida y caída cruzan el umbral en ambos sentidos). Ahora `smoothstep(0.35, 0.55, velYAbs)` mezcla los mismos endpoints (0.18→0.24 / 0.16→0.24) sobre la banda donde estaba el umbral.
- **Metadata parcial en `sendFakeTextMeta`**: el hash único forzaba el set completo de 15 entries ante cualquier cambio. Ahora detección separada texto vs estilo runtime (scale/opacity/duration — el resto es config inmutable del entry): solo texto (el label de distancia avanza un metro) → 1 entry; solo estilo (step de escala del symbol, step de opacity por FOV) → 5 entries (8/9/10/12/26 — mismo patrón parcial que ya usaba el hide meta); ambos → parciales combinados; first frame / re-show tras hide → set completo. La metadata de entidad es sticky client-side, así que enviar solo índices dirty es el comportamiento estándar del protocolo. Counter nuevo: `partial=` en la línea de stats (`WaypointStats.partialMeta`).

No tocado (justificación): el salto de escala del symbol snapped→arrived (minScale→maxScale en 1 tick de interp) es un cambio de estado deliberado que comunica la llegada — suavizarlo sería smoothing decorativo con más metas. El doble meta del first frame (duration 0→interp al segundo tick) es una vez por vida de entidad y ahora cuesta 5 entries en vez de 15.

### Rotación tipo beacon vanilla (`beam.rotateLikeBeacon`, opcional, default false)

Como en el beacon vanilla: **solo la capa interior rota; el vidrio exterior queda estático**. Ritmo vanilla 2.25°/tick (`BEAM_ROTATE_DEG_PER_TICK`; core cuadrado → período visual de 90° ≈ 1 s por cara).

- **Mecánica**: keyframe de quaternion (index 13) cada `BEAM_ROTATE_PERIOD_TICKS=10` con `interpolation_duration` (index 9) = 10 → el cliente hace slerp continuo entre keyframes. **1 metadata cada 10 ticks (2/s) por beam** — costo trivial frente a los 40 teleports/s de un beam en follow.
- **Firmeza intacta**: `teleport_duration` (index 10) mantiene la cadencia interna fija del beam en los keyframes → los teleports del beam siguen interpolando exactamente igual; la rotación añade cero lag posicional. Sin Y, sin bob, sin glow.
- **Rotación sobre el eje de la columna**: la translation de centrado (−sx/2, 0, −sz/2) se rota por el mismo ángulo (`−R·c`); sin esto el BlockDisplay giraría alrededor de su esquina origen. Desviación cuerda-vs-arco del lerp de translation entre keyframes ≈ 2 % del half-width (~2 mm) — invisible.
- **Ángulo módulo 720°** (no 360): los componentes del quaternion tienen período 720° → dot positivo entre keyframes consecutivos → el slerp del cliente nunca toma el camino largo en el wrap.
- **Convivencia**: spawn de la capa interior ya con el ángulo actual (no barre desde 0°); re-show tras hide restaura con duration=interp al ángulo actual y el siguiente keyframe retoma el giro; escala/altura de la capa interior viajan en el keyframe (≤0.5 s de latencia en un step 1/32 de thinFactor — invisible en el core dentro del vidrio); la capa exterior mantiene el hash-gate instantáneo. Culling width ×1.5 (≥√2) para que las esquinas giradas no salgan del AABB.
- Ángulo derivado de `tickSerial` (los beams de un jugador giran en fase; lag del servidor ralentiza el giro como el gameTime vanilla).

### Cadencia interna y fluidez conjunta beam+label (fase 5)

- **`beamTickRate` eliminado del panel/JSON**: ahora es política interna `INTERNAL_BEAM_TICK_RATE = 1`. El beam teleporta con `teleport_duration = 1`, exactamente igual que label y symbol (`TEXT_INTERP = 1`): los tres visuales comparten una ventana de interpolación por tick de servidor — movimiento en lockstep, cero trailing entre beam y texto. Cualquier valor mayor hacía tartamudear el beam contra el label per-tick; por eso dejó de ser configurable. JSONs viejos con el campo se ignoran silenciosamente (Gson descarta campos desconocidos).
- **`fullUpdate` estaba muerto**: `general.tickRate` generaba el flag pero NADA lo consumía — el setting del panel no afectaba ningún visual. Re-empleado como cadencia de trabajo periódico secundario: el bloque de glow (entity lookup + alloc de location + distance por slot) ahora corre cada `tickRate` ticks (default 5 → latencia de activación 0.25 s, invisible) en vez de cada tick. Help actualizado.
- **`fullBeamUpdate` eliminado**: con rate interno 1 era siempre true — rama, contador (`beamTickCounter`) y parámetros muertos fuera. El beam corre cada tick incondicionalmente dentro de `doUpdate`.
- **`dynamicHeight` cuantizado con histéresis** (`BEAM_BASE_STEP=4.0`, `BEAM_BASE_RAISE_HYSTERESIS=8.0`): la base cruda (`min(pointerY, playerY) − 20`) seguía el Y del jugador continuamente — cada tick de movimiento vertical (caídas, escaleras, elytra, saltos bajo el Y del target) cambiaba `beamBaseY` (2 teleports) Y `scaleY` (hash miss → 2 transform metas): **4 packets/tick por beam para un fondo 20+ bloques bajo el jugador, sin cambio visible**. Ahora la base baja inmediato en pasos de 4 bloques (cobertura primero) y solo sube cuando la base necesaria queda 8 bloques por encima — rebotar sobre una frontera nunca la togglea. El top visible (`pointerY + height`) queda exacto. `dynBaseY` en `ActiveBeam` (NaN inicial, reset). Bonus: con `rotateLikeBeacon`, `scaleY` estable = keyframes del core sin variación parásita.
- **Fast path de 1 target en `resolveTargets`**: el caso común (un objective, sin entity targets) se salta el sort con comparator, el `take()` y el `distinctBy` — 3 colecciones menos por jugador por tick bajo carga.

### Respuesta frontal del label (fase 5b — fix de prueba manual)

Síntoma reportado en servidor: con `floatDist=5`, corriendo de frente hacia el waypoint el label se atrasa/da tirones. Diagnóstico con números:

- **Look-ahead muerto por unidades**: `smoothstep(0.35, 1.50, speed)` con `player.velocity` en bloques/TICK — sprint (0.28 b/t) queda debajo del edge inferior → el look-ahead nunca se activaba corriendo. Recalibrado a `smoothstep(0.18, 0.42)` (walk ~0.22, sprint ~0.28, speed/salto ~0.36+). Adelanto frontal cap 0.55 bloques (`parAdvance.coerceIn(-0.55, 0.55)`) — floatDist efectivo máx ≈ 5.5, bajo el techo de 6 que el usuario marcó como demasiado.
- **Doble smoothing isotrópico en cascada**: anchor α≈0.59 a delta 0.28 (lag ~0.19 bloques) y encima `smoothLocation` LABEL α≈0.36 (lag ~0.29) → ~0.5 bloques de retraso estacionario que respiraba con la velocidad = tirones. El damping existe para el shimmer LATERAL (jitter de yaw girando `dir`); el avance frontal es movimiento limpio y no lo necesita.
- **Fix: anisotropía frontal/lateral en ambas etapas.** El delta XZ se descompone en paralelo a `dir` (jugador→waypoint) y perpendicular. Paralelo pasa casi directo (anchor: 0.80→1.0 sobre `smoothstep(0.02, 0.60)`; smoothLocation LABEL/SYMBOL: 0.70→1.0 sobre `smoothstep(0.02, 0.25)`); el eje lateral conserva EXACTAMENTE las curvas anteriores → el shimmer lateral queda igual de muerto. `smoothLocation` recibe `dirX/dirZ` opcionales (0,0 = isotrópico: beam, snap, arrive, glide sin cambio).
- Resultado calculado a sprint frontal: lag total ~0.5 → ~0.04 bloques, con α estable (0.89–1.0) en el rango — sin respiración, sin tirones. Y del label intacta (bob crisp), glide de handoff intacto.

### Etapa única de smoothing + lockstep text/icon (fase 5c — fix de prueba manual)

Dos residuos reportados tras 5b: (1) micro-hitch en el arranque frontal, (2) desfase entre label text y symbol en correcciones A/D.

- **Causa de (2)**: label y symbol partían del mismo `visualAnchor` pero cada uno re-suavizaba por separado en `smoothLocation` con floors laterales distintos (LABEL 0.36 vs SYMBOL 0.46) y estado `previous` propio → el icon convergía ~27 % más rápido que el texto en correcciones laterales. **Fix**: en tracking normal `smoothLocation` hace passthrough para callers con `dir` (label/symbol follow) — ambos toman anchor + offset fijo → lockstep exacto. La rama de glide se conserva (handoff, snap→unsnap convergen desde su posición stale y luego entran en lockstep).
- **Causa de (1)**: doble etapa paralela residual — anchor 0.80 × label 0.70 en deltas pequeños → respuesta neta ~0.58 en los primeros ticks de avance. **Fix**: el anchor es la ÚNICA etapa; floor paralelo 0.85, llega a 1.0 en 0.20 bloques/tick (sprint = lock total).
- **Conservación de la vibración lateral**: floor lateral del anchor bajado a 0.20 = el neto de las dos etapas viejas (0.55 × 0.36 ≈ 0.198) → el micro-shimmer queda igual de amortiguado con una sola etapa; las correcciones A/D responden algo más firmes e idénticas para texto e icono.
- Y del anchor (smoothedBaseY + bob) intacta; SYMBOL_SNAP, arrive, beam y glide sin cambios.

### Look-ahead con desplazamiento real (fase 5d — fix de prueba manual)

Residuo tras 5c: retraso frontal ligero pero visible al correr — "el label intenta ponerse en su posición pero no llega a tiempo". La causa NO era smoothing ni constantes: **`player.velocity` es poco fiable para jugadores** (movimiento client-authoritative; el motion vector del servidor subestima y retrasa el desplazamiento real — sprint reporta ~0.21 en vez de 0.28, con delay). El look-ahead — cuya función es cubrir la latencia estructural de ~2 ticks (1 de server→packet + 1 de ventana de interpolación duration-1) — entraba tarde y corto.

- **Fix**: el estimador de movimiento es ahora el **delta real de `playerEyes` por tick** (contra `lastPlayerLocation`, calculado en `updatePlayerSync` antes de sobrescribir), cap ±1.0 por eje (teleports/lag spikes no catapultan el anchor). Exacto y disponible el mismo tick. Elimina además el alloc de `player.velocity`.
- Con sprint real (0.28): `speed01 ≈ 0.42` → lookAhead ≈ 1.83 ticks → `parAdvance ≈ 0.51` — cubre la latencia estructural; el label queda clavado en vez de perseguir. Ventanas de 5b sin cambios (ya estaban calibradas en bloques/tick — solo que velocity nunca las alcanzaba).
- `vertT` (damping vertical del anchor) también usa `|dispY|`. Bonus: parado, el desplazamiento es exactamente 0 → cero ruido de anchor (velocity tenía residuos).
- floatDist, cap 0.55, lockstep 5c, curvas laterales: intactos.

### Panel cerrado + bob desacoplado (fase 6 — fix de prueba manual)

Pruebas manuales: `tickRate` 1 o 5 se ven bien, quitar `bob` reducía la sensación de lag del label. Diagnóstico y cierre:

- **`general.tickRate` eliminado del panel** → `INTERNAL_SECONDARY_TICK_RATE = 5` (const privada). Solo gobernaba los checks de glow (los visuales ya iban a cada tick desde fase 5); 5 ticks = 0.25 s de latencia de activación de glow, invisible, menos trabajo que 1. Gson ignora `tickRate` en JSONs viejos.
- **`symbol.snapPosition` eliminado del panel y del schema** → siempre centrado sobre el waypoint. Enum `SymbolSnapPosition` borrado, rama `FRONT_OF_BEAM` (push hacia el jugador) borrada del snap branch. Menos una comparación y hasta un sqrt por tick en snap.
- **Bob desacoplado del pipeline de tracking** — la causa real de que "sin bob se sienta menos lag":
  1. `bobY` viajaba DENTRO de las posiciones trackeadas (`rawAnchor.y`, `snapPos`, `arrivedPos`). En snap/arrive/glide pasaba por `smoothLocation`: el delta del bob (~0.023 b/t máx) quedaba bajo el floor del perfil → amplitud damped al ~70 %/tick + fase retrasada; peor, el delta del bob inflaba la `distance` del smoothing → el alpha de convergencia XZ oscilaba con la fase del bob → chase lag/micro-glitch percibido.
  2. `calculateBob()` usaba wall clock (`System.currentTimeMillis`): con MSPT jitter la sinusoide se muestreaba a intervalos irregulares mientras el cliente interpola SIEMPRE exactamente 1 tick → velocidad vertical del bob irregular.
  - **Fix estructural**: todo el tracking/smoothing/estado (`lastVisualAnchor`, `lastLabelLocation`, `lastSymbolLocation`) opera en posiciones SIN bob; `bobY` se suma al Y final en el momento del packet (`teleportFakeDisplay(pos, yOffset)`, spawn incluido; dedupe compara el Y final para que un cambio de fase solo también emita). Fase por `state.tickSerial` (50 ms constantes por packet → pendiente uniforme de la sinusoide lineal a trozos que renderiza el cliente). El bob ahora es capa visual pura: amplitud íntegra, fase exacta, cero contaminación del movimiento horizontal, crisp también durante glides/handoffs.

### Cadencia interna del beam — decisión

El follow del beam a 20 Hz con duration 1 es la única combinación que mantiene beam y label sin desfase perceptible. `beamTickRate > 1` producía: teleports del beam cada N ticks interpolados sobre N mientras el label interpola sobre 1 → el beam siempre N−1 ticks detrás del texto en movimiento lateral. Eliminado como opción para que ningún manifest pueda reintroducir ese desfase.

## Remote visual compensation + vertical catch-up

### Compensación de latencia remota (REMOTE_EXTRA_TICKS)

En servidores con ping ≈75–80 ms la latencia visual acumulada es de ~3 ticks (1 tick server→packet + 1.5 ticks red + ~0.5 ticks interp_duration promedio). El look-ahead base `1 + 2×speed01` cubre ~1.84 ticks a sprint → déficit ≈ 1.16 ticks ≈ 0.32 bloques.

`REMOTE_EXTRA_TICKS = 1.5` se suma a `lookAheadTicks` → a sprint: ticks ≈ 3.34, `parAdvance` ≈ 0.80 (cap). Para ajustar al entorno: cambia únicamente la constante privada (no el panel). En localhost el label quedará ≤0.42 bloques por delante del jugador, que es menos perceptible que el mismo lag por detrás.

### smoothedHSpeed — predictor de velocidad horizontal

`state.smoothedHSpeed` reemplaza `sqrt(dispX²+dispZ²)` en el cálculo de `speed01`. Ataque rápido (instant ≥ smoothed → accept), liberación de 2 ticks (un tick de XZ bajo → ×0.85; dos consecutivos → reset). Evita que un solo tick de desplazamiento reducido (arco de salto, timing de paquete) colapse `parAdvance` → desaparece el "zoom in" del label al saltar.

### Fallback de eje Y para label/icon

Problema: `maxStepY = 0.16 b/t` cap anterior + alpha 0.18 → convergencia ~45 s para un delta Y=220 bloques.

Pipeline actualizado:

1. **Y look-ahead** — `vertLookAhead = (dispY × lookAheadTicks × 0.40).coerceIn(−0.60, 0.60)`. Suma al `rawBaseY` antes del suavizado. Peso 0.40 evita amplificar la oscilación del arco de salto. Cap ±0.60 bloques.
2. **Alpha escalado** — `baseAlphaY = lerp(0.30, 0.65, vertT)` (era 0.18–0.24); `maxStepY = lerp(0.25, 0.50, vertT)` (era 0.16–0.24). Responde ~2× más rápido sin cambiar la suavidad en reposo.
3. **Catch-up rápido** — `|dy| ≥ LABEL_VERTICAL_CATCHUP_THRESHOLD (3 bloques)`: alpha 0.65 interpolado hasta 0.95 según la magnitud. Converge 3 bloques en ~3 ticks.
4. **Snap instantáneo** — `|dy| ≥ LABEL_VERTICAL_SNAP_THRESHOLD (25 bloques)`: salta a `rawBaseY` directamente y establece `labelInterp = 0` para ese frame (teleport sin interpolación cliente), luego vuelve a 1.
5. **Bob no contamina** — toda la lógica mide `rawBaseY` sin bob; el `bobY` sigue sumándose solo en el packet.

`WaypointStats.labelVertCatchupCount` cuenta frames en catch-up o snap cuando debug activo.

### Beam — cobertura vertical cuando el jugador está por encima del target

Problema: `scaleY = pointerPos.y + height − beamBaseY` → cuando `playerY > pointerPos.y`, el beam no llega hasta el jugador.

Fix: `scaleY = dynTopY − dynBaseY` donde `dynTopY` sigue `max(pointerPos.y, playerY) + height`:
- Sube inmediatamente cuando `needTop > top` (cuantizado a pasos de BEAM_BASE_STEP=4 bloques).
- Baja con histéresis `BEAM_TOP_RAISE_HYSTERESIS = 8` bloques (simétrico a la base).
- `BEAM_VERTICAL_CATCHUP_THRESHOLD = 10` bloques: si el top almacenado se aleja más de 10 bloques del `needTop`, se fuerza actualización inmediata.
- `beam.dynTopY` en `ActiveBeam` (NaN inicial, reset junto con `dynBaseY`).
- `WaypointStats.beamTopForceCount` cuenta actualizaciones forzadas cuando debug activo.

El beam ahora siempre cubre el rango vertical entre jugador y waypoint, sea cual sea la dirección.

## BetterHUD Bridge — diseño interno (Paso 5)

`WaypointBetterHudBridgeDisplay` reemplaza el bridge V1 (solo objectives, index-based IDs) con soporte completo V2.

**Resolver compartido**: llama `resolveWaypointTargets(player, selection, maxTargets, entityTargets, includeObjectives, includeEntities)` — misma función package-level que zone trigger.

**`BetterHudTargetMode`**: `OBJECTIVES_ONLY` / `ENTITIES_ONLY` / `ANY_ACTIVE_TARGET`. Controla `includeObjectives` e `includeEntities` en la llamada al resolver.

**`pointIdFor(target)`**: `"${prefix}${entry.id}_${target.zoneKey()}"` sanitizado y truncado a 96 chars. Estable aunque cambie el orden de sort — no más flicker.

**`PlayerHudState(activeIds, knownPositions)`**: rastrea qué puntos están activos y su última posición `Triple<Double,Double,Double>`. Si el target se mueve (entidad Bukkit, NPC Typewriter en patrol), `posChanged = knownPositions[id] != newPos` → remove + re-add del punto → posición actualizada en BetterHUD. Cadencia interna `HUD_UPDATE_INTERVAL_TICKS = 5`.

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
