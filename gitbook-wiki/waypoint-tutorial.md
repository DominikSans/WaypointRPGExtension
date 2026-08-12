# Waypoint tutorial

This page creates the global `static_waypoint` using the tested headpop server values as a v3.0.3 starting point.

{% hint style="info" %}
The v3.0.3 preset uses direct Quest profiles and removes local `routes` and `entityTargets` arrays.
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

{% endstep %}

{% step %}

## Select the target

The tested server uses:

```json
{
  "general": {
    "hideOnArrive": true,
    "selection": "HIGHEST_PRIORITY",
    "maxTargets": 5,
    "arriveRadius": 1.5
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
    "text": "<white>{name}</white><newline>{distance}<newline><white>{direction}</white>",
    "height": 1.0,
    "hideRange": 8.0,
    "minScale": 2.0,
    "maxScale": 8.0,
    "nearDist": 18.0,
    "farDist": 45.0,
    "opacity": 255,
    "lineWidth": 200
  }
}
```

`shadow` appears first in the Label group. Through-wall and shader behavior are internal and cannot be disabled from the panel.

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
    "scaleSpacing": 0.25
  }
}
```

The glyph `佚` is the server reference and requires the `images_rpg:emojis` font. Use `<gold>◆</gold>` if your pack does not provide that font.

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
    "outer": "PURPLE_STAINED_GLASS",
    "inner": "PURPLE_CONCRETE_POWDER",
    "width": 0.3,
    "depth": 0.3,
    "coreWidth": 0.15,
    "coreDepth": 0.15,
    "height": 150.0,
    "depthBelow": 5.0,
    "labelClearance": 1.0
  }
}
```

The inner core rotates while the outer layer remains fixed. `labelClearance` keeps the beam physically behind label and symbol.

Beam height and update cadence are controlled internally. v3.0.3 does not expose the old dynamic or follow controls.

{% endstep %}

{% step %}

## Configure bob

```json
{
  "bob": {
    "enabled": true,
    "height": 0.4,
    "speed": 0.6
  }
}
```

These are the tested server values. For a subtle effect, try height `0.06` and speed `1.2`.

{% endstep %}

{% step %}

## Load the shaders and test

Merge this generated folder into the final server resource pack:

```text
plugins/WaypointRPGExtension/resourcepack/
```

Restart after replacing the extension. Track the quest, sprint toward the objective, and test again from behind a wall.

{% endstep %}
{% endstepper %}

## Complete starter example

```json
{
  "type": "static_waypoint",
  "id": "main_quest_waypoint",
  "name": "Main Quest Waypoint",
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

Create routes and entity targets as separate entries as shown in
[Routes and entity targets](integrations.md). Use [Entry list](entry-list.md) for every
field and [Placeholders](placeholders.md) for text and custom glyphs.
