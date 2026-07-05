# Examples — WaypointRPGExtension V2

> **Versión:** `v0.7.0-dev`  
> **Descripción:** referencia breve de campos y ejemplos de uso del esquema V2, incluye entity targets (Paso 3), zone trigger V2 (Paso 4) y BetterHUD bridge V2 (Paso 5).  
> **Modificado:** sábado, 4 de julio de 2026, 21:04 `(-05:00)` (America/Lima).

Use `tracked_locatable_waypoint` como hijo de una audiencia. Como entry raíz se aplica a todos los jugadores.

## Fields

### `general`

| Field | Descripción |
|---|---|
| `mode` | `HOLOGRAM`, `BEAM` o `BOTH`. |
| `selection` | Orden `HIGHEST_PRIORITY` o `CLOSEST`. |
| `maxTargets` | Máximo de objectives visibles; `0` oculta todos. |
| `tickRate` | Intervalo general, en ticks. |
| `arriveRadius` | Distancia 3D para considerar alcanzado el target. |
| `hideOnArrive` | Oculta beam y label al llegar; conserva el symbol. |

### `target`

| Field | Descripción |
|---|---|
| `offset` | Offset Y: normalmente `2.0` para NPC y `0.0` para ubicación. |
| `verticalThreshold` | Diferencia Y que activa `▲`/`▼` y modo vertical. |

### `label`

| Field | Descripción |
|---|---|
| `text` | MiniMessage; `{name}`, `{distance}`, `{direction}`, `{index}`, `{total}`, `{target_type}`, `{entity_name}`, `{entity_type}`. |
| `useObjectiveName` | Usa el display del objective como `{name}`. |
| `height` | Altura sobre el anchor visual. |
| `floatDist` | Distancia máxima del label respecto al jugador. |
| `hideRange` | Distancia horizontal donde se oculta. |
| `fov` | Ángulo máximo; `0` desactiva el filtro. |
| `scale` | Escala del texto. |
| `billboard` | `CENTER`, `VERTICAL`, `HORIZONTAL`, `FIXED`. |
| `align` | `CENTER`, `LEFT`, `RIGHT`. |
| `background`, `bgColor` | Fondo y color ARGB; en `BOTH` el fondo se suprime. |
| `opacity`, `shadow` | Opacidad `0..255` y sombra. |
| `seeThrough` | Reservado; actualmente no tiene efecto. |
| `lineWidth` | Ancho antes del wrap. |
| `multiOffset` | Separación lateral entre targets simultáneos. |

### `symbol`

| Field | Descripción |
|---|---|
| `enabled`, `text` | Activa y define el icono MiniMessage/custom font. |
| `minScale`, `maxScale` | Escalas cercana y lejana. |
| `nearDist`, `farDist` | Rango de interpolación de escala. |
| `offset` | Offset Y en modo flotante. |
| `snapRange`, `snapLeave` | Entrada y salida del snap con histéresis. |
| `snapHeight` | Altura en snap/arrived. |
| `snapPosition` | `CENTER_ON_WAYPOINT` o `FRONT_OF_BEAM`. |

### `beam`

| Field | Descripción |
|---|---|
| `enabled`, `fullBright` | Activa el beam y brillo máximo. |
| `outer`, `inner` | Materiales de las dos capas. |
| `width`, `depth` | Dimensiones exteriores. |
| `coreWidth`, `coreDepth` | Dimensiones interiores. |
| `height`, `dynamicHeight` | Altura y extensión hacia el Y del jugador. |
| `staticRange`, `followRange`, `followDist` | Transición entre target fijo y seguimiento. |
| `fadeStart`, `fadeEnd` | Reducción al acercarse. |
| `beamTickRate` | Frecuencia independiente de movimiento. |

### Otras secciones

| Sección | Fields |
|---|---|
| `bob` | `enabled`, `height`, `speed`: oscilación vertical. |
| `routes` | `objectiveId`, `routeId`, `allowSkip`, `resetOnObjectiveChange`, `resetOnComplete`, `points` con `name`, `position`, `radius`. Placeholders: `{route_index}`, `{route_total}`, `{route_name}`, `{route_remaining}`. |
| `integrations` | `entityTargets` (ver abajo), `entityGlow`, `glowRange`. |
| `performance` | `lazyUpdate`, `cleanupOnJoin`, `cleanupRadius`. |

## Ejemplo — ubicación

```json
{
  "type": "tracked_locatable_waypoint",
  "id": "wp_location",
  "name": "Waypoint ubicación",
  "general": {
    "mode": "BOTH",
    "maxTargets": 1,
    "arriveRadius": 3.0
  },
  "target": {
    "offset": 0.0,
    "verticalThreshold": 10.0
  },
  "label": {
    "text": "<gold>{name}</gold><newline><white>{distance}</white>"
  }
}
```

## Ejemplo — NPC

```json
{
  "type": "tracked_locatable_waypoint",
  "id": "wp_npc",
  "general": {
    "mode": "BOTH",
    "maxTargets": 1,
    "arriveRadius": 1.5
  },
  "target": {
    "offset": 2.0
  },
  "label": {
    "text": "<gold>{direction} {name}</gold><newline><white>{distance}</white>"
  },
  "symbol": {
    "enabled": true,
    "snapHeight": 3.0
  }
}
```

## Ejemplo — múltiples objectives (detallado)

Muestra hasta 3 objectives activos simultáneamente, separados lateralmente, con conteo visible.

**Requisitos en el manifest:**
- `general.maxTargets` ≥ 2 (si es 1, solo se muestra el primero)
- `general.selection` define el orden: `CLOSEST` o `HIGHEST_PRIORITY`
- `label.multiOffset` > 0 para separación lateral (recomendado `0.30`–`0.45`)
- Cada objetivo activo necesita una `LocatableObjective` con posición válida

**Placeholders útiles:**
- `{index}` → número del waypoint actual (empieza en 1)
- `{total}` → total visible en este tick
- `{name}` → display name del objective
- `{distance}` → distancia formateada

```json
{
  "type": "tracked_locatable_waypoint",
  "id": "wp_multi_detallado",
  "name": "Multi Objectives",
  "general": {
    "mode": "BOTH",
    "selection": "CLOSEST",
    "maxTargets": 3,
    "tickRate": 5,
    "arriveRadius": 2.0,
    "hideOnArrive": false
  },
  "target": {
    "offset": 0.0,
    "verticalThreshold": 10.0
  },
  "label": {
    "text": "<gold>{name}</gold> <gray>({index}/{total})</gray><newline><white>{distance} {direction}</white>",
    "useObjectiveName": true,
    "height": 1.0,
    "floatDist": 5.0,
    "hideRange": 0.0,
    "fov": 0.0,
    "scale": 1.0,
    "billboard": "CENTER",
    "align": "CENTER",
    "background": true,
    "bgColor": "#80000000",
    "opacity": 255,
    "shadow": true,
    "lineWidth": 200,
    "multiOffset": 0.35
  },
  "symbol": {
    "enabled": true,
    "text": "<gold>◆</gold>",
    "minScale": 3.0,
    "maxScale": 5.0,
    "nearDist": 5.0,
    "farDist": 150.0,
    "offset": 0.5,
    "snapRange": 8.0,
    "snapLeave": 12.0,
    "snapHeight": 2.5,
    "snapPosition": "CENTER_ON_WAYPOINT"
  },
  "beam": {
    "enabled": true,
    "fullBright": true,
    "outer": "LIME_STAINED_GLASS",
    "inner": "LIME_CONCRETE",
    "width": 0.5,
    "coreWidth": 0.25,
    "depth": 0.5,
    "coreDepth": 0.25,
    "height": 150.0,
    "dynamicHeight": true,
    "staticRange": 30.0,
    "followRange": 60.0,
    "followDist": 55.0,
    "fadeStart": 10.0,
    "fadeEnd": 3.0,
    "beamTickRate": 1
  },
  "bob": { "enabled": true, "height": 0.06, "speed": 1.2 },
  "integrations": {
    "entityTargets": [],
    "entityGlow": false,
    "glowRange": 20.0
  },
  "performance": {
    "lazyUpdate": false,
    "cleanupOnJoin": false,
    "cleanupRadius": 50.0
  }
}
```

> **Nota:** si `maxTargets = 1` y hay 3 objectives activos, solo aparece el primero según `selection`. El `{total}` siempre refleja cuántos se están mostrando (no cuántos existen).

---

## Ejemplo — entity targets (detallado)

Rastrear entidades reales de Bukkit: mobs, villagers, armor stands, etc. Los entity targets **compiten con los objectives** en el mismo pool de `maxTargets`.

**¿Cuándo usar cada `targetType`?**

| targetType | Cuándo usarlo | Qué necesitas saber del mob |
|---|---|---|
| `UUID` | Entidad específica única (ej. boss spawneado por quest) | Su UUID exacto (`/data get entity @e[limit=1,...]`) |
| `NAME` | Entidad con nombre/customName conocido | El `customName` o nombre de tipo; busca en el mundo del jugador |
| `SCOREBOARD_TAG` | Cualquier entidad con tag asignado (más flexible) | El tag que le asignaste con `/tag <entidad> add <tag>` |

**Cómo obtener el UUID de una entidad en Minecraft:**
```
/data get entity @e[type=zombie,limit=1,sort=nearest]
```
Busca `UUID:` en el output. Formato: `[I;-1234567, 890123456, -789012345, 678901234]` → convierte a formato `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` con una herramienta online.

**Cómo asignar un scoreboard tag:**
```
/tag @e[type=zombie,limit=1,sort=nearest] add escort_mob
```
Verifica: `/tag @e[tag=escort_mob] list`

### Fields de cada entrada en `entityTargets`

| Field | Tipo | Default | Requerido para |
|---|---|---|---|
| `targetType` | `UUID` / `NAME` / `SCOREBOARD_TAG` | `UUID` | Todos |
| `uuid` | string (UUID) | `""` | `targetType=UUID` |
| `name` | string | `""` | `targetType=NAME` |
| `tag` | string | `""` | `targetType=SCOREBOARD_TAG` |
| `displayName` | MiniMessage | `""` | Opcional (vacío = nombre del mob) |
| `maxDistance` | double (bloques) | `128.0` | Limita búsqueda para `NAME` y `SCOREBOARD_TAG` |
| `priority` | int | `0` | Orden en `selection=HIGHEST_PRIORITY` |

### `entityGlow` y `glowRange`

- `entityGlow: true` aplica outline al mob (efecto glow client-side)
- `glowRange` es la distancia máxima para activar el glow
- Funciona solo para entity targets (no objectives)
- Si el mob desaparece, el glow se limpia automáticamente al limpiar el slot

### Ejemplo completo — escolta con tag

```json
{
  "type": "tracked_locatable_waypoint",
  "id": "wp_escolta",
  "name": "Seguir al NPC escolta",
  "general": {
    "mode": "BOTH",
    "selection": "CLOSEST",
    "maxTargets": 1,
    "tickRate": 3,
    "arriveRadius": 2.0,
    "hideOnArrive": false
  },
  "target": {
    "offset": 2.0,
    "verticalThreshold": 8.0
  },
  "label": {
    "text": "<aqua><bold>{name}</bold></aqua><newline><white>{distance}</white>",
    "height": 1.0,
    "floatDist": 5.0,
    "hideRange": 0.0,
    "fov": 0.0,
    "scale": 1.0,
    "billboard": "CENTER",
    "align": "CENTER",
    "background": true,
    "bgColor": "#80000000",
    "opacity": 255,
    "shadow": true,
    "lineWidth": 200,
    "multiOffset": 0.0
  },
  "symbol": {
    "enabled": true,
    "text": "<aqua>▶</aqua>",
    "minScale": 3.0,
    "maxScale": 5.0,
    "nearDist": 3.0,
    "farDist": 80.0,
    "offset": 0.5,
    "snapRange": 4.0,
    "snapLeave": 6.0,
    "snapHeight": 3.0,
    "snapPosition": "CENTER_ON_WAYPOINT"
  },
  "beam": {
    "enabled": true,
    "fullBright": true,
    "outer": "CYAN_STAINED_GLASS",
    "inner": "CYAN_CONCRETE",
    "width": 0.5,
    "coreWidth": 0.25,
    "depth": 0.5,
    "coreDepth": 0.25,
    "height": 120.0,
    "dynamicHeight": true,
    "staticRange": 20.0,
    "followRange": 50.0,
    "followDist": 45.0,
    "fadeStart": 8.0,
    "fadeEnd": 2.0,
    "beamTickRate": 1
  },
  "bob": { "enabled": true, "height": 0.05, "speed": 1.0 },
  "integrations": {
    "entityTargets": [
      {
        "targetType": "SCOREBOARD_TAG",
        "tag": "escort_npc",
        "displayName": "<aqua>Seguir al guía</aqua>",
        "maxDistance": 200.0,
        "priority": 0
      }
    ],
    "entityGlow": true,
    "glowRange": 25.0
  },
  "performance": {
    "lazyUpdate": false,
    "cleanupOnJoin": false,
    "cleanupRadius": 50.0
  }
}
```

### Ejemplo completo — boss por UUID

```json
{
  "type": "tracked_locatable_waypoint",
  "id": "wp_boss_uuid",
  "name": "Boss tracker",
  "general": {
    "mode": "BOTH",
    "selection": "CLOSEST",
    "maxTargets": 1,
    "tickRate": 2,
    "arriveRadius": 3.0,
    "hideOnArrive": false
  },
  "target": { "offset": 1.0, "verticalThreshold": 12.0 },
  "label": {
    "text": "<red><bold>{name}</bold></red> [{entity_type}]<newline><white>{distance} {direction}</white>",
    "height": 1.5,
    "floatDist": 5.0,
    "hideRange": 0.0,
    "fov": 0.0,
    "scale": 1.2,
    "billboard": "CENTER",
    "align": "CENTER",
    "background": true,
    "bgColor": "#C0000000",
    "opacity": 255,
    "shadow": true,
    "lineWidth": 200,
    "multiOffset": 0.0
  },
  "symbol": { "enabled": false },
  "beam": {
    "enabled": true,
    "fullBright": true,
    "outer": "RED_STAINED_GLASS",
    "inner": "RED_CONCRETE",
    "width": 0.6,
    "coreWidth": 0.3,
    "depth": 0.6,
    "coreDepth": 0.3,
    "height": 150.0,
    "dynamicHeight": true,
    "staticRange": 30.0,
    "followRange": 60.0,
    "followDist": 55.0,
    "fadeStart": 10.0,
    "fadeEnd": 3.0,
    "beamTickRate": 1
  },
  "bob": { "enabled": false },
  "integrations": {
    "entityTargets": [
      {
        "targetType": "UUID",
        "uuid": "REEMPLAZA-CON-UUID-REAL",
        "displayName": "<red>⚠ Boss Final</red>",
        "maxDistance": 0.0,
        "priority": 0
      }
    ],
    "entityGlow": true,
    "glowRange": 50.0
  },
  "performance": { "lazyUpdate": false, "cleanupOnJoin": false, "cleanupRadius": 50.0 }
}
```

> Para `targetType=UUID`, `maxDistance` no se usa — la búsqueda es global con `Bukkit.getEntity(uuid)`.  
> Si el boss está despawnado, el slot simplemente no aparece ese tick.

### Ejemplo — mix objectives + entity targets

Combina un objective de ubicación con un escort NPC en el mismo waypoint entry. `maxTargets=2` muestra ambos simultáneamente.

```json
{
  "type": "tracked_locatable_waypoint",
  "id": "wp_mix",
  "general": {
    "mode": "BOTH",
    "selection": "HIGHEST_PRIORITY",
    "maxTargets": 2,
    "tickRate": 4
  },
  "label": {
    "text": "<gold>{name}</gold> <gray>[{target_type}]</gray><newline>{distance}",
    "multiOffset": 0.35
  },
  "integrations": {
    "entityTargets": [
      {
        "targetType": "SCOREBOARD_TAG",
        "tag": "quest_escort",
        "displayName": "<green>Escolta</green>",
        "maxDistance": 150.0,
        "priority": 10
      }
    ],
    "entityGlow": true,
    "glowRange": 20.0
  }
}
```

En este ejemplo el escort (prioridad 10) aparece antes que un objective sin prioridad (0). `{target_type}` mostrará `"entity"` o `"objective"` en el label.

---

## Ejemplo — Typewriter NPC (NpcInstance)

Los NPCs creados con **NpcDefinition + NpcInstance** de EntityExtension son entidades **fake (packet-only)**. No aparecen en `world.entities` y no tienen UUID de Bukkit. El sistema los rastrea directamente mediante la API interna de Typewriter:

- **Posición live**: si el NPC tiene una actividad de movimiento (Patrol, Path, Target Location), el waypoint sigue su posición actual en tiempo real vía `SharedAudienceEntityDisplay.position(playerUUID)`
- **Fallback**: si el NPC no está siendo mostrado al jugador (fuera de audiencia), usa la `spawnLocation` estática del manifest
- **Sin glow**: los NPCs fake no son entidades reales — `entityGlow` no aplica

### ¿Cómo obtener el Entry ID del NPC?

**Opción A — Desde el panel web:**
1. Abre el editor web de Typewriter
2. Ve a la página donde tienes el NPC Instance
3. Haz click en el NPC Instance
4. En el inspector, copia el valor del campo **ID** (no "Name" — son distintos)

**Opción B — Desde el archivo JSON:**
Abre `plugins/Typewriter/pages/<tu-pagina>.json` y busca la entrada con `"type": "npc_instance"`:
```json
{
  "id": "oliver_npc",
  "type": "npc_instance",
  "name": "Oliver el Herrero",
  "definition": "oliver_def",
  "spawnLocation": { "world": "world", "x": 100.0, "y": 64.0, "z": 200.0 },
  "activity": "oliver_patrol"
}
```
El `npcEntryId` sería `"oliver_npc"`.

### Fields para TYPEWRITER_NPC

| Field | Obligatorio | Descripción |
|---|---|---|
| `targetType` | Sí | `"TYPEWRITER_NPC"` |
| `npcEntryId` | Sí | ID de la `npc_instance` en el manifest |
| `displayName` | No | Label MiniMessage. Vacío = nombre del entry |
| `priority` | No | Prioridad en `selection=HIGHEST_PRIORITY` |
| `uuid`, `name`, `tag`, `maxDistance` | — | Ignorados para este targetType |

### Ejemplo completo — NPC de quest

```json
{
  "type": "tracked_locatable_waypoint",
  "id": "wp_npc_oliver",
  "name": "Hablar con Oliver",
  "general": {
    "mode": "BOTH",
    "selection": "CLOSEST",
    "maxTargets": 1,
    "tickRate": 3,
    "arriveRadius": 2.0,
    "hideOnArrive": true
  },
  "target": {
    "offset": 2.0,
    "verticalThreshold": 8.0
  },
  "label": {
    "text": "<gold><bold>{name}</bold></gold><newline><white>{distance}</white>",
    "height": 1.0,
    "floatDist": 5.0,
    "hideRange": 0.0,
    "fov": 0.0,
    "scale": 1.0,
    "billboard": "CENTER",
    "align": "CENTER",
    "background": true,
    "bgColor": "#80000000",
    "opacity": 255,
    "shadow": true,
    "lineWidth": 200,
    "multiOffset": 0.0
  },
  "symbol": {
    "enabled": true,
    "text": "<yellow>!</yellow>",
    "minScale": 4.0,
    "maxScale": 6.0,
    "nearDist": 3.0,
    "farDist": 100.0,
    "offset": 0.5,
    "snapRange": 3.0,
    "snapLeave": 5.0,
    "snapHeight": 3.5,
    "snapPosition": "CENTER_ON_WAYPOINT"
  },
  "beam": {
    "enabled": true,
    "fullBright": true,
    "outer": "YELLOW_STAINED_GLASS",
    "inner": "YELLOW_CONCRETE",
    "width": 0.4,
    "coreWidth": 0.2,
    "depth": 0.4,
    "coreDepth": 0.2,
    "height": 120.0,
    "dynamicHeight": true,
    "staticRange": 25.0,
    "followRange": 50.0,
    "followDist": 45.0,
    "fadeStart": 8.0,
    "fadeEnd": 2.0,
    "beamTickRate": 1
  },
  "bob": { "enabled": true, "height": 0.06, "speed": 1.2 },
  "integrations": {
    "entityTargets": [
      {
        "targetType": "TYPEWRITER_NPC",
        "npcEntryId": "oliver_npc",
        "displayName": "<gold>Oliver el Herrero</gold>",
        "priority": 0
      }
    ],
    "entityGlow": false,
    "glowRange": 0.0
  },
  "performance": {
    "lazyUpdate": false,
    "cleanupOnJoin": false,
    "cleanupRadius": 50.0
  }
}
```

### NPC con Patrol/Path en movimiento

Si el NPC tiene una actividad de patrulla, el waypoint **sigue su posición actual** automáticamente. El sistema obtiene la posición live del `ActivityManager` del NPC. No requiere configuración adicional.

**Requisito:** EntityExtension debe estar instalada en el servidor. Si no está, el target devuelve `null` silenciosamente (sin crash, sin warning).

---

---

## waypoint_zone_trigger — V2

Dispara triggers al entrar/salir del radio de targets activos del sistema V2: location objectives, entity targets Bukkit, Typewriter NPCs.

### Fields

| Field | Tipo | Default | Descripción |
|---|---|---|---|
| `radius` | Double | `5.0` | Radio en bloques. |
| `checkIntervalTicks` | Int | `5` | Intervalo de verificación en ticks (1–100). |
| `targetMode` | Enum | `ANY_ACTIVE_TARGET` | `OBJECTIVES_ONLY` solo objectives; `ANY_ACTIVE_TARGET` objectives + entityTargets; `ACTIVE_ROUTE_POINT` trigger en el route point activo (requiere `routes`). |
| `maxTargets` | Int | `5` | Máximo de targets considerados (0 = desactivado). Debe coincidir con `general.maxTargets` del waypoint entry. |
| `selection` | Enum | `CLOSEST` | Orden antes de aplicar maxTargets: `CLOSEST` o `HIGHEST_PRIORITY`. |
| `triggerPerTarget` | Bool | `false` | `false`: un solo onEnter/onExit si ANY target cambia. `true`: onEnter/onExit por cada target key individual. |
| `triggerOnce` | Bool | `false` | `true`: onEnter solo se dispara una vez por key hasta que el jugador salga y resetOnExit sea true. |
| `resetOnExit` | Bool | `true` | `true`: al salir resetea el guard de triggerOnce, permitiendo re-trigger. |
| `entityTargets` | List | `[]` | Lista de entity/NPC targets extra (mismo formato que `integrations.entityTargets`). Solo aplica con `ANY_ACTIVE_TARGET`. |
| `routes` | List | `[]` | Definición de rutas para `ACTIVE_ROUTE_POINT`. Mismo formato que `routes` del visual entry. Comparte índice de ruta vía `globalRouteIndices`. |
| `onEnter` | Ref | emptyRef | Trigger al entrar en el radio de un target. |
| `onExit` | Ref | emptyRef | Trigger al salir o cuando el target desaparece. |

### Semántica de triggerPerTarget

| triggerPerTarget | Resultado |
|---|---|
| `false` | Un solo onEnter cuando **algún** target entra. Un onExit cuando **todos** salen o desaparecen. |
| `true` | onEnter/onExit **por cada target individual** (key = UUID, NPC entry ID, o posición). |

`triggerOnce=true` + `resetOnExit=true`: el jugador puede re-entrar y volver a activar onEnter después de salir.  
`triggerOnce=true` + `resetOnExit=false`: onEnter se activa solo la primera vez (permanente por sesión).

### Ejemplo mínimo — objectives only

```json
{
  "type": "waypoint_zone_trigger",
  "id": "zt_cave_enter",
  "radius": 6.0,
  "checkIntervalTicks": 5,
  "targetMode": "OBJECTIVES_ONLY",
  "maxTargets": 1,
  "onEnter": "my_trigger_id",
  "onExit": "my_exit_trigger_id"
}
```

### Ejemplo — entity target + triggerOnce

Dispara `quest_npc_arrived` la primera vez que el jugador llega al radio del NPC Oliver, y `quest_npc_left` al alejarse. No vuelve a disparar `quest_npc_arrived` hasta que el jugador salga (resetOnExit default = true).

```json
{
  "type": "waypoint_zone_trigger",
  "id": "zt_oliver",
  "radius": 3.0,
  "checkIntervalTicks": 5,
  "targetMode": "ANY_ACTIVE_TARGET",
  "maxTargets": 1,
  "selection": "CLOSEST",
  "triggerOnce": true,
  "resetOnExit": true,
  "entityTargets": [
    {
      "targetType": "TYPEWRITER_NPC",
      "npcEntryId": "oliver_npc",
      "displayName": "<gold>Oliver</gold>",
      "priority": 0
    }
  ],
  "onEnter": "quest_npc_arrived",
  "onExit": "quest_npc_left"
}
```

### Ejemplo — multi-target per-target mode

`triggerPerTarget=true` dispara eventos distintos al entrar/salir de cada target. Útil para colección de items o puntos de control múltiples.

```json
{
  "type": "waypoint_zone_trigger",
  "id": "zt_checkpoints",
  "radius": 5.0,
  "checkIntervalTicks": 4,
  "targetMode": "OBJECTIVES_ONLY",
  "maxTargets": 5,
  "selection": "CLOSEST",
  "triggerPerTarget": true,
  "onEnter": "checkpoint_reached",
  "onExit": "checkpoint_left"
}
```

Con `maxTargets=5` monitorea hasta 5 objectives simultáneos. Cada uno genera su propio evento al entrar/salir.

### Ejemplo — ACTIVE_ROUTE_POINT

Dispara trigger al llegar al route point activo. El índice es el mismo que mueve el visual entry (comparten `globalRouteIndices`). Configura `routes` con los mismos datos que el visual entry.

```json
{
  "type": "waypoint_zone_trigger",
  "id": "zt_herrero_route",
  "radius": 3.0,
  "checkIntervalTicks": 5,
  "targetMode": "ACTIVE_ROUTE_POINT",
  "maxTargets": 1,
  "triggerOnce": true,
  "resetOnExit": false,
  "routes": [
    {
      "objectiveId": "obj_herrero",
      "routeId": "herrero_route",
      "allowSkip": false,
      "points": [
        { "name": "Cruza el puente", "position": { "world": "world", "x": 100, "y": 64, "z": 200 }, "radius": 3.0 },
        { "name": "Sube las escaleras", "position": { "world": "world", "x": 150, "y": 70, "z": 250 }, "radius": 3.0 }
      ]
    }
  ],
  "onEnter": "herrero_checkpoint_reached"
}
```

---

## routes V2 — ruta manual de 3 puntos

```json
"routes": [
  {
    "objectiveId": "obj_herrero",
    "routeId": "herrero_route",
    "allowSkip": false,
    "resetOnObjectiveChange": true,
    "resetOnComplete": false,
    "points": [
      {
        "name": "<yellow>Cruza el puente</yellow>",
        "position": { "world": "world", "x": 100, "y": 64, "z": 200 },
        "radius": 3.0
      },
      {
        "name": "<yellow>Sube las escaleras</yellow>",
        "position": { "world": "world", "x": 150, "y": 70, "z": 250 },
        "radius": 3.0
      },
      {
        "name": "<yellow>Habla con el herrero</yellow>",
        "position": { "world": "world", "x": 180, "y": 65, "z": 280 },
        "radius": 2.5
      }
    ]
  }
]
```

Label con placeholders de ruta:
```json
"text": "<white>{route_name}</white>\n<gold>{distance}</gold> <gray>({route_index}/{route_total})</gray>"
```

---

## waypoint_betterhud_bridge — V2

Sincroniza targets activos V2 con puntos de brújula de BetterHUD. Soporta objectives, entity targets y Typewriter NPCs.

### Fields

| Field | Tipo | Default | Descripción |
|---|---|---|---|
| `iconName` | String | `"default"` | Nombre del elemento de brújula en el layout BetterHUD. |
| `pointNamePrefix` | String | `"waypoint_"` | Prefijo de los IDs de punto. `entry.id` se agrega automáticamente. |
| `updateIntervalTicks` | Int | `10` | Frecuencia de sync en ticks. 5–10 para entidades en movimiento. |
| `targetMode` | Enum | `ANY_ACTIVE_TARGET` | `OBJECTIVES_ONLY`, `ENTITIES_ONLY` o `ANY_ACTIVE_TARGET`. |
| `maxTargets` | Int | `5` | Máximo de puntos enviados simultáneamente a BetterHUD. |
| `selection` | Enum | `CLOSEST` | `CLOSEST` o `HIGHEST_PRIORITY`. |
| `arriveRadius` | Double | `0.0` | Radio en bloques para ocultar el punto al llegar. 0 = nunca ocultar. |
| `entityTargets` | List | `[]` | Entities/NPCs adicionales. Mismo formato que `integrations.entityTargets`. |

### Ejemplo mínimo — 1 objective

```json
{
  "type": "waypoint_betterhud_bridge",
  "id": "bhud_main",
  "iconName": "quest",
  "updateIntervalTicks": 10,
  "targetMode": "OBJECTIVES_ONLY",
  "maxTargets": 1
}
```

### Ejemplo — múltiples objectives + HIGHEST_PRIORITY

```json
{
  "type": "waypoint_betterhud_bridge",
  "id": "bhud_quests",
  "iconName": "quest",
  "updateIntervalTicks": 10,
  "targetMode": "OBJECTIVES_ONLY",
  "maxTargets": 3,
  "selection": "HIGHEST_PRIORITY"
}
```

### Ejemplo — entity target SCOREBOARD_TAG (mob en movimiento)

`updateIntervalTicks: 5` para que la posición del mob se actualice frecuentemente en el HUD.

```json
{
  "type": "waypoint_betterhud_bridge",
  "id": "bhud_escort",
  "iconName": "escort",
  "updateIntervalTicks": 5,
  "targetMode": "ENTITIES_ONLY",
  "maxTargets": 1,
  "entityTargets": [
    {
      "targetType": "SCOREBOARD_TAG",
      "tag": "quest_escort",
      "displayName": "",
      "maxDistance": 300.0,
      "priority": 0
    }
  ]
}
```

### Ejemplo — Typewriter NPC + hideOnArrive

Oculta el punto de brújula cuando el jugador llega a menos de 3 bloques del NPC.

```json
{
  "type": "waypoint_betterhud_bridge",
  "id": "bhud_oliver",
  "iconName": "npc",
  "updateIntervalTicks": 5,
  "targetMode": "ENTITIES_ONLY",
  "maxTargets": 1,
  "arriveRadius": 3.0,
  "entityTargets": [
    {
      "targetType": "TYPEWRITER_NPC",
      "npcEntryId": "oliver_npc",
      "displayName": "",
      "priority": 0
    }
  ]
}
```

### Notas importantes

- **BetterHUD ausente**: si BetterHUD no está instalado, el bridge se desactiva silenciosamente con un solo warning en consola (`[WaypointRPG] BetterHUD not found. waypoint_betterhud_bridge is disabled.`).
- **Texto en HUD**: se configura directamente en el layout de BetterHUD; la API del bridge no admite texto dinámico por punto.
- **Dos bridges**: si se crean dos entries de `waypoint_betterhud_bridge`, usan `entry.id` distinto en el `pointId` — sin colisión.
- **IDs estables**: el `pointId` usa `zoneKey()` (UUID/sourceId/posición) en lugar de index. El orden puede cambiar sin causar flicker o puntos duplicados.

---

## ¿Necesito actualizar el jar del servidor?

**Sí, siempre que agregues o cambies campos o comportamiento.**

El jar compilado en `build/libs/WaypointRPGExtension-1.0.0.jar` contiene el schema KSP generado y el código de runtime. El servidor Typewriter carga el jar al iniciar y registra los entry types con sus campos.

**Flujo obligatorio para que los nuevos campos funcionen:**

1. Compilar: `./gradlew build`
2. Copiar `build/libs/WaypointRPGExtension-1.0.0.jar` a `plugins/Typewriter/extensions/`
3. Reiniciar el servidor (o `/tw reload` si Typewriter lo soporta para extensions)
4. Typewriter regenera el schema — los nuevos campos (`targetType`, `uuid`, `name`, `tag`, etc.) aparecen en el editor

**Sin actualizar el jar:** el editor no conoce los nuevos campos y los ignora o los borra al guardar. El runtime del servidor tampoco tiene el nuevo código de resolución.

## Ejemplo JSON completo

```json
{
  "type": "tracked_locatable_waypoint",
  "id": "wp_v2_complete",
  "name": "Waypoint V2",
  "general": {
    "mode": "BOTH",
    "selection": "CLOSEST",
    "maxTargets": 5,
    "tickRate": 5,
    "arriveRadius": 1.5,
    "hideOnArrive": true
  },
  "target": {
    "offset": 0.0,
    "verticalThreshold": 10.0
  },
  "label": {
    "text": "<gold>{name}</gold><newline><white>{distance}</white><newline><green>{direction}</green>",
    "useObjectiveName": true,
    "height": 1.0,
    "floatDist": 5.0,
    "hideRange": 8.0,
    "fov": 55.0,
    "scale": 1.0,
    "billboard": "CENTER",
    "align": "CENTER",
    "background": true,
    "bgColor": "#80000000",
    "opacity": 255,
    "shadow": true,
    "seeThrough": false,
    "lineWidth": 255,
    "multiOffset": 0.35
  },
  "symbol": {
    "enabled": true,
    "text": "<gold>◆</gold>",
    "minScale": 3.0,
    "maxScale": 5.0,
    "nearDist": 5.0,
    "farDist": 150.0,
    "offset": 0.5,
    "snapRange": 8.0,
    "snapLeave": 12.0,
    "snapHeight": 3.0,
    "snapPosition": "CENTER_ON_WAYPOINT"
  },
  "beam": {
    "enabled": true,
    "fullBright": true,
    "outer": "LIME_STAINED_GLASS",
    "inner": "LIME_CONCRETE",
    "width": 0.5,
    "coreWidth": 0.25,
    "depth": 0.5,
    "coreDepth": 0.25,
    "height": 150.0,
    "dynamicHeight": true,
    "staticRange": 30.0,
    "followRange": 60.0,
    "followDist": 55.0,
    "fadeStart": 10.0,
    "fadeEnd": 3.0,
    "beamTickRate": 1
  },
  "bob": {
    "enabled": true,
    "height": 0.06,
    "speed": 1.2
  },
  "routes": [],
  "integrations": {
    "entityTargets": [],
    "entityGlow": false,
    "glowRange": 20.0
  },
  "performance": {
    "lazyUpdate": false,
    "cleanupOnJoin": false,
    "cleanupRadius": 50.0
  }
}
```

