# Tested Versions — WaypointRPGExtension

> **Versión:** `v0.7.0-dev`  
> **Descripción:** historial del entorno probado, resultados verificables y cambios mayores.  
> **Modificado:** domingo, 5 de julio de 2026, 11:35 `(-05:00)` (America/Lima).

## Reglas de registro

- Registrar día, fecha, hora y zona horaria.
- Separar pruebas `BUILD` y `MANUAL`.
- Solo declarar estable lo confirmado explícitamente por el usuario.
- Cada versión estable debe corresponder a un commit y push.
- Describir ampliamente únicamente cambios grandes, como V2.

## Estado actual

| Versión | Momento | Verificación | Estado |
|---|---|---|---|
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

Fue un breaking change para JSON V1 avanzados. `simple_tracked_waypoint` quedó como adaptador deprecado. Además incorporó:

- materiales nativos para el beam;
- frecuencia independiente mediante `beamTickRate`;
- múltiples objectives simultáneos;
- separación lateral de labels y symbols;
- `snapPosition` configurable;
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
- Manifests V1 mediante `simple_tracked_waypoint`.
- Entity targets: `UUID` con entidad despawnada (deve devolver null limpiamente).
- Entity targets: `NAME` con múltiples entidades del mismo nombre (debe tomar la más cercana).
- Entity targets: `SCOREBOARD_TAG` con entidades en distinto mundo.
- Entity targets mezclados con objectives en `maxTargets=2`.
