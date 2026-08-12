# Integrations

## Routes

Routes temporarily replace an objective's final location with ordered checkpoints.

```json
{
  "routes": [
    {
      "objectiveId": "main_objective",
      "routeId": "main_path",
      "allowSkip": true,
      "resetOnObjectiveChange": true,
      "resetOnComplete": false,
      "points": [
        {
          "name": "Bridge",
          "position": "bridge_position",
          "radius": 3.0
        },
        {
          "name": "Gate",
          "position": "gate_position",
          "radius": 3.0
        }
      ]
    }
  ]
}
```

The visual `static_waypoint` owns route advancement. Zone and BetterHUD entries read the shared route state.

## Entity targets

Entity targets can resolve by UUID, name, scoreboard tag, or Typewriter NPC entry ID. They can be used by all three registered entries.

```json
{
  "integrations": {
    "entityGlow": true,
    "entityTargets": [
      {
        "targetType": "SCOREBOARD_TAG",
        "tag": "quest_target",
        "displayName": "<gold>Quest Target</gold>",
        "maxDistance": 128.0,
        "priority": 10
      }
    ],
    "glowRange": 20.0
  }
}
```

Entity glow is private to the waypoint audience.

## Zone trigger

`waypoint_zone_trigger` fires `onEnter` and `onExit` around objectives, entities, or the active route point.

Important fields:

| Field | Purpose |
|---|---|
| `radius` | Detection radius. |
| `targetMode` | `OBJECTIVES_ONLY`, `ANY_ACTIVE_TARGET`, or `ACTIVE_ROUTE_POINT`. |
| `triggerPerTarget` | Separates trigger execution by target. |
| `triggerOnce` | Prevents repetition until rearmed. |
| `resetOnExit` | Rearms after the player leaves. |

Checks run every five ticks as an internal policy.

## BetterHUD bridge

`waypoint_betterhud_bridge` is optional. `static_waypoint` does not require BetterHUD.

The bridge uses reflection so BetterHUD classes are never a load-time dependency. If BetterHUD is absent, the bridge disables itself without preventing the extension from loading.

```json
{
  "type": "waypoint_betterhud_bridge",
  "id": "quest_compass",
  "name": "Quest Compass",
  "iconName": "quest_waypoint",
  "pointNamePrefix": "waypoint_",
  "targetMode": "ANY_ACTIVE_TARGET",
  "maxTargets": 3,
  "selection": "CLOSEST",
  "arriveRadius": 2.0,
  "entityTargets": [],
  "routes": []
}
```
