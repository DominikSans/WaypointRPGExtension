# Tested Setup

This page lists the environment used to test `WaypointRPGExtension 1.0.0`.

Use it as a compatibility reference when installing the jar on your server.

## Server

| Component | Tested with | Required |
|---|---|---|
| Minecraft | `1.21.11` | Yes |
| Server software | `Purpur / Paper` | Yes |
| Java | `21+` | Yes |
| Typewriter | `0.9.0-beta-173` or compatible | Yes |

## Required plugins / libraries

| Dependency | Tested with | Notes |
|---|---|---|
| Typewriter | `0.9.0-beta-173` | Main dependency. The extension is loaded by Typewriter. |
| PacketEvents | Typewriter bundled / compatible version | Used for packet-based display updates. |

## Optional integrations

| Plugin | Tested with | Required | Used for |
|---|---|---:|---|
| BetterHUD | `1.14.1+` | No | Compass waypoint integration. |
| PlaceholderAPI | Compatible with your Typewriter setup | No | Placeholder expansion, if your setup uses it. |
| WorldGuard | Compatible server version | No | Not required; useful alongside zone-trigger style quest areas. |
| CraftEngine | Compatible server version | No | Works alongside custom block/resource-pack servers. |

## Typewriter entries used in examples

| Entry | Source | Used for |
|---|---|---|
| `quest` | Typewriter quest system | Quest state. |
| `location_objective` | Typewriter quest system | Target position. |
| `tracked_quest_audience` | Typewriter quest system | Shows waypoint entries while the quest is tracked. |
| `tracked_locatable_waypoint` | WaypointRPGExtension | Text, icon, beam, routes, entity targets. |
| `waypoint_zone_trigger` | WaypointRPGExtension | Enter/exit triggers around targets. |
| `waypoint_betterhud_bridge` | WaypointRPGExtension | BetterHUD compass points. |

## Recommended test checklist

Before using the jar in production, test:

- one simple `location_objective`;
- one `tracked_locatable_waypoint`;
- text + icon + beam enabled together;
- `rotateLikeBeacon` enabled and disabled;
- player movement on a remote server;
- large Y movement, such as climbing or falling;
- BetterHUD bridge if BetterHUD is installed;
- zone trigger enter and exit behavior;
- direction snippets after `/tw reload`.

## Notes

- BetterHUD is optional. If it is not installed, the bridge entry does not run.
- Chunk loading, resource packs, and custom block plugins can affect client-side smoothness while moving.
