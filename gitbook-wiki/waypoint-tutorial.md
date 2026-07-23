# Waypoint Tutorial

This page shows how to start with `tracked_locatable_waypoint`, the main entry of the extension.

Use this entry when you want the player to see where their active objective is.

## 1. Create the entry

In Typewriter, create:

```txt
tracked_locatable_waypoint
```

Use it under the audience that should see the waypoint. If it is placed globally, every valid player in that audience can receive the display.

## 2. Choose what the player sees

Start with:

```json
{
  "general": {
    "mode": "BOTH",
    "selection": "CLOSEST",
    "maxTargets": 1,
    "arriveRadius": 2.0,
    "hideOnArrive": true
  }
}
```

Recommended first values:

| Field | Value | Why |
|---|---:|---|
| `mode` | `BOTH` | Shows text/icon and beam together. |
| `selection` | `CLOSEST` | Easy to understand while testing. |
| `maxTargets` | `1` | Keeps the first setup simple. |
| `arriveRadius` | `2.0` | Good default for locations. |
| `hideOnArrive` | `true` | Cleans the display when the target is reached. |

## 3. Set target height

For a location objective:

```json
{
  "target": {
    "offset": 0.0,
    "verticalThreshold": 10.0
  }
}
```

For an NPC target, use an offset around `2.0` so the marker appears near head height.

## 4. Configure the text

```json
{
  "label": {
    "text": "<gold>{name}</gold><newline><white>{distance} {direction}</white>",
    "floatDist": 5.0,
    "hideRange": 5.0,
    "billboard": "CENTER",
    "align": "CENTER"
  }
}
```

Useful placeholders:

| Placeholder | Shows |
|---|---|
| `{name}` | Objective, route point, or entity name. |
| `{distance}` | Distance to the current target. |
| `{direction}` | Direction arrow from snippets. |
| `{index}` | Current visible target number. |
| `{total}` | Total visible targets. |
| `{route_name}` | Current route point name. |

## 5. Add an icon

```json
{
  "symbol": {
    "enabled": true,
    "text": "<gold>◆</gold>",
    "minScale": 3.0,
    "maxScale": 5.0,
    "snapRange": 8.0,
    "snapLeave": 12.0,
    "snapHeight": 3.0
  }
}
```

You can use normal Unicode, MiniMessage colors, or a resource-pack font glyph.

## 6. Add the beam

```json
{
  "beam": {
    "enabled": true,
    "outer": "LIME_STAINED_GLASS",
    "inner": "LIME_CONCRETE",
    "height": 150.0,
    "dynamicHeight": true,
    "fullBright": true,
    "rotateLikeBeacon": false
  }
}
```

Use `dynamicHeight` when targets can be above or below the player.

Use `rotateLikeBeacon` when you want the inner core to spin while the outer layer stays still.

## 7. Add route points when needed

Routes are part of the same waypoint entry. Use them when the player should visit checkpoints before the final objective.

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

When the player reaches `Bridge`, the waypoint moves to `Gate`. When the route is complete, it points to the final objective unless `resetOnComplete` is enabled.

## Complete starter example

```json
{
  "type": "tracked_locatable_waypoint",
  "id": "main_quest_waypoint",
  "name": "Main Quest Waypoint",
  "general": {
    "mode": "BOTH",
    "selection": "CLOSEST",
    "maxTargets": 1,
    "arriveRadius": 2.0,
    "hideOnArrive": true
  },
  "target": {
    "offset": 0.0,
    "verticalThreshold": 10.0
  },
  "label": {
    "text": "<gold>{name}</gold><newline><white>{distance} {direction}</white>",
    "floatDist": 5.0,
    "hideRange": 5.0
  },
  "symbol": {
    "enabled": true,
    "text": "<gold>◆</gold>"
  },
  "beam": {
    "enabled": true,
    "outer": "LIME_STAINED_GLASS",
    "inner": "LIME_CONCRETE",
    "dynamicHeight": true,
    "fullBright": true
  },
  "bob": {
    "enabled": true,
    "height": 0.06,
    "speed": 1.2
  }
}
```

