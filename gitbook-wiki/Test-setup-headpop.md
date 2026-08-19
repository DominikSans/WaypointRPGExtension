# Test Setup (headpop)

This page records the environment and visual values used to validate WaypointRPGExtension v3.1.0.

## Server

| Component | Tested with | Required |
|---|---|---:|
| Minecraft | `1.21.11` | Yes |
| Server software | Purpur / Paper | Yes |
| Java | `21+` | Yes |
| Typewriter | beta-173 primary; beta-174 and beta-175 builds also produced | Yes |
| PacketEvents | Typewriter-compatible version | Yes |
| CraftEngine | Resource-pack folder merger | No |
| BetterHUD | Optional compass integration | No |

## Reference waypoint values

These values come from page `8qAYWA1k0zFJn27` and are used throughout this v3.1.0 documentation:

| Section | Tested values |
|---|---|
| General | `hideOnArrive=false`, `HIGHEST_PRIORITY`, 5 targets, arrival `1.2` |
| Label | distance + direction, scale `1–8`, distances `5–59`, hide range `8`, shadow enabled |
| Symbol | scale `6–25`, distances `15–45`, offset `0.5`, spacing `0.15` |
| Beam | stone / stone, outer `0.25`, core `0.10`, rotating core |
| Beam clearance | `1.0` |
| Bob | disabled; stored height `0.4`, speed `0.4` |

The source page reports schema `0.9.0-beta-170` and still contains the historical
`integrations.entityGlow` and `integrations.glowRange` properties. They are not included
in the v3.1.0 examples. Glow now belongs to `entity_waypoint`, and the public panel no
longer exposes the old `static_waypoint.integrations` group.

## Test checklist

* Walk and sprint toward the waypoint.
* Verify the stable V3 movement inside and beyond the 70-block culling boundary.
* Confirm the beam remains behind label and symbol.
* Test label and symbol through a wall with the final resource pack loaded.
* Confirm glass alone stays in the normal pass and a wall behind glass is detected.
* Toggle `label.shadow` and `symbol.shadow` independently.
* Enter and leave symbol snap range.
* Test route advancement with two points of radius `3.0`.
* Start the server without BetterHUD and confirm the extension still loads.
