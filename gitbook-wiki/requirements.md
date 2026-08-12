# Requirements

| Component | Required | Tested / supported value |
|---|---:|---|
| Paper / Purpur | Yes | Minecraft `1.21.11` tested |
| Java | Yes | `21+` |
| Typewriter | Yes | `0.9.0-beta-173`, beta-174, or beta-175 |
| Matching extension build | Yes | Use the JAR compiled for the installed Typewriter beta |
| PacketEvents | Yes | Supplied by or compatible with the Typewriter setup |
| BetterHUD | No | Only for `waypoint_betterhud_bridge` |
| CraftEngine | No | Recommended when merging the generated shader folder |

## Extension installation

Place the matching build in:

```text
plugins/Typewriter/extensions/
```

Restart the server after installing or replacing the extension.

## Shader pack

The extension generates:

```text
plugins/WaypointRPGExtension/resourcepack/
├── pack.mcmeta
└── assets/minecraft/shaders/core/
    ├── rendertype_text.vsh
    ├── rendertype_text.fsh
    ├── rendertype_text_intensity.vsh
    └── rendertype_text_intensity.fsh
```

The shader folder is required for the protected through-wall appearance. WaypointRPGExtension creates the folder but does not distribute the final resource pack.

For CraftEngine, it must be listed under `merge-external-folders`, not under `merge-external-zip-files`:

```yaml
resource-pack:
  merge-external-folders:
    - "ModelEngine/resource pack"
    - "WaypointRPGExtension/resourcepack"
```

Generate and resend the CraftEngine pack only after Typewriter has created or refreshed the folder. Verify that the resulting ZIP contains all four shader files.

## Client requirements

Players do not need Fabric, Forge, Wynntils, BetterHUD, or another client mod. They only need to accept the server resource pack when custom glyphs or the waypoint shaders are used.
