# How to Start

WaypointRPGExtension V4 adds personal RPG-style waypoint visuals to Typewriter quests. Players do not need a client mod.

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

1. Check [Requirements](requirements.md).
2. Select the extension JAR matching your Typewriter beta.
3. Place it in `plugins/Typewriter/extensions/` and restart the server.
4. Follow [Waypoint tutorial](waypoint-tutorial.md).
5. Review every field in [Entry list](entry-list.md).
6. Add zones, entities, routes, or BetterHUD using [Integrations](integrations.md).
7. Configure arrows and custom glyphs using [Placeholders](placeholders.md).

## Entries included

| Entry | Use it for |
|---|---|
| `static_waypoint` | Main waypoint: label, symbol, beam, routes, entities, and glow. It appears as **Waypoint** in the panel. |
| `waypoint_zone_trigger` | Fires Typewriter triggers when a player enters or leaves a target area. |
| `waypoint_betterhud_bridge` | Sends active targets to BetterHUD compass points. |

`tracked_locatable_waypoint` is retained in source only. It is not registered and does not appear in the panel.

## Resource-pack shaders

When the extension loads, it creates:

```text
plugins/WaypointRPGExtension/resourcepack/
```

Merge that folder into the resource pack sent to players. It contains the four core shaders used to keep label and symbol readable through walls. CraftEngine can merge it as an external resource-pack folder.
