# WaypointRPGExtension

A [Typewriter](https://github.com/gabber235/Typewriter) extension that displays personal waypoints for each player's active quest objectives. `static_waypoint` is the single supported visual renderer.

## Requirements

| Component | Version |
|---|---|
| Paper / Purpur | 1.21.11 tested |
| Java | 21 |
| Typewriter | 0.9.0-beta-173, beta-174, or beta-175 matching build |
| PacketEvents | 2.9.4 (bundled with Typewriter) |
| BetterHUD | 1.14.1+ *(optional)* |

## Installation

1. Drop the JAR matching your Typewriter beta into `plugins/Typewriter/extensions/`.
2. Restart the server.
3. Seven entries are available in the Typewriter panel, including the simple
   `quest_waypoint` profile and its optional advanced `waypoint_theme`.

### Core shader resource pack (Minecraft 1.21.11)

When Typewriter loads the extension it creates this standalone resource-pack folder:

```text
plugins/WaypointRPGExtension/resourcepack/
├── pack.mcmeta
└── assets/minecraft/shaders/core/
    ├── rendertype_text.vsh
    ├── rendertype_text.fsh
    ├── rendertype_text_intensity.vsh
    ├── rendertype_text_intensity.fsh
    ├── rendertype_text_see_through.vsh
    ├── rendertype_text_see_through.fsh
    ├── rendertype_text_intensity_see_through.vsh
    └── rendertype_text_intensity_see_through.fsh
```

The extension does not edit CraftEngine. Add
`WaypointRPGExtension/resourcepack` to CraftEngine's
`resource-pack.merge-external-folders`, then rebuild and resend its pack.

The shaders preserve Mojang's behavior for ordinary text and ScreenRPG's menu-FOV
branch. `static_waypoint` marks only its label and symbol with opacity byte `252`.
See-through, shader correction, and smart occlusion are hardcoded. Every four ticks,
the renderer switches label and symbol to the protected see-through pass only when a
block obstructs them; a clear view keeps the regular depth-writing pass. Occlusion is
sampled at stable, non-bobbing centers for label and symbol. This avoids scaled edge
rays descending into distant terrain when the player views the waypoint near ground level.
Label and symbol use a hardcoded `VERTICAL` billboard with centered text alignment.
They follow camera yaw laterally, and their entity pitch is normalized to `0°` so
route or objective rotation data cannot leave them inclined.

## Entries

### `static_waypoint`
Renders a private two-layer `BlockDisplay` beam plus a Bukkit `TextDisplay` label
and symbol anchored to the objective. The marker is displaced toward the camera
past the beam radius so the column cannot intersect the text. It also supports
distance scaling, snapping, smooth bob motion, registered routes, rotating beam,
entity glow, registered entity targets, and optional BetterHUD integration. A newly tracked
target also receives a hardcoded Wynncraft-style acquisition effect: the symbol itself
performs a progressive three-stage focus lock (`1.90x` -> `1.55x` -> `1.25x` -> `1.00x`) over 11 ticks (0.55 seconds at 20 TPS). Each stage continues inward, with one-tick holds between stages, so the motion never bounces outward. The configured image is animated directly on its stable V3 display and finishes at the exact normal scale, avoiding entity handoff or texture-frame changes.

**Placeholders** (via PAPI or Typewriter): `%typewriter_<id>:direction:up%`, `:north%`, `:northeast%`, `:east%`, `:southeast%`, `:south%`, `:southwest%`, `:west%`, `:northwest%`, `:down%`

### `waypoint_zone_trigger`
Fires entry/exit triggers when a player enters or leaves a waypoint's detection radius. Supports objective targets, entity targets, and the active route point.

### `quest_waypoint`
Connects a selectable Quest directly to `INHERIT`, `PURPLE`, `GREEN`, `RED`, `GOLD`,
`BLUE`, or `CUSTOM`. All location objectives belonging to that quest inherit the
appearance automatically. Its only configuration fields are `quest`, `preset`, and
optional `customTheme`; `CUSTOM` selects an advanced `waypoint_theme`.

### `waypoint_path`
Defines an ordered path for one location objective. `objective` is a filtered entry
selector in the Typewriter panel, not a manually copied ID. Create one route entry per
objective and add/reorder its points there. If several active routes select the same
objective, the path with the highest `priority` wins. Appearance comes from the
objective's Quest profile, so this entry is only needed for actual checkpoints.

### `waypoint_theme`
Defines reusable beam materials, rotation/full-bright overrides, label text/shadow, and
symbol text/shadow. It is selected from `quest_waypoint.customTheme` only for advanced
`CUSTOM` profiles, or as an entity-specific override.

### `entity_waypoint`
Registers one entity target while its Typewriter audience is active. This lets a quest,
criterion, world audience, or any other parent decide exactly which players see it.
`TYPEWRITER_NPC` provides a filtered selector for any shared Typewriter entity instance;
Bukkit entities can instead be resolved by UUID, name, or scoreboard tag. Selecting its
optional Quest inherits `quest_waypoint`; `themeOverride` handles exceptional entities.

### `waypoint_betterhud_bridge`
Syncs the active waypoint targets to a [BetterHUD](https://github.com/toxicity188/BetterHud) compass element. Requires BetterHUD installed on the server.

## Waypoint Example

```json
{
  "id": "static_quest_waypoint",
  "typewriterId": "static_waypoint",
  "name": "Static Quest Waypoint",
  "general": {
    "hideOnArrive": true,
    "selection": "CLOSEST",
    "maxTargets": 5,
    "arriveRadius": 1.5
  },
  "target": {
    "offset": 0.0
  },
  "label": {
    "shadow": true,
    "text": "<white>{name}</white>\n<gold>{distance}</gold>",
    "height": 1.0,
    "floatDist": 5.0,
    "hideRange": 8.0,
    "minScale": 1.0,
    "maxScale": 8.0,
    "nearDist": 5.0,
    "farDist": 50.0,
    "opacity": 255,
    "lineWidth": 255
  },
  "symbol": {
    "enabled": true,
    "shadow": false,
    "text": "<gold>◆</gold>",
    "minScale": 3.0,
    "maxScale": 25.0,
    "nearDist": 5.0,
    "farDist": 150.0,
    "offset": 0.5,
    "snapRange": 8.0,
    "snapLeave": 12.0,
    "snapHeight": 3.0,
    "scaleSpacing": 0.16
  },
  "beam": {
    "enabled": true,
    "fullBright": true,
    "rotateInner": true,
    "outer": "LIME_STAINED_GLASS",
    "inner": "LIME_CONCRETE",
    "width": 0.5,
    "depth": 0.5,
    "coreWidth": 0.25,
    "coreDepth": 0.25,
    "height": 150.0,
    "depthBelow": 20.0,
    "labelClearance": 0.12
  },
  "bob": {
    "enabled": true,
    "height": 0.06,
    "speed": 1.2
  },
  "integrations": {
    "entityGlow": false,
    "glowRange": 20.0
  }
}
```

Use one global `static_waypoint`, normally under `world_audience`. It automatically
renders all tracked locatable objectives plus the active `waypoint_path` and
`entity_waypoint` entries for each player. You no longer need to duplicate the
renderer for every quest page.

The stable V3 movement is preserved: label and symbol stay anchored to the
waypoint inside 70 blocks and transition to a camera-range anchor from 70 to
72 blocks. The beam has its own anchor and a small physical clearance keeps it
behind the text.

The archived `tracked_locatable_waypoint` renderer is not registered and does
not appear in Typewriter. Its source is retained only as a historical reference.

## Direction Snippets

Direction glyphs for `{direction}` are read from Typewriter snippets. Add these keys to `plugins/Typewriter/snippets.yml`:

```yaml
waypoint.direction.up: "▲"
waypoint.direction.down: "▼"
waypoint.direction.north: "↑"
waypoint.direction.northeast: "↗"
waypoint.direction.east: "→"
waypoint.direction.southeast: "↘"
waypoint.direction.south: "↓"
waypoint.direction.southwest: "↙"
waypoint.direction.west: "←"
waypoint.direction.northwest: "↖"
```

Defaults are written automatically on first use if the keys are missing.

## BetterHUD Integration

Add a `waypoint_betterhud_bridge` entry and set `iconName` to the compass element defined in your BetterHUD layout. The bridge syncs positions every 0.25 s. BetterHUD is optional; if absent, the bridge disables itself without blocking the extension.

## Known Limitations

- Beam and Display Entities require Paper 1.21.1 or newer. Older versions are not supported.
- The extension uses PacketEvents for beam rendering; a server-side CraftEngine installation changes the block state lookup path (no action needed unless you see a beam material warning in console).
- BetterHUD compass requires BetterHUD 1.14.1+.

## FAQ

**The beam is not visible.**
Check that the beam material (`outer` / `inner`) is a valid solid block. Invalid materials log a warning and fall back to safe lime materials.

**{direction} shows a placeholder.**
Add the `waypoint.direction.*` keys to `plugins/Typewriter/snippets.yml` (see above) and run `/tw reload`.

**The text label disappears when I'm close.**
This is expected. `hideRange` controls the distance at which the text hides; the icon stays visible. Lower the value or set it to 0 to disable.

**Multiple objectives are shown at once.**
Set `general.maxTargets` to 1 to show only the highest-priority objective. Use `general.selection` to choose between HIGHEST_PRIORITY and CLOSEST.
