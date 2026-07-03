# Notes — Source Code

> **Versión:** `v0.1.0-dev`  
> **Descripción:** migración, problemas, soluciones, warnings y extractos del código.  
> **Modificado:** viernes, 3 de julio de 2026, 12:22 (America/Lima).

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
| `pathTrail`, `trailRange`, etc. | `trail.*` |
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

## Límites conocidos

- El fondo del label se suprime en modo `BOTH`.
- `label.opacity` también controla el symbol.
- `label.seeThrough` no tiene efecto actual.
- `entityTargets` queda fuera de `maxTargets`.
- Los índices de ruta pueden conservarse al reactivar un objective.
- La escala del symbol usa distancia 3D; snap usa distancia horizontal.
- El trail no es pathfinding y puede atravesar obstáculos.
- Zone trigger y BetterHUD usan targets directos, no puntos de ruta.
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
