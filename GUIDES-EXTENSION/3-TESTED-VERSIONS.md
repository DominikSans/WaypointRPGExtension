# Tested Versions — WaypointRPGExtension

> **Versión:** `v0.7.0-dev`  
> **Descripción:** historial del entorno probado, resultados verificables y cambios mayores.  
> **Modificado:** lunes, 6 de julio de 2026, 13:03 `(-05:00)` (America/Lima).

## Reglas de registro

- Registrar día, fecha, hora y zona horaria.
- Separar pruebas `BUILD` y `MANUAL`.
- Solo declarar estable lo confirmado explícitamente por el usuario.
- Cada versión estable debe corresponder a un commit y push.
- Describir ampliamente únicamente cambios grandes, como V2.

## Estado actual

| Versión | Momento | Verificación | Estado |
|---|---|---|---|
| `v0.7.0-dev` | lunes, 6 de julio de 2026, 18:30 `-05:00` | `BUILD SUCCESSFUL` — remote visual compensation + vertical catch-up: `REMOTE_EXTRA_TICKS=1.5` suma 1.5 ticks al look-ahead XZ para cubrir ~75 ms de latencia de red (cap 0.80 b sigue como límite); Y look-ahead `dispY × lookAheadTicks × 0.40` (cap ±0.60) agrega compensación vertical sin amplificar el arco de salto; `baseAlphaY` subido 0.18→0.30–0.65, `maxStepY` 0.16→0.25–0.50 (respuesta 2× más rápida); catch-up `|dy| ≥ 3` bloques: alpha 0.65–0.95; snap `|dy| ≥ 25` bloques: pos directa + `labelInterp=0` (interp cliente instantáneo ese frame, luego vuelve a 1); beam `scaleY` corregido: `dynTopY = max(pointerY, playerY) + height` cuantizado, asciende inmediato, baja con histéresis 8 b; `BEAM_VERTICAL_CATCHUP_THRESHOLD=10` fuerza actualización si el top acumulado se aleja > 10 b; `ActiveBeam.dynTopY` (NaN inicial, reset junto con dynBaseY); `WaypointStats` ampliado con `labelVertCatchupCount` y `beamTopForceCount`. No tocado: FOV, BetterHUD, routes, CraftEngine BLOCK_STATE, glow, offsets visuales, bob, panel | Desarrollo |
| `v0.7.0-dev` | lunes, 6 de julio de 2026, 16:50 `-05:00` | `BUILD SUCCESSFUL` — smoothedHSpeed predictor (ataque-rápido / liberación 2 ticks): reemplaza `sqrt(dispX²+dispZ²)` instantáneo como input de `parAdvance`; un único tick de bajo desplazamiento XZ (arco de salto, timing de paquetes) ya no colapsa el look-ahead. Lógica: si `instant >= smoothed → smoothed = instant` (sin lag en aceleración); si `instant < 0.05` dos ticks seguidos → `smoothed = 0` (frenada real); un único tick bajo → `smoothed *= 0.85` (caída max 15%, mantiene por encima del umbral 0.08 objetivo); cambio de mundo → reset. Campo por jugador (`smoothedHSpeed`, `hSpeedZeroTicks`) en `PlayerWaypointState`. `lastParAdvance` por slot para medir drop de look-ahead. `WaypointStats` ampliado: `lookAheadDropCount` (drop > 0.08 en un tick), `teleportSkipWhileMoving` (lazyUpdate suprimió transform mientras jugador se movía); ambos en log de stats. Debug condicional por slot: snapshot en log cuando `anchorErrXZ > 0.05` o `parAdvanceDrop > 0.08`, y cuando `icon-sep > 0.001`. Limpieza de bloques de comentario largo (pre-regla) en `updatePlayerSync`, anchor IIR, entity glow y near-hide | Desarrollo |
| `v0.7.0-dev` | lunes, 6 de julio de 2026, 15:10 `-05:00` | `BUILD SUCCESSFUL` — fix lateral anchor lag: `smoothstep(0.02, 1.2, dLat)` → `smoothstep(0.01, 0.25, dLat)` en el suavizado IIR del anchor. Con el rango viejo (1.2), a sprint lateral (0.28 b/tick) `alphaLat ≈ 0.30` → error estacionario ≈ 0.65 bloques ("perseguía la posición anterior"). Con rango nuevo (0.25), `alphaLat = 1.0` desde 0.25 b/tick → sin lag lateral a sprint. Suelo 0.20 preservado para anti-shimmer sub-pixel. Cadencia real confirmada: label/symbol actualizan cada tick (no cada 5 — `general.tickRate` fue eliminado del panel en fase 5, el campo en JSON es ignorado); `fullUpdate`/glow cada 5 ticks es independiente del visual. `teleport_duration = TEXT_INTERP = 1` sin cambio. `lazyUpdate` trackea yaw/pitch correctamente. No se tocó: beam, CraftEngine, BLOCK_STATE, BetterHUD, ZoneTrigger, entities, routes | Desarrollo |
| `v0.7.0-dev` | lunes, 6 de julio de 2026, 14:30 `-05:00` | `BUILD SUCCESSFUL` — look-ahead cap subido de ±0.55 a ±0.80 bloques (`parAdvance`): a velocidades > 0.36 b/t ("speed 1.75") el cap anterior dejaba un déficit de lag de −0.05 a −0.20 bloques (latencia estructural ≈ 1.5×v vs. cap fijo). Con 0.80 la cobertura alcanza v≈0.53 b/t; sprint normal (v=0.28) sin cambio. Instrumentación nueva en WaypointStats (debug): anchor error mean/max por ejes parallel+lateral, contador frames>0.05 bloques, text-icon separation XZ mean/max. Órbita del beam confirmada resuelta por rotcheck.jsh: endpoint centering error 2.78e-17 (exacto), mid-interp PERIOD=1 max 0.104 mm (invisible vs. 10.3 mm con PERIOD=10). Sin cambios en rotación ni packet order | Desarrollo |
| `v0.7.0-dev` | lunes, 6 de julio de 2026, 13:03 `-05:00` | `BUILD SUCCESSFUL` — glyphs de `{direction}` migrados a snippets nativos de Typewriter (`waypoint.direction.*` en `plugins/Typewriter/snippets.yml`): loader YAML custom `direction-glyphs.yml` eliminado (era duplicación de `SnippetDatabaseImpl` — verificado en bytecode de `engine-paper-0.9.0`: cache en memoria, escritura de defaults al primer uso, refresh con `/tw reload`); 10 delegates `by snippet(...)` a nivel de archivo (requisito: `Snippet<T>` es `ReadOnlyProperty<Nothing?, T>`); `DirectionGlyphs.get` único punto de resolución, O(1) por tick, sin fields nuevos en panel; warning una sola vez si existe el yml legado; mismo patrón que `RoadNetworkExtension` oficial | Desarrollo |
| `v0.7.0-dev` | domingo, 5 de julio de 2026, 18:30 `-05:00` | `BUILD SUCCESSFUL` — fase 7, limpieza fuerte: `simple_tracked_waypoint` eliminado (archivo borrado, sin adaptador); warning `isVisualFire` resuelto (removida la rama deprecada, `fireTicks > 0` suficiente); `checkIntervalTicks` (zone trigger) y `updateIntervalTicks` (BetterHUD bridge) fuera del panel → `ZONE_CHECK_INTERVAL_TICKS = 5` y `HUD_UPDATE_INTERVAL_TICKS = 5` (consts privadas); `@Entry` actualizados con iconos y colores coherentes (naranja map-marker, rojo map-marker-radius, azul compass); `@Help` strings acortados en todas las config classes y entries; docs limpios de campos eliminados | Desarrollo |
| `v0.7.0-dev` | domingo, 5 de julio de 2026, 18:15 `-05:00` | `BUILD SUCCESSFUL` — fase 6, panel cerrado + bob desacoplado: `general.tickRate` y `symbol.snapPosition` fuera del panel/schema (internos: `INTERNAL_SECONDARY_TICK_RATE=5` para glow, snap siempre centrado, enum `SymbolSnapPosition` borrado); bob desacoplado del pipeline de tracking — antes viajaba dentro de anchor/snap/arrive y pasaba por `smoothLocation` (amplitud damped, fase retrasada, alpha XZ oscilando con la fase → chase lag percibido) y usaba wall clock (velocidad irregular con MSPT jitter); ahora fase por `tickSerial` y `bobY` sumado al Y final en el packet (`teleportFakeDisplay` con `yOffset`), tracking 100 % libre de bob | Desarrollo |
| `v0.7.0-dev` | domingo, 5 de julio de 2026, 17:40 `-05:00` | `BUILD SUCCESSFUL` — fase 5d, look-ahead con desplazamiento real (fix de prueba manual): `player.velocity` poco fiable para jugadores (movimiento client-authoritative; subestima sprint ~0.21 vs ~0.28 real y llega con retraso) → reemplazado por delta real de ojos por tick (`dispX/Y/Z`, cap ±1.0/eje, calculado antes de sobrescribir `lastPlayerLocation`); a sprint `speed01≈0.42 → lookAhead≈1.83 ticks → parAdvance≈0.51` cubre la latencia estructural de ~2 ticks (server→packet + ventana interp), `vertT` con `|dispY|`, quieto = 0 exacto (sin ruido residual), un alloc menos por slot/tick | Desarrollo |
| `v0.7.0-dev` | domingo, 5 de julio de 2026, 17:10 `-05:00` | `BUILD SUCCESSFUL` — fase 5c, etapa única de smoothing: `smoothLocation` passthrough para label/symbol en tracking normal (anchor + offset directo = lockstep exacto text/icon; antes floors 0.36 vs 0.46 → icon 27 % más rápido en A/D), anchor consolidado (paralelo floor 0.85→1.0 en 0.20 b/t — fin del hitch de arranque; lateral floor 0.20 = neto de las dos etapas viejas, shimmer igual), glide/snap/arrive/beam sin cambios | Desarrollo |
| `v0.7.0-dev` | domingo, 5 de julio de 2026, 16:45 `-05:00` | `BUILD SUCCESSFUL` — fase 5b, respuesta frontal del label (fix de prueba manual): look-ahead recalibrado a bloques/tick (`smoothstep(0.18,0.42)` — antes nunca se activaba al sprintar) con cap frontal 0.55 bloques, anisotropía frontal/lateral en anchor y `smoothLocation` LABEL/SYMBOL (paralelo casi directo, lateral con curvas idénticas); lag frontal estacionario calculado ~0.5 → ~0.04 bloques a sprint | Desarrollo |
| `v0.7.0-dev` | domingo, 5 de julio de 2026, 16:10 `-05:00` | `BUILD SUCCESSFUL` — fase 5 cadencia interna: `beamTickRate` fuera del panel (política interna `INTERNAL_BEAM_TICK_RATE=1`, beam+label+symbol en lockstep con duration 1), `fullUpdate` muerto re-empleado para glow (cada `tickRate` ticks), `fullBeamUpdate`/`beamTickCounter` eliminados, `dynamicHeight` cuantizado con histéresis 4/8 bloques (fin de los 4 packets/tick en movimiento vertical), fast path de 1 target en `resolveTargets`, Help de `general.tickRate` corregido | Desarrollo |
| `v0.7.0-dev` | domingo, 5 de julio de 2026, 12:55 `-05:00` | `BUILD SUCCESSFUL` — `beam.rotateLikeBeacon` (opcional, default false): capa interior gira 2.25°/tick como beacon vanilla, exterior estática; keyframes de quaternion cada 10 ticks con slerp client-side (2 metas/s por beam), `teleport_duration` intacto (cero lag posicional), rotación sobre el eje de columna (translation rotada), ángulo mod 720° anti-wrap, spawn/re-show con ángulo actual | Desarrollo |
| `v0.7.0-dev` | domingo, 5 de julio de 2026, 12:30 `-05:00` | `BUILD SUCCESSFUL` — clean animation fase 4 (post commit `c9b8163`): curva continua para `alphaXZ` del anchor (sin gear shifts en bordes de bracket), blend continuo del damping vertical (sin flip de régimen en saltos, `smoothstep(0.35,0.55)`), metadata parcial en text displays (texto solo → 1 entry, estilo solo → 5 entries, vs 15 completos; counter `partial=` en stats) | Desarrollo |
| `v0.7.0-dev` | domingo, 5 de julio de 2026, 11:35 `-05:00` | `BUILD SUCCESSFUL` — cierre de riesgos fase 3b: TTL 30 s para entidades ocultas (destroy real + re-show por spawn path), re-show teleport-first (label 3→2 packets, beam 6→4, sin meta extra), glide firme propio del beam (0.50/4.0/0.75 → 30 bloques en ~5 ticks), deadband 0.01 + histéresis de 3 resolves en cadencia adaptativa de objectives, pasada de lifecycle verificada sin retención | Desarrollo |
| `v0.7.0-dev` | domingo, 5 de julio de 2026, 10:59 `-05:00` | `BUILD SUCCESSFUL` — hardening fase 3: instrumentación `-Dwaypointrpg.debug=true` (packets/s, spawns/min, resolves/s, tamaños de mapas cada 10 s), beam hide/re-show sin destroy (fade + arrive), label hide en arrive, latch de `verticalColumnMode`, cadencia adaptativa de objectives (móvil → 1 tick), cap `HANDOFF_MAX_ALPHA=0.65` para handoffs cortos, scalarización de vectores en `updateSlot` (~10 allocs menos por slot/tick), `playerFeet` único por tick, hashes manuales sin boxing, memo de slot key, config de label parseada una vez, `symbolSnapped` en `clearSlotVisualState` | Desarrollo |
| `v0.7.0-dev` | sábado, 4 de julio de 2026, 23:55 `-05:00` | `BUILD SUCCESSFUL` — hardening fase 2: caches de objectives (5t) y route points (20t), cuantización thinFactor/symbolScale/opacity anti-resend, hash de inputs del label (sin replaces por tick), curvas de movimiento continuas por tipo, glide de handoff (route/selector/final), slots estables para selectores NAME/TAG, menos allocs Location/Vector | Desarrollo |
| `v0.7.0-dev` | sábado, 4 de julio de 2026, 23:31 `-05:00` | `BUILD SUCCESSFUL` (clean build) — hardening visual: key de slot estable para rutas (sin recreación al avanzar punto), `TEXT_INTERP=1`, flag `hidden` (sin spam de hide ni streak al reaparecer), histéresis arrive/hideRange/beamFade, dedupe de teleport, `globalRouteIndices` scoped por entry, epsilon BetterHUD | Desarrollo |
| `v0.7.0-dev` | sábado, 4 de julio de 2026, 21:04 `-05:00` | `BUILD SUCCESSFUL` — cache de resolución para `NAME`/`SCOREBOARD_TAG`, dedupe de metadata/teleports, fix de key estable para NPC Typewriter, limpieza BetterHUD sin texto por API | Desarrollo |
| `v0.6.0-dev` | jueves, 3 de julio de 2026 | `BUILD SUCCESSFUL` — Nuevo Paso 6 Routes V2 | Desarrollo |
| `v0.6-no-path-trail-clean` | jueves, 3 de julio de 2026 | Commit + tag post-limpieza path trail | Estable (BetterHUD bridge + sin trail) |
| `v0.5-betterhud-bridge-stable` | jueves, 3 de julio de 2026 | Commit + tag git estable pre-Routes V2 | Estable (BetterHUD bridge V2) |
| `v0.4-zone-trigger-stable` | jueves, 3 de julio de 2026 | Commit + tag git estable pre-Paso5 | Estable (zone trigger V2) |
| `v0.3-entity-targets-stable` | jueves, 3 de julio de 2026 | Commit + tag git estable pre-Paso4 | Estable (entity targets) |
| `v0.2-waypoint-v2-multitarget-stable` | jueves, 3 de julio de 2026 | Commit + tag git estable pre-Paso3 | Estable (pre entity targets) |
| `v0.1.0-dev` | viernes, 3 de julio de 2026, 12:22 `-05:00` | Documentación alineada con el source V2 | Archivo |
| build `1.0.0` | martes, 30 de junio de 2026; hora no registrada | `BUILD SUCCESSFUL` histórico | Build aprobado; pruebas manuales no certificadas |

## Cambio mayor — V2

Implementado alrededor del martes, 30 de junio de 2026. La hora exacta no quedó registrada.

V2 reemplazó el constructor plano por las secciones `general`, `target`, `label`, `symbol`, `beam`, `bob`, `routes`, `integrations` y `performance`.

Fue un breaking change para JSON V1. `simple_tracked_waypoint` quedó inicialmente como adaptador, pero fue eliminado definitivamente en fase 7 — no existe compatibilidad retroactiva. Además incorporó:

- materiales nativos para el beam;
- cadencia interna fija del beam, fuera del panel, para mantener follow suave y consistente;
- múltiples objectives simultáneos;
- separación lateral de labels y symbols;
- `snapPosition` configurable (retirado después en fase 6: siempre centrado sobre el waypoint, interno);
- smoothing lateral mejorado;
- IDs BetterHUD reforzados con el ID del entry.

## Entorno de referencia

| Componente | Versión |
|---|---|
| Paper/Minecraft objetivo | `1.21.1`, según documentación histórica |
| Java | `21` |
| Kotlin | `2.2.10` |
| Typewriter | `0.9.0-beta-173` |
| Typewriter module plugin | `2.1.0` |
| PacketEvents | `2.9.4` |
| BetterHUD compile API | `1.14.1` |

## Pruebas funcionales confirmadas

No existe todavía una versión estable certificada en este historial. Las pruebas antiguas estaban pendientes o fueron descritas informalmente; no se convierten retroactivamente en resultados aprobados.

Formato para próximos resultados:

| Versión | Fecha y hora | Tipo | Resultado | Commit |
|---|---|---|---|---|
| `v0.1.0` | `día, DD de mes de AAAA, HH:mm TZ` | `BUILD + MANUAL` | Función concreta probada | SHA |

## Compatibilidad pendiente de prueba

- Paper y Typewriter reales del servidor con manifests V2.
- PacketEvents junto con CraftEngine.
- BetterHUD runtime frente a la API de compilación `1.14.1`.
- Varias quests activas y cleanup de slots.
- Rutas reactivadas dentro de la misma sesión.
- Glow combinado con otros shared entity flags.
- ~~Manifests V1 mediante `simple_tracked_waypoint`~~ (entry eliminado en fase 7).
- Entity targets: `UUID` con entidad despawnada (deve devolver null limpiamente).
- Entity targets: `NAME` con múltiples entidades del mismo nombre (debe tomar la más cercana).
- Entity targets: `SCOREBOARD_TAG` con entidades en distinto mundo.
- Entity targets mezclados con objectives en `maxTargets=2`.
