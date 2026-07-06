# WaypointRPGExtension

A [Typewriter](https://typewritermc.com) extension that displays a personal waypoint for each player's active quest objective. Renders a vertical beacon beam, floating text label, and scaling icon using Display Entities — visible only to the individual player.

## Requirements

| Component | Version |
|---|---|
| Paper | 1.21.1 |
| Java | 21 |
| Typewriter | 0.9.0-beta-173 |
| PacketEvents | 2.9.4 (bundled with Typewriter) |
| BetterHUD | 1.14.1+ *(optional)* |

## Installation

1. Drop `WaypointRPGExtension-1.0.0.jar` into `plugins/Typewriter/extensions/`.
2. Restart the server (or run `/tw reload`).
3. The three entries are available in the Typewriter panel.

## Entries

### `tracked_locatable_waypoint`
Renders a beam, text label, and icon for the player's active tracked objective. Configure text, beam materials, follow distances, float motion, and guided route points from the panel.

**Placeholders** (via PAPI or Typewriter): `%typewriter_<id>:direction:up%`, `:north%`, `:northeast%`, `:east%`, `:southeast%`, `:south%`, `:southwest%`, `:west%`, `:northwest%`, `:down%`

### `waypoint_zone_trigger`
Fires entry/exit triggers when a player enters or leaves a waypoint's detection radius. Supports objective targets, entity targets, and the active route point.

### `waypoint_betterhud_bridge`
Syncs the active waypoint targets to a [BetterHUD](https://github.com/toxicity188/BetterHud) compass element. Requires BetterHUD installed on the server.

## Minimal Example

```json
{
  "id": "main_quest_waypoint",
  "typewriterId": "tracked_locatable_waypoint",
  "name": "Main Quest Waypoint",
  "general": {
    "mode": "BOTH",
    "arriveRadius": 2.0,
    "hideOnArrive": true
  },
  "label": {
    "text": "<white>{name}</white>\n<gold>{distance}</gold>",
    "floatDist": 5.0
  },
  "beam": {
    "enabled": true,
    "dynamicHeight": true
  }
}
```

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

Add a `waypoint_betterhud_bridge` entry and set `iconName` to the compass element defined in your BetterHUD layout. The bridge syncs positions every 0.25 s. BetterHUD is optional — if not installed the entry is silently skipped.

## Known Limitations

- Beam and Display Entities require Paper 1.21.1 or newer. Older versions are not supported.
- The extension uses PacketEvents for beam rendering; a server-side CraftEngine installation changes the block state lookup path (no action needed unless you see a beam material warning in console).
- BetterHUD compass requires BetterHUD 1.14.1+.

## FAQ

**The beam is not visible.**
Check that the beam material (`outer` / `inner`) is a valid solid block. Invalid materials log a warning and fall back to STONE.

**{direction} shows a placeholder.**
Add the `waypoint.direction.*` keys to `plugins/Typewriter/snippets.yml` (see above) and run `/tw reload`.

**The text label disappears when I'm close.**
This is expected. `hideRange` controls the distance at which the text hides; the icon stays visible. Lower the value or set it to 0 to disable.

**Multiple objectives are shown at once.**
Set `general.maxTargets` to 1 to show only the highest-priority objective. Use `general.selection` to choose between HIGHEST_PRIORITY and CLOSEST.
