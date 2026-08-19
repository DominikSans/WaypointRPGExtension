# Waypoint Tutorial

This page creates the global `static_waypoint` using the tested headpop server values as a v3.1.0 starting point.

The reference values come from Typewriter page `8qAYWA1k0zFJn27` (`man_tuto_3`). That
page was saved with an older beta schema, so this tutorial reuses its visual values but
omits historical fields that are not public in v3.1.0.

{% hint style="info" %}
The v3.1.0 preset uses direct Quest profiles and removes local `routes` and `entityTargets` arrays.
{% endhint %}

{% stepper %}
{% step %}

## Create the entry

Create one **Waypoint** under a global `world_audience`:

```text
static_waypoint
```

A recommended layout is:

```text
world_audience
└── static_waypoint

tracked_quest_audience
├── location_objective
├── quest_waypoint
├── waypoint_path (only with checkpoints)
├── waypoint_theme (only for CUSTOM)
└── entity_waypoint (optional)
```

The global renderer automatically finds every tracked locatable objective and every
active target entry for that player. Do not duplicate it for each quest.

In `quest_waypoint`, select the same Quest referenced by the location objective and
choose a simple color preset. The objective inherits it automatically.

The source page contains these three Quest objectives:

| Quest | Objective display | Target location | Suggested v3.1.0 preset |
|---|---|---|---|
| Aldea Verde | Ve a la aldea verde | `world: 21.45, 72, 51.39` | `GREEN` |
| Aldea Roja | Ve a la aldea morada | `world: 3.51, 72, 50.69` | `PURPLE` |
| Aldea Azul | Ve a la aldea azul | `world: 46.42, 72, 51.39` | `BLUE` |

The Quest facts and criteria continue controlling which objective is active. The
waypoint extension only reads the active tracked objectives and their selected profiles.

{% endstep %}

{% step %}

## Select the target

The tested server uses:

```json
{
  "general": {
    "hideOnArrive": false,
    "selection": "HIGHEST_PRIORITY",
    "maxTargets": 5,
    "arriveRadius": 1.2
  },
  "target": {
    "offset": 0.0
  }
}
```

Use `maxTargets: 1` while learning if you want only one marker. Increase `target.offset` for an NPC or a low objective position.

{% endstep %}

{% step %}

## Configure the label

```json
{
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
  }
}
```

`shadow` appears first in the Label group. Through-wall and shader behavior are internal
and cannot be disabled from the panel. The final see-through shader protects every label
line and its shadow at the same depth.

{% endstep %}

{% step %}

## Add the symbol

```json
{
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
  }
}
```

The glyph `佚` is the server reference and requires the `images_rpg:emojis` font. Use `<gold>◆</gold>` if your pack does not provide that font. Symbols remain TextDisplays so the protected through-wall shader continues working.

`symbol.shadow` is independent from `label.shadow`. Keep `snapLeave` greater than `snapRange`.

The first time a target is tracked, the configured symbol image automatically performs a progressive three-stage focus lock over 11 ticks (0.55 seconds at 20 TPS): `1.90x` -> `1.55x` -> `1.25x` -> `1.00x`. The scale only moves inward and pauses for one tick between stages, avoiding any rebound. The same stable V3 display is used throughout and finishes at its exact normal scale, avoiding entity handoff or texture-frame changes. This behavior is internal and requires no additional panel fields. An animated CraftEngine image such as `<image:waypoints:quest_tracked>` can still be used as `symbol.text`; its texture animation and the one-shot acquisition effect are independent. A separate white focus-marker layer is reserved as a possible future option and is not currently rendered.

{% endstep %}

{% step %}

## Add the beam

```json
{
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
  }
}
```

The inner core rotates while the outer layer remains fixed. `labelClearance` keeps the beam physically behind label and symbol.

Beam height and update cadence are controlled internally. v3.1.0 does not expose the old dynamic or follow controls.

{% endstep %}

{% step %}

## Configure bob

```json
{
  "bob": {
    "enabled": false,
    "height": 0.4,
    "speed": 0.4
  }
}
```

These are the current server values. Bob is disabled, but its stored height and speed are
`0.4`. Enable it only when floating motion is desired.

{% endstep %}

{% step %}

## Load the shaders and test

Merge this generated folder into the final server resource pack:

```text
plugins/WaypointRPGExtension/resourcepack/
```

Restart after replacing the extension. Track the quest, sprint toward the objective,
and test the label and symbol separately behind a full wall, leaves, glass, doors, and
the edge of a slab. Full obstruction should activate immediately; leaves and partial
shapes should stabilize without flashing; glass and panes alone should retain the normal
depth-tested pass. A solid wall behind glass must still activate protection.

{% endstep %}
{% endstepper %}

## Complete starter example

```json
{
  "type": "static_waypoint",
  "id": "main_quest_waypoint",
  "name": "Main Quest Waypoint",
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

Create routes and entity targets as separate entries as shown in
[Routes and Entity Targets](Integrations.md). Use [Entry List](Entry-list.md) for every
field and [Placeholders](Placeholders.md) for text and custom glyphs.
