# How to Start

WaypointRPGExtension v3.1.0 adds personal RPG-style waypoint visuals to Typewriter quests. Players do not need a client mod.

## What it can show

* A stable V3-style floating label.
* A configurable symbol or resource-pack glyph.
* A private two-layer vertical beam.
* A rotating inner beam core.
* Route points before the final objective.
* Entity and Typewriter NPC targets.
* Private entity glow.
* Zone enter and exit triggers.
* Optional BetterHUD compass points.

## First setup path

1. Check [Requirements](Requirements.md).
2. Select the extension JAR matching your Typewriter beta.
3. Place it in `plugins/Typewriter/extensions/` and restart the server.
4. Follow [Waypoint Tutorial](Waypoint-tutorial.md).
5. Review every field in [Entry List](Entry-list.md).
6. Add zones, entities, routes, or BetterHUD using [Integrations](Integrations.md).
7. Configure arrows and custom glyphs using [Placeholders](Placeholders.md).

## Entries included

| Entry | Use it for |
|---|---|
| `static_waypoint` | One global renderer for label, symbol, and beam. It appears as **Waypoint** in the panel. |
| `quest_waypoint` | Selects a Quest and gives all its objectives a simple preset or custom theme. |
| `waypoint_theme` | Advanced reusable beam, label, text-based symbol, and BetterHUD icon appearance. |
| `waypoint_path` | Selects a location objective and defines only its ordered checkpoints. |
| `entity_waypoint` | Registers one Bukkit entity or selectable Typewriter entity for its active audience. |
| `waypoint_zone_trigger` | Fires Typewriter triggers when a player enters or leaves a target area. |
| `waypoint_betterhud_bridge` | Sends active targets to BetterHUD compass points. |

`tracked_locatable_waypoint` is retained in source only. It is not registered and does not appear in the panel.

The recommended page has one `static_waypoint` beneath `world_audience`. Quest pages
only define objectives, routes, and entity targets. The central player registry makes
the same active targets available to the renderer, zone trigger, and BetterHUD bridge.

## Resource-pack shaders

When the extension loads, it creates:

```text
plugins/WaypointRPGExtension/resourcepack/
```

Merge that folder into the resource pack sent to players. It contains four core shader
programs represented by eight `.vsh` and `.fsh` files. CraftEngine can merge it as an
external resource-pack folder.
