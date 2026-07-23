# waypoint_betterhud_bridge — V2 Reference

> **Versión:** `v0.7.0-dev`  
> **Descripción:** Referencia completa del entry `waypoint_betterhud_bridge` V2.  
> **Modificado:** domingo, 5 de julio de 2026, 18:30 `(-05:00)` (America/Lima).

## Qué hace

Sincroniza los targets activos del sistema V2 con puntos de brújula de **BetterHUD** en tiempo real. Soporta:

- Location objectives de quest
- Entity targets Bukkit (UUID, NAME, SCOREBOARD_TAG)
- Typewriter NPC targets (posición live o fallback spawnLocation)
- Múltiples targets simultáneos con IDs únicos y estables
- Actualización de posición para entidades en movimiento
- Limpieza automática al salir del audience o dispose

Si BetterHUD no está instalado, el bridge se desactiva silenciosamente con **un solo warning** en consola.

---

## Campos

| Campo | Tipo | Default | Descripción |
|---|---|---|---|
| `iconName` | `String` | `"default"` | Nombre del elemento de brújula en el layout BetterHUD. |
| `pointNamePrefix` | `String` | `"waypoint_"` | Prefijo de los IDs de punto. `entry.id` se agrega automáticamente. |
| `targetMode` | Enum | `ANY_ACTIVE_TARGET` | Qué targets incluir (ver sección abajo). |
| `maxTargets` | `Int` | `5` | Máximo de puntos enviados simultáneamente (0 = desactivado). |
| `selection` | Enum | `CLOSEST` | Orden antes de `maxTargets`: `CLOSEST` o `HIGHEST_PRIORITY`. |
| `arriveRadius` | `Double` | `0.0` | Oculta el punto cuando `distance <= arriveRadius`. 0 = nunca ocultar. |
| `entityTargets` | `List<EntityWaypointTarget>` | `[]` | Entities/NPCs extra. Requiere `targetMode = ANY_ACTIVE_TARGET` o `ENTITIES_ONLY`. |
El texto visible se configura en el layout de BetterHUD. La API `PointedLocation` solo acepta ID, icono y ubicación, por lo que el entry no expone campos de texto sin efecto.

---

## targetMode

| Valor | Comportamiento |
|---|---|
| `OBJECTIVES_ONLY` | Solo location objectives activos del jugador (`trackedShowingObjectives`). Más ligero. |
| `ENTITIES_ONLY` | Solo `entityTargets` list (entidades Bukkit + Typewriter NPCs). Objectives ignorados. |
| `ANY_ACTIVE_TARGET` | Objectives + `entityTargets` combinados, sujetos al mismo sort y `maxTargets`. |

---

## IDs de punto (pointId)

Formato: `${pointNamePrefix}${entry.id}_${target.zoneKey()}`

- `entry.id` garantiza que dos bridge entries no colisionan.
- `zoneKey()` = UUID para Bukkit entities, `npc:<entryId>` para NPCs Typewriter, posición codificada para objectives.
- Estable aunque cambie el orden de sort → sin flicker ni puntos duplicados.
- Caracteres inválidos sanitizados a `_`, truncado a 96 caracteres.

---

## Actualización de posición (entidades móviles)

El bridge detecta si la posición del target cambió desde el último sync:
- Si `knownPositions[id] != newPos` → remove + re-add del punto con nueva coordenada.
- La cadencia de sync es interna (5 ticks / 0.25 s): suficientemente frecuente para entidades en movimiento sin overhead por tick.

---

## Cleanup

| Evento | Resultado |
|---|---|
| Target desaparece (objective desactivado, entidad muere) | Punto eliminado en el siguiente tick. |
| Player sale del audience | Todos los puntos de ese player eliminados (`onPlayerRemove`). |
| `dispose()` | Todos los puntos de todos los players eliminados. |
| `arriveRadius` superado | Punto eliminado; reaparece si player se aleja. |

---

## BetterHUD no instalado

Al primer intento de sync, el bridge verifica `Bukkit.getPluginManager().getPlugin("BetterHud")`. Si no existe o no está activo:

```
[WaypointRPG] BetterHUD not found. waypoint_betterhud_bridge is disabled.
```

El warning aparece **una sola vez** por sesión de servidor. El bridge no hace nada más (no crash, no spam).

---

## Compatibilidad con múltiples bridge entries

Dos entries de `waypoint_betterhud_bridge` con distintos `id` y distintos `iconName` pueden coexistir sin colisión de `pointId`. Ejemplo:

- Entry `bhud_main` → `iconName: "quest"` → puntos: `waypoint_bhud_main_...`
- Entry `bhud_escort` → `iconName: "escort"` → puntos: `waypoint_bhud_escort_...`

---

## Ejemplos JSON

### Un objective

```json
{
  "type": "waypoint_betterhud_bridge",
  "id": "bhud_main",
  "iconName": "quest",
  "targetMode": "OBJECTIVES_ONLY",
  "maxTargets": 1
}
```

### Múltiples objectives por prioridad

```json
{
  "type": "waypoint_betterhud_bridge",
  "id": "bhud_quests",
  "iconName": "quest",
  "targetMode": "OBJECTIVES_ONLY",
  "maxTargets": 3,
  "selection": "HIGHEST_PRIORITY"
}
```

### Escort mob (SCOREBOARD_TAG, movimiento frecuente)

```json
{
  "type": "waypoint_betterhud_bridge",
  "id": "bhud_escort",
  "iconName": "escort",
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

### Typewriter NPC + ocultar al llegar

```json
{
  "type": "waypoint_betterhud_bridge",
  "id": "bhud_oliver",
  "iconName": "npc",
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

### Mix objectives + NPC (ANY_ACTIVE_TARGET)

```json
{
  "type": "waypoint_betterhud_bridge",
  "id": "bhud_full",
  "iconName": "quest",
  "targetMode": "ANY_ACTIVE_TARGET",
  "maxTargets": 3,
  "selection": "CLOSEST",
  "entityTargets": [
    {
      "targetType": "TYPEWRITER_NPC",
      "npcEntryId": "oliver_npc",
      "displayName": "",
      "priority": 5
    }
  ]
}
```

---

## Casos de prueba recomendados (A–G)

| ID | Escenario | Resultado esperado |
|---|---|---|
| A | Servidor sin BetterHUD instalado | Warning único en consola; cero errores; bridge inactivo |
| B | 1 location objective activo | Aparece 1 punto en brújula; se actualiza distancia al mover jugador |
| C | Jugador desactiva objective | Punto eliminado en siguiente tick |
| D | `maxTargets=3`, 5 objectives activos | Solo 3 puntos en HUD; cambiar orden no duplica ni elimina mal |
| E | SCOREBOARD_TAG mob en movimiento | Punto sigue al mob; seguimiento fluido con cadencia interna de 5 ticks |
| F | Mob con tag muere | Punto eliminado en siguiente tick |
| G | TYPEWRITER_NPC en Patrol | Punto sigue posición live del NPC; fallback spawnLocation si no spawneado |
| H | Jugador pierde audience | Todos los puntos del bridge eliminados |
| I | Dos bridge entries distintos | Sin colisión de pointId; ambos funcionan independientemente |
