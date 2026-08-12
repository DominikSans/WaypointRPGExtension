# Test Setup (headpop)

This page records the environment and visual values used to validate WaypointRPGExtension v3.0.3.

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

These values come from the tested server page and are used throughout this v3.0.3 documentation:

| Section | Tested values |
|---|---|
| General | `HIGHEST_PRIORITY`, 5 targets, arrival `1.5` |
| Label | scale `2–8`, distances `18–45`, hide range `8`, shadow enabled |
| Symbol | scale `6–25`, distances `15–45`, offset `0.5`, spacing `0.25` |
| Beam | purple glass/powder, outer `0.30`, core `0.15`, rotating core |
| Beam clearance | `1.0` |
| Bob | height `0.4`, speed `0.6` |

The server page still contains several historical JSON properties from earlier schemas. They are not included in the v3.0.3 examples because the current panel no longer exposes or uses them.

## Test checklist

* Walk and sprint toward the waypoint.
* Verify the stable V3 movement inside and beyond the 70-block culling boundary.
* Confirm the beam remains behind label and symbol.
* Test label and symbol through a wall with the final resource pack loaded.
* Toggle `label.shadow` and `symbol.shadow` independently.
* Enter and leave symbol snap range.
* Test route advancement with two points of radius `3.0`.
* Start the server without BetterHUD and confirm the extension still loads.
