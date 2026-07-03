# Examples — WaypointRPGExtension V2

> **Versión:** `v0.1.0-dev`  
> **Descripción:** referencia breve de campos y ejemplos de uso del esquema V2.  
> **Modificado:** viernes, 3 de julio de 2026, 12:22 (America/Lima).

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
| `text` | MiniMessage; `{name}`, `{distance}`, `{direction}`, `{index}`, `{total}`. |
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
| `trail` | `enabled`, `range`, `spacing`, `color`, `size`, `wave`, `waveWidth`, `waveCycles`, `waveSpeed`. Es línea directa, no pathfinding. |
| `routes` | `objectiveId` y `points` con `name`, `position`, `radius`. |
| `integrations` | `entityTargets`, `entityGlow`, `glowRange`. |
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

## Ejemplo — múltiples objectives

```json
{
  "type": "tracked_locatable_waypoint",
  "id": "wp_multi",
  "general": {
    "selection": "CLOSEST",
    "maxTargets": 3
  },
  "label": {
    "text": "<gold>{name}</gold> <gray>({index}/{total})</gray><newline><white>{distance}</white>",
    "multiOffset": 0.35
  }
}
```

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
  "trail": {
    "enabled": false,
    "range": 60.0,
    "spacing": 2.5,
    "color": "#00ff88",
    "size": 0.8,
    "wave": false,
    "waveWidth": 0.4,
    "waveCycles": 1.5,
    "waveSpeed": 1.0
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

