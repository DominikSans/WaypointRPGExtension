# Quest waypoints, paths, and entities

v3.0.3 uses one global `static_waypoint`. Quest appearance, path checkpoints, and
entity targets are separate definitions consumed by the same per-player registry.

## Quick setup: `quest_waypoint`

Create one `quest_waypoint`, select the Quest, and choose a preset:

```json
{
  "type": "quest_waypoint",
  "id": "red_village_waypoint",
  "name": "Red Village Waypoint",
  "quest": "red_village_quest",
  "preset": "PURPLE",
  "customTheme": ""
}
```

All tracked location objectives whose `quest` field selects `red_village_quest`
automatically use the purple beam. No path entry or audience connection is needed just
to select the color.

Built-in presets:

| Preset | Outer / inner beam |
|---|---|
| `INHERIT` | Uses `static_waypoint` unchanged. |
| `PURPLE` | Purple stained glass / purple concrete. |
| `GREEN` | Green stained glass / green concrete. |
| `RED` | Red stained glass / red concrete. |
| `GOLD` | Yellow stained glass / yellow concrete. |
| `BLUE` | Light-blue stained glass / light-blue concrete. |
| `CUSTOM` | Uses the selected `customTheme`. |

Presets intentionally preserve the global label and symbol. Use a custom theme if a
Quest also needs a different glyph, image, label template, rotation, or brightness.

## Several simultaneous quest definitions

```text
Quest: Red Village ───── quest_waypoint: PURPLE
Quest: Forest Search ─── quest_waypoint: GREEN
Quest: Demon Castle ──── quest_waypoint: RED
```

Facts continue determining the state of each Quest. The extension reads the Quest
already referenced by every objective, so color does not depend on duplicated facts or
the visual position of entries in the page.

## Advanced appearance: `waypoint_theme`

Create this entry only when a built-in preset is insufficient:

```json
{
  "type": "waypoint_theme",
  "id": "animated_purple_theme",
  "name": "Animated Purple Theme",
  "beam": {
    "overrideMaterials": true,
    "overrideRotation": true,
    "overrideFullBright": false,
    "outer": "PURPLE_STAINED_GLASS",
    "inner": "PURPLE_CONCRETE_POWDER",
    "rotateInner": true,
    "fullBright": true
  },
  "label": {
    "overrideText": false,
    "overrideShadow": false,
    "text": "<light_purple>{name}</light_purple><newline>{distance}",
    "shadow": true
  },
  "symbol": {
    "overrideText": true,
    "overrideShadow": false,
    "text": "<light_purple>◆</light_purple>",
    "shadow": false
  }
}
```

Then configure the profile:

```json
{
  "type": "quest_waypoint",
  "quest": "red_village_quest",
  "preset": "CUSTOM",
  "customTheme": "animated_purple_theme"
}
```

Disabled override booleans inherit their values from `static_waypoint`.

## Paths: `waypoint_path`

Create a path only when an objective needs intermediate checkpoints. Its objective
picker is filtered to location objectives, and it inherits appearance from that
objective's Quest profile.

```json
{
  "type": "waypoint_path",
  "id": "red_village_path",
  "name": "Red Village Path",
  "objective": "red_village_objective",
  "priority": 10,
  "allowSkip": true,
  "resetOnObjectiveChange": true,
  "resetOnComplete": false,
  "points": [
    {
      "name": "Bridge",
      "position": {
        "world": "world",
        "x": 29.02,
        "y": 71.0,
        "z": 106.94,
        "yaw": 0.0,
        "pitch": 0.0
      },
      "radius": 3.0
    }
  ]
}
```

If two active paths select the same objective, the highest priority wins. Do not create
an empty path merely to assign a color; that is now the role of `quest_waypoint`.

## Entities: `entity_waypoint`

This remains an audience entry because its parent audiences decide which players can
see the entity target. Select an optional Quest to inherit its appearance:

```json
{
  "type": "entity_waypoint",
  "id": "red_village_guide",
  "name": "Red Village Guide",
  "targetType": "TYPEWRITER_NPC",
  "npc": "red_village_guide_instance",
  "quest": "red_village_quest",
  "themeOverride": "",
  "uuid": "",
  "entityName": "",
  "scoreboardTag": "",
  "displayName": "<gold>Village Guide</gold>",
  "maxDistance": 128.0,
  "priority": 10
}
```

Appearance precedence is:

```text
entity_waypoint.themeOverride
→ quest_waypoint selected by entity_waypoint.quest
→ static_waypoint fallback
```

Use `UUID`, `NAME`, or `SCOREBOARD_TAG` for Bukkit entities. `TYPEWRITER_NPC` exposes a
filtered selector for Typewriter shared entity instances.

## Recommended page structure

```text
world_audience
├── static_waypoint
├── waypoint_zone_trigger (optional)
└── waypoint_betterhud_bridge (optional)

Quest manifest page
├── quest
├── location_objective
├── quest_waypoint
├── waypoint_path (only with checkpoints)
├── waypoint_theme (only for CUSTOM)
└── entity_waypoint (optional audience entry)
```

## Migration from the temporary names

| Temporary v3.0.2 ID | v3.0.3 ID | Change |
|---|---|---|
| `waypoint_style` | `waypoint_theme` | Advanced appearance only. |
| `waypoint_route` | `waypoint_path` | Remove its old `style` field. |
| `waypoint_entity_target` | `entity_waypoint` | Replace `style` with optional `quest` and `themeOverride`. |
| none | `quest_waypoint` | New direct Quest-to-preset/profile association. |

## Zone and BetterHUD

`waypoint_zone_trigger` and `waypoint_betterhud_bridge` automatically consume active
objectives, paths, and entity waypoints. Their configuration does not duplicate paths
or entities. BetterHUD remains optional and is accessed reflectively.
