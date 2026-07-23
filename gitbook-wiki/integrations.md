# Integrations

This page covers the entries that connect waypoints with other systems.

## BetterHUD

Use `waypoint_betterhud_bridge` when you want the active waypoint to appear in a BetterHUD compass.

### Basic setup

1. Install BetterHUD.
2. Create or choose a compass element in BetterHUD.
3. Create a `waypoint_betterhud_bridge` entry in Typewriter.
4. Set `iconName` to the BetterHUD element name.

### Example

```json
{
  "type": "waypoint_betterhud_bridge",
  "id": "quest_compass_bridge",
  "name": "Quest Compass Bridge",
  "iconName": "quest_waypoint",
  "pointNamePrefix": "waypoint_",
  "targetMode": "ANY_ACTIVE_TARGET",
  "maxTargets": 3,
  "selection": "CLOSEST",
  "arriveRadius": 2.0
}
```

### Notes for configurators

- BetterHUD is optional.
- If BetterHUD is not installed, the bridge does not run.
- Use this integration for off-screen direction instead of pinning labels to the screen.
- The update interval is handled internally.

## WorldGuard-style zones

WaypointRPGExtension does not require WorldGuard, but `waypoint_zone_trigger` is useful for the same kind of gameplay: detecting when a player enters or leaves an area.

Use it when you want to run Typewriter actions around a waypoint target, route point, NPC, or entity.

### Zone Trigger

Entry:

```txt
waypoint_zone_trigger
```

Common uses:

- Start dialogue near an objective.
- Run a cutscene when a player reaches a route point.
- Start combat when entering a marked area.
- Fire an exit trigger when the player leaves.

### Basic example

```json
{
  "type": "waypoint_zone_trigger",
  "id": "quest_zone_gate",
  "name": "Gate Zone",
  "radius": 4.0,
  "targetMode": "ACTIVE_ROUTE_POINT",
  "maxTargets": 1,
  "selection": "CLOSEST",
  "triggerPerTarget": false,
  "triggerOnce": true,
  "resetOnExit": true,
  "routes": [
    {
      "objectiveId": "main_objective",
      "routeId": "main_path",
      "points": [
        {
          "name": "Gate",
          "position": "gate_position",
          "radius": 3.0
        }
      ]
    }
  ],
  "onEnter": "start_gate_dialogue",
  "onExit": "leave_gate_area"
}
```

### Target modes

| Mode | Use it when |
|---|---|
| `OBJECTIVES_ONLY` | Only objective locations should be checked. |
| `ANY_ACTIVE_TARGET` | Objectives and entity targets should be checked. |
| `ACTIVE_ROUTE_POINT` | The zone should follow the current route point. |

### Trigger behavior

| Field | Result |
|---|---|
| `triggerPerTarget` | Fires one event per target instead of one combined event. |
| `triggerOnce` | Fires once and waits until the player exits. |
| `resetOnExit` | Allows the trigger to fire again after leaving. |

## Entity targets

Entity targets can be used by the main waypoint, the zone trigger, and the BetterHUD bridge.

Supported lookup types:

| Type | Meaning |
|---|---|
| `UUID` | Tracks a specific Bukkit entity UUID. |
| `NAME` | Finds the nearest entity with a matching name. |
| `SCOREBOARD_TAG` | Finds the nearest entity with a scoreboard tag. |
| `TYPEWRITER_NPC` | Tracks a Typewriter NPC entry. |

Example:

```json
{
  "targetType": "SCOREBOARD_TAG",
  "tag": "quest_target",
  "displayName": "<gold>Quest Target</gold>",
  "maxDistance": 128.0,
  "priority": 10
}
```

