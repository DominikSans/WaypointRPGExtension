# Tracked Locatable Waypoint

`tracked_locatable_waypoint` is the main entry.

It displays the player's active objective using:

- floating text;
- scaling icon;
- vertical beam;
- optional route points;
- optional entity targets;
- optional BetterHUD compass sync.

## Recommended setup

Use this entry as a child of a Typewriter audience when you want it to apply only to selected players.

If used as a root entry, it can apply globally depending on your Typewriter setup.

## Visual parts

| Section | Purpose |
|---|---|
| `general` | Mode, target selection, arrival behavior. |
| `target` | Vertical offset and vertical direction threshold. |
| `label` | Floating text near the camera. |
| `symbol` | Icon displayed with the waypoint. |
| `beam` | Vertical beam column. |
| `bob` | Small float motion for text and icon. |
| `routes` | Ordered route points before the final objective. |
| `integrations` | Entity targets and glow options. |
| `performance` | Cleanup behavior. |

## Display modes

| Mode | Result |
|---|---|
| `BOTH` | Shows text/icon and beam. |
| `HOLOGRAM` | Shows text/icon only. |
| `BEAM` | Shows beam only. |

## Notes

The waypoint is server-side. Movement smoothness depends on server tick stability, ping, chunk loading, and packet delivery.

