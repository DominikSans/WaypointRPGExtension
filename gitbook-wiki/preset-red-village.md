# Preset (red village)

This v3.0.3 preset uses one global `static_waypoint`, a Quest profile, and an optional path.

## Page example

<div><figure><img src="/files/1PPZIfW5vBDdbGECkLzW" alt="Waypoint page configuration"><figcaption></figcaption></figure> <figure><img src="/files/PowYlONEq2Mn2wAVf33d" alt="Waypoint visual configuration"><figcaption></figcaption></figure></div>

<div><figure><img src="/files/PbPt7e0MwawqmmztjW08" alt="Waypoint in game"><figcaption></figcaption></figure> <figure><img src="/files/mbb7ym42mg7X6TVwQ3Xu" alt="Waypoint route"><figcaption></figcaption></figure></div>

## Recommended layout

```text
world_audience
├── static_waypoint
├── waypoint_zone_trigger
└── waypoint_betterhud_bridge (optional)

tracked_quest_audience
├── location_objective
├── quest_waypoint: PURPLE
└── waypoint_path
```

## Waypoint preset

```json
{
  "type": "static_waypoint",
  "id": "waypoint_red_village",
  "name": "Red Village Waypoint",
  "general": {
    "hideOnArrive": true,
    "selection": "HIGHEST_PRIORITY",
    "maxTargets": 5,
    "arriveRadius": 1.5
  },
  "target": {
    "offset": 0.0
  },
  "label": {
    "shadow": true,
    "text": "<white>{name}</white><newline>{distance}<newline><white>{direction}</white>",
    "height": 1.0,
    "hideRange": 8.0,
    "minScale": 2.0,
    "maxScale": 8.0,
    "nearDist": 18.0,
    "farDist": 45.0,
    "opacity": 255,
    "lineWidth": 200
  },
  "symbol": {
    "enabled": true,
    "shadow": false,
    "text": "<white><font:images_rpg:emojis>佚</font></white>",
    "minScale": 6.0,
    "maxScale": 25.0,
    "nearDist": 15.0,
    "farDist": 45.0,
    "offset": 0.5,
    "snapRange": 8.0,
    "snapLeave": 12.0,
    "snapHeight": 3.0,
    "scaleSpacing": 0.25
  },
  "beam": {
    "enabled": true,
    "fullBright": true,
    "rotateInner": true,
    "outer": "PURPLE_STAINED_GLASS",
    "inner": "PURPLE_CONCRETE_POWDER",
    "width": 0.3,
    "depth": 0.3,
    "coreWidth": 0.15,
    "coreDepth": 0.15,
    "height": 150.0,
    "depthBelow": 5.0,
    "labelClearance": 1.0
  },
  "bob": {
    "enabled": true,
    "height": 0.4,
    "speed": 0.6
  },
  "integrations": {
    "entityGlow": false,
    "glowRange": 20.0
  }
}
```

## Quest profile

```json
{
  "type": "quest_waypoint",
  "id": "red_village_waypoint",
  "name": "Red Village Waypoint",
  "quest": "your_red_village_quest_id",
  "preset": "PURPLE",
  "customTheme": ""
}
```

## Path preset

Select the Red Village location objective from the `objective` picker in the panel.
The path automatically inherits the purple Quest profile.

```json
{
  "type": "waypoint_path",
  "id": "route_red_village",
  "name": "Red Village Route",
  "objective": "your_red_village_objective_id",
  "priority": 10,
  "allowSkip": true,
  "resetOnObjectiveChange": true,
  "resetOnComplete": false,
  "points": [
    {
      "name": "point1",
      "position": {
        "world": "world",
        "x": 29.02,
        "y": 71.0,
        "z": 106.94,
        "yaw": 0.0,
        "pitch": 0.0
      },
      "radius": 3.0
    },
    {
      "name": "point2",
      "position": {
        "world": "world",
        "x": 35.01,
        "y": 70.0,
        "z": 65.61,
        "yaw": 0.0,
        "pitch": 0.0
      },
      "radius": 3.0
    }
  ]
}
```

Replace the objective ID and coordinates with your own page values. The coordinates shown reproduce the two-point pattern used by the test server.

## Optional zone trigger

```json
{
  "type": "waypoint_zone_trigger",
  "id": "zone_red_village",
  "name": "Red Village Zone",
  "radius": 5.0,
  "targetMode": "ACTIVE_ROUTE_POINT",
  "maxTargets": 1,
  "selection": "CLOSEST",
  "triggerPerTarget": false,
  "triggerOnce": true,
  "resetOnExit": true,
  "onEnter": "on_enter_red_village",
  "onExit": "on_exit_red_village"
}
```

BetterHUD is not required. Add `waypoint_betterhud_bridge` only when the server also needs a compass point.
