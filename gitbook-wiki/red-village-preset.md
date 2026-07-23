# Red Village Preset

This preset shows one complete quest flow using Typewriter quest entries together with WaypointRPGExtension.

Use it as a starting point for a simple objective:

```txt
Quest starts
↓
Player sees a waypoint to the red village
↓
Waypoint text, icon, beam, zone trigger, and BetterHUD point are active
↓
Player enters the zone
↓
Typewriter trigger runs
```

## Preview images

Add your screenshots here after importing this page into GitBook.

| View | Image |
|---|---|
| Waypoint from far away | Add image here |
| Text and icon close-up | Add image here |
| BetterHUD compass point | Add image here |
| Zone trigger moment | Add image here |

## What this preset uses

| Entry | Purpose |
|---|---|
| `quest` | Controls when the objective is active and completed. |
| `location_objective` | Stores the red village target position. |
| `tracked_quest_audience` | Shows the child entries only while the quest is tracked. |
| `tracked_locatable_waypoint` | Displays text, icon, and beam. |
| `waypoint_zone_trigger` | Runs triggers when the player reaches the target area. |
| `waypoint_betterhud_bridge` | Sends the target to BetterHUD. |

## Recommended layout

```txt
tracked_quest_audience
├─ location_objective
├─ tracked_locatable_waypoint
├─ waypoint_zone_trigger
└─ waypoint_betterhud_bridge
```

## Clean preset JSON

This example keeps the important parts and removes private IDs, exact world data, NPC skin data, and old fields that are handled internally.

Replace:

- `quest_fact_progress`
- `red_village_position`
- `on_enter_red_village`
- `on_exit_red_village`
- `betterhud_icon_name`

with your own Typewriter values.

```json
{
  "type": "manifest",
  "name": "red_village_waypoint_preset",
  "entries": [
    {
      "id": "quest_red_village",
      "type": "quest",
      "name": "quest_red_village",
      "displayName": "Go to the Red Village",
      "activeCriteria": [
        {
          "fact": "quest_fact_progress",
          "operator": ">=",
          "value": 3
        }
      ],
      "completedCriteria": [
        {
          "fact": "quest_fact_progress",
          "operator": ">=",
          "value": 4
        }
      ]
    },
    {
      "id": "objective_red_village",
      "type": "location_objective",
      "name": "objective_red_village",
      "quest": "quest_red_village",
      "display": "Go to the Red Village",
      "criteria": [
        {
          "fact": "quest_fact_progress",
          "operator": "==",
          "value": 3
        }
      ],
      "targetLocation": {
        "world": "world",
        "x": 0.0,
        "y": 100.0,
        "z": 0.0,
        "yaw": 0.0,
        "pitch": 0.0
      }
    },
    {
      "id": "audience_red_village",
      "type": "tracked_quest_audience",
      "name": "audience_red_village",
      "quest": "quest_red_village",
      "inverted": false,
      "children": [
        "objective_red_village",
        "waypoint_red_village",
        "zone_red_village",
        "hud_red_village"
      ]
    },
    {
      "id": "waypoint_red_village",
      "type": "tracked_locatable_waypoint",
      "name": "waypoint_red_village",
      "general": {
        "mode": "BOTH",
        "selection": "CLOSEST",
        "maxTargets": 5,
        "arriveRadius": 1.5,
        "hideOnArrive": true
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
        "hideRange": 5.0,
        "fov": 75.0,
        "scale": 1.0,
        "billboard": "CENTER",
        "align": "CENTER",
        "background": true,
        "bgColor": "#80000000",
        "opacity": 255,
        "shadow": true,
        "lineWidth": 200,
        "multiOffset": 0.45
      },
      "symbol": {
        "enabled": true,
        "text": "<white><font:images_rpg:emojis>丅</font></white>",
        "minScale": 2.0,
        "maxScale": 5.0,
        "nearDist": 20.0,
        "farDist": 150.0,
        "offset": 0.8,
        "snapRange": 5.0,
        "snapLeave": 8.0,
        "snapHeight": 2.5
      },
      "beam": {
        "enabled": true,
        "outer": "LIME_STAINED_GLASS",
        "inner": "LIME_CONCRETE",
        "width": 0.5,
        "coreWidth": 0.25,
        "depth": 0.5,
        "coreDepth": 0.25,
        "height": 125.0,
        "dynamicHeight": true,
        "follow": {
          "staticRange": 30.0,
          "followRange": 35.0,
          "followDist": 45.0
        },
        "fadeStart": 10.0,
        "fadeEnd": 3.0,
        "fullBright": true,
        "rotateLikeBeacon": true
      },
      "bob": {
        "enabled": true,
        "height": 0.06,
        "speed": 1.2
      },
      "routes": [],
      "integrations": {
        "entityTargets": [],
        "entityGlow": true,
        "glowRange": 10.0
      },
      "performance": {
        "lazyUpdate": true,
        "cleanupOnJoin": true,
        "cleanupRadius": 50.0
      }
    },
    {
      "id": "zone_red_village",
      "type": "waypoint_zone_trigger",
      "name": "zone_red_village",
      "radius": 5.0,
      "targetMode": "ANY_ACTIVE_TARGET",
      "maxTargets": 2,
      "selection": "HIGHEST_PRIORITY",
      "triggerPerTarget": true,
      "triggerOnce": true,
      "resetOnExit": true,
      "entityTargets": [],
      "onEnter": "on_enter_red_village",
      "onExit": "on_exit_red_village"
    },
    {
      "id": "hud_red_village",
      "type": "waypoint_betterhud_bridge",
      "name": "hud_red_village",
      "iconName": "betterhud_icon_name",
      "pointNamePrefix": "waypoint_",
      "targetMode": "ANY_ACTIVE_TARGET",
      "maxTargets": 5,
      "selection": "CLOSEST",
      "arriveRadius": 50.0,
      "entityTargets": []
    }
  ]
}
```

## Visual variation: lime adventure marker

Use this when the quest target should feel friendly or safe.

```json
{
  "symbol": {
    "enabled": true,
    "text": "<white><font:images_rpg:emojis>丅</font></white>",
    "minScale": 2.0,
    "maxScale": 5.0
  },
  "beam": {
    "enabled": true,
    "outer": "LIME_STAINED_GLASS",
    "inner": "LIME_CONCRETE",
    "rotateLikeBeacon": true
  }
}
```

## Visual variation: red danger marker

Use this for hostile areas, boss arenas, or warnings.

```json
{
  "label": {
    "text": "<red>{name}</red><newline><white>{distance} {direction}</white>"
  },
  "symbol": {
    "enabled": true,
    "text": "<red>◆</red>",
    "minScale": 3.0,
    "maxScale": 6.0
  },
  "beam": {
    "enabled": true,
    "outer": "RED_STAINED_GLASS",
    "inner": "RED_CONCRETE",
    "rotateLikeBeacon": true
  }
}
```

## Visual variation: subtle compass-only support

Use this when BetterHUD should carry the off-screen direction and the world display should stay clean.

```json
{
  "label": {
    "hideRange": 8.0,
    "fov": 65.0,
    "background": false
  },
  "beam": {
    "enabled": true,
    "fadeStart": 12.0,
    "fadeEnd": 4.0
  },
  "bob": {
    "enabled": true,
    "height": 0.04,
    "speed": 1.0
  }
}
```

## Image checklist

When preparing the public page, add screenshots for:

- the waypoint from 80–120 blocks away;
- the label and icon while running toward the target;
- the beam with `rotateLikeBeacon` enabled;
- the BetterHUD compass point;
- the zone trigger moment, if it starts a dialogue or cutscene.

Keep screenshots free of private coordinates, staff tools, private chat, or server-only debug overlays.

