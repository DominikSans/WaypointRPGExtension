# Entry list

v3.0.3 registers seven public entries. Boolean controls appear first inside each configuration group.

## `static_waypoint`

The main visual entry, displayed as **Waypoint** in the Typewriter panel.

### `general`

| Field | Type | Default | Description |
|---|---|---:|---|
| `hideOnArrive` | Boolean | `true` | Hides label and beam after arrival; the snapped symbol can remain. |
| `selection` | Enum | `HIGHEST_PRIORITY` | Uses `HIGHEST_PRIORITY` or `CLOSEST`. |
| `maxTargets` | Integer | `5` | Maximum simultaneous targets, clamped to 16. |
| `arriveRadius` | Number | `1.5` | Distance that counts as arrival. |

### `target`

| Field | Type | Default | Description |
|---|---|---:|---|
| `offset` | Number | `0.0` | Vertical offset added to the target position. |

### `label`

| Field | Type | Default | Description |
|---|---|---:|---|
| `shadow` | Boolean | `true` | Draws label text shadow. |
| `text` | Var<String> | name + distance | MiniMessage template. |
| `height` | Number | `1.0` | Height above the target anchor. |
| `floatDist` | Number | `5.0` | Compatibility value retained from V3; v3.0.3 movement uses its internal 70–72 block transition. |
| `hideRange` | Number | `8.0` | Hides label text inside this distance. |
| `minScale` | Float | `1.0` | Label scale at `nearDist`. |
| `maxScale` | Float | `8.0` | Label scale at `farDist`. |
| `nearDist` | Number | `5.0` | Start of scale interpolation. |
| `farDist` | Number | `50.0` | End of scale interpolation. |
| `opacity` | Integer | `255` | Text opacity from 0 to 255. |
| `lineWidth` | Integer | `255` | Text wrapping width. |

Through-wall rendering, shader correction, and smart occlusion are hardcoded safeguards. Every four ticks, label and symbol switch to the protected see-through shader pass when blocks obstruct the view; while unobstructed they use the regular depth-writing pass. Occlusion samples stable, non-bobbing centers for label and symbol. This prevents the projected edge of a highly scaled label from producing a false wall hit against distant terrain when the camera is near ground level. These controls are intentionally absent from the panel.

Their billboard mode is hardcoded to `VERTICAL` with centered text alignment and entity pitch normalized to `0°`, matching a yaw-only hologram: label and symbol turn laterally toward the player but remain upright.

### `symbol`

| Field | Type | Default | Description |
|---|---|---:|---|
| `enabled` | Boolean | `true` | Shows the symbol. |
| `shadow` | Boolean | `false` | Draws symbol shadow independently from label shadow. |
| `text` | Var<String> | `<gold>◆</gold>` | MiniMessage symbol or custom glyph. |
| `minScale` | Float | `3.0` | Near/snapped scale. |
| `maxScale` | Float | `25.0` | Far scale. |
| `nearDist` | Number | `5.0` | Start of scale interpolation. |
| `farDist` | Number | `150.0` | End of scale interpolation. |
| `offset` | Number | `0.5` | Height above the label. |
| `snapRange` | Number | `8.0` | Distance at which snap mode starts. |
| `snapLeave` | Number | `12.0` | Distance at which snap mode ends. Keep it above `snapRange`. |
| `snapHeight` | Number | `3.0` | Height of the snapped symbol. |
| `scaleSpacing` | Number | `0.16` | Extra vertical spacing based on symbol scale. |

When a target becomes newly tracked, the configured symbol image performs a progressive three-stage focus lock (`1.90x` -> `1.55x` -> `1.25x` -> `1.00x`) over 11 ticks (0.55 seconds at 20 TPS). Each stage continues inward and pauses for one tick before the next one, so it reads as an acquisition effect instead of a bounce. The same stable V3 display is used from beginning to end, avoiding entity handoff and texture-frame changes. Normal updates, bob movement, and visibility changes do not restart it.

A separate white focus-marker layer was evaluated but is intentionally not rendered. It remains a possible future visual option if a dedicated texture is added instead of using a text glyph.

### `beam`

| Field | Type | Default | Description |
|---|---|---:|---|
| `enabled` | Boolean | `true` | Shows the private two-layer beam. |
| `fullBright` | Boolean | `true` | Uses maximum block and sky brightness. |
| `rotateInner` | Boolean | `false` | Rotates only the inner core. |
| `outer` | Material | `LIME_STAINED_GLASS` | Outer block material. |
| `inner` | Material | `LIME_CONCRETE` | Inner block material. |
| `width` | Float | `0.5` | Outer width. |
| `depth` | Float | `0.5` | Outer depth. |
| `coreWidth` | Float | `0.25` | Inner width. |
| `coreDepth` | Float | `0.25` | Inner depth. |
| `height` | Float | `150.0` | Extension above player and target. |
| `depthBelow` | Float | `20.0` | Extension below player and target. |
| `labelClearance` | Number | `0.12` | Extra physical separation from label and symbol. |

Beam height, near-target thinning, rotation speed, and update cadence are internal policies.

### `bob`

| Field | Type | Default | Description |
|---|---|---:|---|
| `enabled` | Boolean | `true` | Enables vertical floating motion. |
| `height` | Number | `0.06` | Bob amplitude in blocks. |
| `speed` | Number | `1.2` | Bob cycles per second. |

## `quest_waypoint`

The normal per-quest configuration. It directly connects a selectable Quest with an
appearance; every location objective whose `quest` field points to it inherits the same
result automatically.

| Field | Type | Default | Description |
|---|---|---:|---|
| `quest` | Ref | empty | Quest selector filtered by Typewriter's `quest` tag. |
| `preset` | Enum | `INHERIT` | `PURPLE`, `GREEN`, `RED`, `GOLD`, `BLUE`, `CUSTOM`, or global inheritance. |
| `customTheme` | Ref | empty | Selectable `waypoint_theme`, used only by `CUSTOM`. |

Built-in presets change beam materials while preserving the global label and symbol.
This prevents a color preset from unexpectedly replacing a custom resource-pack image.
If duplicate profiles select one Quest, the extension logs a warning and uses the
lowest stable entry ID so reload order cannot change the result.

## `waypoint_path`

Create this entry once for each objective that needs intermediate path points. Its
`objective` field opens Typewriter's filtered `locatable_objective` selector.

| Field | Type | Default | Description |
|---|---|---:|---|
| `objective` | Ref | empty | Selectable location objective that owns the route. |
| `priority` | Integer | `0` | Highest active route wins if an objective has more than one. |
| `allowSkip` | Boolean | `true` | Allows reaching a later point to advance the route. |
| `resetOnObjectiveChange` | Boolean | `true` | Clears progress when this route leaves the player's audience. |
| `resetOnComplete` | Boolean | `false` | Loops after the final point instead of returning to the objective. |
| `points` | List | empty | Ordered route points. |

Each point has `name`, `position`, and `radius`.

An empty path has no effect and is unnecessary. Appearance is inherited through the
selected objective's Quest and its `quest_waypoint` profile.

## `waypoint_theme`

Advanced reusable manifest entry selected from `quest_waypoint.customTheme` or
`entity_waypoint.themeOverride`.

| Group | Fields | Description |
|---|---|---|
| `beam` | `overrideMaterials`, `outer`, `inner` | Replaces the global beam materials per target. |
| `beam` | `overrideRotation`, `rotateInner` | Optionally replaces the global rotation setting. |
| `beam` | `overrideFullBright`, `fullBright` | Optionally replaces global brightness. |
| `label` | `overrideText`, `text` | Optionally supplies a per-target MiniMessage label. |
| `label` | `overrideShadow`, `shadow` | Optionally supplies per-target label shadow. |
| `symbol` | `overrideText`, `text` | Optionally supplies a different glyph or image. |
| `symbol` | `overrideShadow`, `shadow` | Optionally supplies per-target symbol shadow. |

Leave an override boolean disabled to inherit that value from `static_waypoint`.

### `static_waypoint.integrations`

| Field | Type | Default | Description |
|---|---|---:|---|
| `entityGlow` | Boolean | `false` | Applies a private glow to resolved Bukkit entities. |
| `glowRange` | Number | `20.0` | Maximum private-glow distance. |

## `entity_waypoint`

This is an audience entry. It registers its target only for players currently inside
its parent audience. The `npc` field opens Typewriter's filtered
`shared_entity_instance` selector.

| Field | Description |
|---|---|
| `targetType` | `UUID`, `NAME`, `SCOREBOARD_TAG`, or `TYPEWRITER_NPC`. |
| `npc` | Selectable Typewriter shared entity used by `TYPEWRITER_NPC`. |
| `quest` | Optional owning Quest used to inherit its `quest_waypoint`. |
| `themeOverride` | Optional `waypoint_theme` with precedence over the Quest profile. |
| `uuid`, `entityName`, `scoreboardTag` | Lookup value for the selected Bukkit type. |
| `displayName` | Optional MiniMessage label override. |
| `maxDistance` | Search range for name/tag resolution. |
| `priority` | Used by `HIGHEST_PRIORITY`. |

## `waypoint_zone_trigger`

| Field | Default | Description |
|---|---:|---|
| `radius` | `5.0` | Detection radius. |
| `targetMode` | `ANY_ACTIVE_TARGET` | Objectives, all targets, or active route point. |
| `maxTargets` | `5` | Maximum targets checked. |
| `selection` | `CLOSEST` | Sort order. |
| `triggerPerTarget` | `false` | Fires separately for every target. |
| `triggerOnce` | `false` | Holds after the first enter event. |
| `resetOnExit` | `true` | Rearms after exit. |
| `onEnter` / `onExit` | empty | Typewriter trigger references. |

## `waypoint_betterhud_bridge`

| Field | Default | Description |
|---|---:|---|
| `iconName` | `default` | BetterHUD compass element name. |
| `pointNamePrefix` | `waypoint_` | Prefix for generated point IDs. |
| `targetMode` | `ANY_ACTIVE_TARGET` | Objectives, entities, or both. |
| `maxTargets` | `5` | Maximum compass points. |
| `selection` | `CLOSEST` | Sort order. |
| `arriveRadius` | `0.0` | Hides points inside this distance; zero disables hiding. |

Both integrations consume the central registry automatically. BetterHUD is optional
and accessed reflectively; its absence does not prevent v3.0.3 from loading.
