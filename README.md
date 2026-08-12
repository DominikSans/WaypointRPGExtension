# WaypointRPGExtension

A [Typewriter](https://typewritermc.com) extension that displays personal waypoints for each player's active quest objectives. `static_waypoint` is the single supported visual renderer.

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
3. The `static_waypoint` visual entry and its two auxiliary integration entries are available in the Typewriter panel.

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
distance scaling, snapping, smooth bob motion, advancing routes, rotating beam,
entity glow, entity targets, and optional BetterHUD integration.

**Placeholders** (via PAPI or Typewriter): `%typewriter_<id>:direction:up%`, `:north%`, `:northeast%`, `:east%`, `:southeast%`, `:south%`, `:southwest%`, `:west%`, `:northwest%`, `:down%`

### `waypoint_zone_trigger`
Fires entry/exit triggers when a player enters or leaves a waypoint's detection radius. Supports objective targets, entity targets, and the active route point.

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
  "routes": [],
  "integrations": {
    "entityGlow": false,
    "entityTargets": [],
    "glowRange": 20.0
  }
}
```

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
