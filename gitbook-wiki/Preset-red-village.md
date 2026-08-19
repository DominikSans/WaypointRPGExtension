# Preset (Red Village)

This v3.1.0 preset reproduces the current visual values from page `8qAYWA1k0zFJn27`
and combines them with the new Quest profile workflow.

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

## Source objective

The server page identifies the Quest as **Aldea Roja** and its active location objective
as **Ve a la aldea morada**:

| Value | Source page value |
|---|---|
| Quest entry | `jGOxxXwmghSTI8d` (`simple_quest_2`) |
| Objective entry | `4NfG2H5raB9D9SD` (`loc_morado`) |
| Target | `world: 3.51, 72, 50.69` |
| Objective criterion | fact `DDuBUGrXtKEYpWN == 3` |
| Quest completion | fact `DDuBUGrXtKEYpWN >= 4` |

## Waypoint preset

```json
{
  "type": "static_waypoint",
  "id": "waypoint_red_village",
  "name": "Red Village Waypoint",
  "general": {
    "hideOnArrive": false,
    "selection": "HIGHEST_PRIORITY",
    "maxTargets": 5,
    "arriveRadius": 1.2
  },
  "target": {
    "offset": 0.0
  },
  "label": {
    "shadow": true,
    "text": "{distance}<newline><white>{direction}</white>",
    "height": 1.0,
    "floatDist": 5.0,
    "hideRange": 8.0,
    "minScale": 1.0,
    "maxScale": 8.0,
    "nearDist": 5.0,
    "farDist": 59.0,
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
    "scaleSpacing": 0.15
  },
  "beam": {
    "enabled": true,
    "fullBright": true,
    "rotateInner": true,
    "outer": "STONE",
    "inner": "STONE",
    "width": 0.25,
    "depth": 0.25,
    "coreWidth": 0.1,
    "coreDepth": 0.1,
    "height": 150.0,
    "depthBelow": 5.0,
    "labelClearance": 1.0
  },
  "bob": {
    "enabled": false,
    "height": 0.4,
    "speed": 0.4
  }
}
```

## Quest profile

```json
{
  "type": "quest_waypoint",
  "id": "red_village_waypoint",
  "name": "Red Village Waypoint",
  "quest": "jGOxxXwmghSTI8d",
  "preset": "PURPLE",
  "customTheme": ""
}
```

## Path preset

The source page does not currently define intermediate path points. The following
section is optional: select `4NfG2H5raB9D9SD` from the `objective` picker and replace
the example checkpoints with the route you want players to follow. The path
automatically inherits the purple Quest profile.

```json
{
  "type": "waypoint_path",
  "id": "route_red_village",
  "name": "Red Village Route",
  "objective": "4NfG2H5raB9D9SD",
  "loop": false,
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

The checkpoint coordinates above are illustrative and are not stored in the source
page. The final objective remains `world: 3.51, 72, 50.69`.

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
