# Routes V2 — Reference

> **Versión:** `v0.6.0-dev`  
> **Descripción:** Referencia completa del sistema de rutas manuales en `tracked_locatable_waypoint`, `waypoint_zone_trigger` y `waypoint_betterhud_bridge`.  
> **Modificado:** jueves, 3 de julio de 2026 (America/Lima).

## Qué hace

Una ruta manual guía al jugador paso a paso hacia un objective, mostrando un route point intermedio en lugar del objetivo final hasta que el jugador llegue a cada punto.

El índice de progreso se guarda en `globalRouteIndices` (compartido entre entries). Solo el visual entry (`tracked_locatable_waypoint`) avanza el índice. Zone trigger y BetterHUD leen el mismo índice para mantenerse sincronizados.

---

## Campos de `WaypointRoute`

| Campo | Tipo | Default | Descripción |
|---|---|---|---|
| `objectiveId` | `String` | `""` | ID del objective al que aplica esta ruta. Debe coincidir con el `id` del entry de objective en Typewriter. |
| `routeId` | `String` | `""` | ID de la ruta dentro del entry. Vacío = usar `objectiveId` como clave. Útil para sincronizar la misma ruta entre visual, zone trigger y BetterHUD. |
| `allowSkip` | `Boolean` | `true` | `true`: el jugador puede saltar puntos pasando por radio de puntos no consecutivos. `false`: avance estrictamente secuencial. |
| `resetOnObjectiveChange` | `Boolean` | `true` | Limpiar progreso cuando el objective desaparece. Al reactivarse, el jugador empieza desde el punto 0. |
| `resetOnComplete` | `Boolean` | `false` | Resetear a punto 0 al terminar el último route point. `false` = mostrar el objective final directo al completar. |
| `points` | `List<WaypointRoutePoint>` | `[]` | Lista de route points en orden. |

## Campos de `WaypointRoutePoint`

| Campo | Tipo | Default | Descripción |
|---|---|---|---|
| `name` | `Var<String>` | `""` | Nombre del punto. MiniMessage. Vacío = fallback a nombre del objective. Disponible como `{route_name}` en el label. |
| `position` | `Var<Position>` | Origin | Posición del punto en el mundo. |
| `radius` | `Double` | `3.0` | Radio de llegada en bloques. Player dentro → avanza al siguiente punto. |

---

## Placeholders en el label

| Placeholder | Valor |
|---|---|
| `{route_index}` | Número del punto activo (1-based). Vacío si no hay ruta activa. |
| `{route_total}` | Total de route points. Vacío si no hay ruta activa. |
| `{route_name}` | Nombre del route point activo (`point.name`). Vacío si no configurado o sin ruta. |
| `{route_remaining}` | Puntos restantes incluyendo el actual (`total - index + 1`). Vacío sin ruta. |

Ejemplo de label con todos los placeholders:
```json
"text": "<white>{route_name}</white>\n<gold>{distance}</gold> <gray>({route_index}/{route_total})</gray>"
```

---

## Estado compartido — globalRouteIndices

Clave: `"$playerUUID:$objectiveId:$effectiveRouteId"`

`effectiveRouteId = routeId.ifBlank { objectiveId }`

| Entry | Acceso |
|---|---|
| `TrackedLocatableWaypointDisplay` | Lee + escribe (avanza índice en `applyRoute`) |
| `WaypointZoneTriggerDisplay` | Solo lee (en `applyRouteReadOnly`) |
| `WaypointBetterHudBridgeDisplay` | Solo lee (en `applyRouteReadOnly`) |

Para que zone trigger y BetterHUD vean el mismo progreso que el visual entry, configura el mismo `routeId` en todos los entries que participen de la misma ruta.

---

## Ejemplo — ruta de 3 puntos

### Visual entry

```json
{
  "type": "tracked_locatable_waypoint",
  "id": "wp_herrero",
  "general": { "maxTargets": 1, "arriveRadius": 2.5 },
  "label": {
    "text": "<white>{route_name}</white>\n<gold>{distance}</gold> <gray>({route_index}/{route_total})</gray>",
    "useObjectiveName": false
  },
  "routes": [
    {
      "objectiveId": "obj_herrero",
      "routeId": "herrero_route",
      "allowSkip": false,
      "resetOnObjectiveChange": true,
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
}
```

### Zone trigger sincronizado

Mismo `routeId` → mismo índice en `globalRouteIndices`.

```json
{
  "type": "waypoint_zone_trigger",
  "id": "zt_herrero_route",
  "radius": 3.0,
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
        { "name": "", "position": { "world": "world", "x": 100, "y": 64, "z": 200 }, "radius": 3.0 },
        { "name": "", "position": { "world": "world", "x": 150, "y": 70, "z": 250 }, "radius": 3.0 },
        { "name": "", "position": { "world": "world", "x": 180, "y": 65, "z": 280 }, "radius": 2.5 }
      ]
    }
  ],
  "onEnter": "herrero_checkpoint_reached"
}
```

> **Nota:** El trigger dispara al llegar al route point activo. El índice lo avanza el visual entry. Si solo existe zone trigger sin visual, el índice nunca avanza (trigger siempre en el punto 0).

### BetterHUD sincronizado

BetterHUD apunta al route point activo en lugar del objective final.

```json
{
  "type": "waypoint_betterhud_bridge",
  "id": "bhud_herrero",
  "iconName": "quest",
  "targetMode": "OBJECTIVES_ONLY",
  "maxTargets": 1,
  "routes": [
    {
      "objectiveId": "obj_herrero",
      "routeId": "herrero_route",
      "points": [
        { "name": "", "position": { "world": "world", "x": 100, "y": 64, "z": 200 }, "radius": 3.0 },
        { "name": "", "position": { "world": "world", "x": 150, "y": 70, "z": 250 }, "radius": 3.0 },
        { "name": "", "position": { "world": "world", "x": 180, "y": 65, "z": 280 }, "radius": 2.5 }
      ]
    }
  ]
}
```

---

## Ejemplo — múltiples objectives con rutas distintas

```json
"routes": [
  {
    "objectiveId": "obj_herrero",
    "routeId": "ruta_herrero",
    "allowSkip": true,
    "points": [
      { "name": "Zona del herrero", "position": {...}, "radius": 3.0 }
    ]
  },
  {
    "objectiveId": "obj_mago",
    "routeId": "ruta_mago",
    "allowSkip": false,
    "resetOnComplete": true,
    "points": [
      { "name": "Cruce norte", "position": {...}, "radius": 4.0 },
      { "name": "Torre del mago", "position": {...}, "radius": 5.0 }
    ]
  }
]
```

Cada objective tiene su propio índice independiente en `globalRouteIndices`. Si ambos están activos simultáneamente, ambos avanzan de forma independiente.

---

## allowSkip — comportamiento

| allowSkip | Jugador entra directo al punto 2 (saltando punto 1) |
|---|---|
| `true` (default) | Avanza a punto 3. Punto 1 queda atrás. |
| `false` | Sin efecto. Sigue en punto 1. |

`allowSkip=false` es más robusto en rutas donde el orden importa (no permitir saltar checkpoints).

---

## resetOnObjectiveChange — comportamiento

Cuando el objective desaparece (`criteria` no cumplidos, quest desactivada):

| resetOnObjectiveChange | Qué pasa con el índice |
|---|---|
| `true` (default) | Índice eliminado de `globalRouteIndices`. Al reactivarse: empieza en punto 0. |
| `false` | Índice conservado. Al reactivarse: continúa desde donde dejó. |

---

## Entity targets — sin ruta

Las rutas solo aplican a objectives (`objective != null`). Los entity targets (UUID, NAME, SCOREBOARD_TAG, TYPEWRITER_NPC) ignoran `routes` y siempre apuntan a la posición live de la entidad.

---

## Casos de prueba (A–H)

| ID | Escenario | Resultado esperado |
|---|---|---|
| A | Route 3 puntos, `allowSkip=false` | Waypoint apunta punto 1. Al llegar → punto 2. Al llegar → punto 3. Al llegar → objective final. |
| B | `allowSkip=true`, jugador entra radio de punto 2 sin pasar por punto 1 | Índice avanza a 3 (skip point 1). |
| C | `allowSkip=false`, jugador entra radio de punto 2 sin punto 1 | Sin efecto. Sigue en punto 1. |
| D | Dos objectives activos con rutas distintas | Índices avanzan independientemente en `globalRouteIndices`. |
| E | Objective desaparece, `resetOnObjectiveChange=true` | Índice limpiado. Al reactivarse: punto 0. |
| F | Zone trigger `ACTIVE_ROUTE_POINT` con visual entry presente | Trigger dispara al llegar al route point activo, sincronizado con el visual. |
| G | BetterHUD con `routes` configurado | Punto BetterHUD sigue el route point activo (no el objective final). |
| H | Entity target en mismo entry | Entity target no aplica ruta. Apunta a entidad directamente. |
