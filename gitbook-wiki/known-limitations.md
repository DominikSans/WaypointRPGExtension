# Known Limitations

WaypointRPGExtension is server-side.

It uses Minecraft Display Entities and packet-based visuals, so it cannot behave exactly like a client-side Fabric mod.

## Remote servers

On remote servers, visual smoothness depends on:

- player ping;
- server tick stability;
- chunk loading;
- packet delivery;
- resource pack weight;
- other plugins modifying chunk packets.

The extension includes motion compensation, but it cannot use true client-side prediction.

## Chunk loading

If the player's Minecraft ping rises only while moving, the cause is usually chunk loading or chunk packet delivery, not the waypoint display itself.

Pregenerated chunks help with world generation, but they do not remove the cost of loading and sending chunks to the client.

## BetterHUD

BetterHUD integration requires BetterHUD to be installed and configured separately.

## Materials

Beam materials must be valid blocks. Invalid materials fall back to safe defaults and may log a warning.

