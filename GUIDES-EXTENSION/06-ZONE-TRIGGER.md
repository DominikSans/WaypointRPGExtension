# waypoint_zone_trigger — V2 Reference

> **Versión:** `v0.7.0-dev`  
> **Descripción:** Referencia completa del entry `waypoint_zone_trigger` V2.  
> **Modificado:** domingo, 5 de julio de 2026, 18:30 `(-05:00)` (America/Lima).

## Qué hace

Dispara triggers de Typewriter cuando un jugador **entra o sale** del radio de cualquier target activo del sistema V2: location objectives, entity targets Bukkit (UUID/NAME/SCOREBOARD_TAG) y Typewriter NPC (TYPEWRITER_NPC).

Se usa siempre como hijo de una audiencia de quest o criteria — igual que `tracked_locatable_waypoint`.

---

## Campos

| Campo | Tipo | Default | Descripción |
|---|---|---|---|
| `radius` | `Double` | `5.0` | Radio de detección en bloques. |
| `targetMode` | Enum | `ANY_ACTIVE_TARGET` | `OBJECTIVES_ONLY`: solo objectives de quest. `ANY_ACTIVE_TARGET`: objectives + entityTargets. `ACTIVE_ROUTE_POINT`: trigger en el punto de ruta actual. |
| `maxTargets` | `Int` | `5` | Máximo de targets considerados. `0` = desactivado. Debe coincidir con `general.maxTargets` del waypoint visual. |
| `selection` | Enum | `CLOSEST` | Orden para elegir los top-N targets: `CLOSEST` o `HIGHEST_PRIORITY`. |
| `triggerPerTarget` | `Bool` | `false` | `false` = un evento para ANY. `true` = evento por cada target key. |
| `triggerOnce` | `Bool` | `false` | `true` = onEnter solo dispara una vez por key hasta reset. |
| `resetOnExit` | `Bool` | `true` | `true` = salir resetea el guard de triggerOnce. |
| `entityTargets` | `List<EntityWaypointTarget>` | `[]` | Targets extra. Mismo formato que `integrations.entityTargets`. Solo activo con `ANY_ACTIVE_TARGET`. |
| `onEnter` | `Ref<TriggerableEntry>` | emptyRef | Trigger al entrar al radio de un target. |
| `onExit` | `Ref<TriggerableEntry>` | emptyRef | Trigger al salir o cuando el target desaparece. |

---

## Modos de target (`targetMode`)

### `OBJECTIVES_ONLY`
Solo monitorea los location objectives que el jugador tiene activos (`trackedShowingObjectives()`). Los campos `entityTargets` son ignorados. Más rápido.

### `ANY_ACTIVE_TARGET`
Combina objectives + entity targets de la lista `entityTargets`. Sujetos al mismo sort y `maxTargets`. Compatible con UUID, NAME, SCOREBOARD_TAG y TYPEWRITER_NPC.

---

## Semántica de triggerPerTarget

### `triggerPerTarget = false` (default)
- **onEnter** se dispara una vez cuando **cualquier** target entra al radio.
- **onExit** se dispara una vez cuando **todos** los targets salen del radio (o desaparecen).
- La key interna es la virtual `"§any"`.

### `triggerPerTarget = true`
- **onEnter** se dispara por cada target individual que entra al radio.
- **onExit** se dispara por cada target individual que sale o desaparece.
- La key es por UUID, `npc:<entryId>` o posición codificada.

---

## Semántica de triggerOnce + resetOnExit

| triggerOnce | resetOnExit | Comportamiento |
|---|---|---|
| `false` | any | Dispara onEnter/onExit cada vez que el jugador entra/sale. |
| `true` | `true` | onEnter solo una vez; resetea al salir → puede re-trigger. |
| `true` | `false` | onEnter solo una vez por sesión; nunca re-trigger. |

---

## Comportamiento en edge cases

| Situación | Resultado |
|---|---|
| El target desaparece mientras el jugador está dentro | onExit se dispara automáticamente en el siguiente tick. |
| El jugador es removido del audience estando dentro | onExit se dispara una vez (modo aggregate, no per-key). |
| Todos los targets desaparecen | onExit si había alguno "inside"; estado limpio para re-trigger. |
| `maxTargets = 0` | No se verifica nada — onEnter nunca dispara. |

---

## Ejemplos JSON

### Mínimo — solo objectives

```json
{
  "type": "waypoint_zone_trigger",
  "id": "zt_reach_cave",
  "radius": 6.0,
  "targetMode": "OBJECTIVES_ONLY",
  "maxTargets": 1,
  "onEnter": "trigger_arrived",
  "onExit": "trigger_left"
}
```

### Con TYPEWRITER_NPC + triggerOnce

```json
{
  "type": "waypoint_zone_trigger",
  "id": "zt_meet_oliver",
  "radius": 3.0,
  "targetMode": "ANY_ACTIVE_TARGET",
  "maxTargets": 1,
  "triggerOnce": true,
  "resetOnExit": true,
  "entityTargets": [
    {
      "targetType": "TYPEWRITER_NPC",
      "npcEntryId": "oliver_npc",
      "displayName": "",
      "priority": 0
    }
  ],
  "onEnter": "quest_oliver_met",
  "onExit": "quest_oliver_left"
}
```

### Multi-target per-target — checkpoints

```json
{
  "type": "waypoint_zone_trigger",
  "id": "zt_checkpoints",
  "radius": 5.0,
  "targetMode": "OBJECTIVES_ONLY",
  "maxTargets": 5,
  "selection": "CLOSEST",
  "triggerPerTarget": true,
  "triggerOnce": true,
  "resetOnExit": false,
  "onEnter": "checkpoint_collected",
  "onExit": ""
}
```

`resetOnExit=false` + `triggerOnce=true` = cada checkpoint solo se colecta una vez, permanentemente.

### Mix objectives + SCOREBOARD_TAG

```json
{
  "type": "waypoint_zone_trigger",
  "id": "zt_escort_zone",
  "radius": 8.0,
  "targetMode": "ANY_ACTIVE_TARGET",
  "maxTargets": 2,
  "selection": "HIGHEST_PRIORITY",
  "triggerPerTarget": false,
  "entityTargets": [
    {
      "targetType": "SCOREBOARD_TAG",
      "tag": "quest_escort",
      "displayName": "",
      "maxDistance": 200.0,
      "priority": 10
    }
  ],
  "onEnter": "escort_zone_entered",
  "onExit": "escort_zone_left"
}
```

---

## Notas de implementación

- `resolveZoneTargets` usa las mismas funciones package-level que `TrackedLocatableWaypointDisplay`: `resolveEntityTarget`, `resolveTypewriterNpcTarget`. No duplica lógica.
- Los zone keys son estables: UUID para Bukkit entities, `npc:<entryId>` para NPCs Typewriter, posición codificada para objectives.
- El tracking de estado usa `ConcurrentHashMap<UUID, PlayerZoneState>` thread-safe.
- `runSync` garantiza que los triggers se disparan en el hilo principal de Bukkit.
- El entry `waypoint_zone_trigger` no renderiza nada visual — es solo lógica de trigger.

---

## Casos de prueba recomendados (A–G)

| ID | Escenario | Resultado esperado |
|---|---|---|
| A | Jugador entra al radio de un objective, `triggerPerTarget=false` | onEnter dispara una vez |
| B | Jugador sale del radio, mismo modo | onExit dispara una vez |
| C | Jugador entra, `triggerOnce=true, resetOnExit=true`, sale y re-entra | onEnter en primer ingreso y en re-ingreso (reset funcionó) |
| D | Jugador entra, `triggerOnce=true, resetOnExit=false`, sale y re-entra | onEnter solo en primer ingreso |
| E | `triggerPerTarget=true`, dos objectives activos — jugador entra al radio de ambos | onEnter x2 (una por key) |
| F | Objective desaparece mientras jugador está dentro del radio | onExit disparado en siguiente tick |
| G | TYPEWRITER_NPC con NPC en patrol — jugador sigue al NPC y entra su radio | onEnter dispara; posición live tracking funciona |
