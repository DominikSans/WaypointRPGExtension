# Entry Configuration

This page lists the configurable fields for the three public entries.

## `tracked_locatable_waypoint`

Main entry for waypoint visuals.

### `general`

| Field | Type | Default | Description |
|---|---|---:|---|
| `mode` | `WaypointType` | `BOTH` | Visual mode: `HOLOGRAM`, `BEAM`, or `BOTH`. |
| `selection` | `WaypointTargetSelection` | `HIGHEST_PRIORITY` | Which target is shown first. |
| `maxTargets` | `Int` | `5` | Maximum targets shown at once. `0` hides all. |
| `arriveRadius` | `Double` | `1.5` | 3D distance required to count as arrived. |
| `hideOnArrive` | `Boolean` | `true` | Hides beam and text on arrival. Icon stays visible. |

### `target`

| Field | Type | Default | Description |
|---|---|---:|---|
| `offset` | `Double` | `0.0` | Y offset above the objective. Use around `2.0` for NPC head height. |
| `verticalThreshold` | `Double` | `10.0` | Y difference that switches to vertical mode and up/down arrows. |

### `label`

| Field | Type | Default | Description |
|---|---|---:|---|
| `text` | `Var<String>` | `<white>{name}</white><newline><gold>{distance}</gold>` | MiniMessage text. Supports waypoint placeholders. |
| `useObjectiveName` | `Boolean` | `true` | Uses the objective display name for `{name}`. |
| `height` | `Double` | `1.0` | Height above the player's eye level. |
| `floatDist` | `Double` | `5.0` | Distance from camera to text. |
| `hideRange` | `Double` | `8.0` | Hides text near the target. |
| `fov` | `Double` | `55.0` | Fades text outside this look angle. `0` keeps it visible. |
| `scale` | `Float` | `1.0` | Text scale. |
| `billboard` | `BillboardMode` | `CENTER` | Direction the text faces: `CENTER`, `VERTICAL`, `HORIZONTAL`, `FIXED`. |
| `align` | `TextAlignMode` | `CENTER` | Text alignment: `CENTER`, `LEFT`, `RIGHT`. |
| `background` | `Boolean` | `true` | Shows a background panel behind text. |
| `bgColor` | `String` | `#80000000` | Background color in `#AARRGGBB` format. |
| `opacity` | `Int` | `255` | Text opacity from `0` to `255`. |
| `shadow` | `Boolean` | `true` | Adds text shadow. |
| `seeThrough` | `Boolean` | `false` | Reserved; no visible effect in this version. |
| `lineWidth` | `Int` | `255` | Text wrap width in pixels. |
| `multiOffset` | `Double` | `0.35` | Side gap when multiple targets are visible. |

### `symbol`

| Field | Type | Default | Description |
|---|---|---:|---|
| `enabled` | `Boolean` | `true` | Shows the icon. |
| `text` | `Var<String>` | `<gold>◆</gold>` | MiniMessage icon text. Custom font glyphs are supported. |
| `minScale` | `Float` | `3.0` | Icon scale near the waypoint. |
| `maxScale` | `Float` | `5.0` | Icon scale far from the waypoint. |
| `nearDist` | `Double` | `5.0` | Starts growing the icon after this distance. |
| `farDist` | `Double` | `150.0` | Reaches full icon size at this distance. |
| `offset` | `Double` | `0.5` | Y offset above the text while following the player. |
| `snapRange` | `Double` | `8.0` | Snaps icon to the waypoint inside this range. |
| `snapLeave` | `Double` | `12.0` | Leaves snap mode outside this range. Must be greater than `snapRange`. |
| `snapHeight` | `Double` | `3.0` | Y height above the waypoint while snapped or arrived. |

### `beam`

| Field | Type | Default | Description |
|---|---|---:|---|
| `enabled` | `Boolean` | `true` | Shows the vertical beam. |
| `fullBright` | `Boolean` | `true` | Renders the beam at full brightness. |
| `rotateLikeBeacon` | `Boolean` | `false` | Spins the inner beam core. Outer layer stays fixed. |
| `outer` | `Material` | `LIME_STAINED_GLASS` | Outer beam material. Must be a block. |
| `inner` | `Material` | `LIME_CONCRETE` | Inner beam material. Must be a block. |
| `width` | `Float` | `0.5` | Outer layer width. |
| `coreWidth` | `Float` | `0.25` | Inner layer width. |
| `depth` | `Float` | `0.5` | Outer layer depth. |
| `coreDepth` | `Float` | `0.25` | Inner layer depth. |
| `height` | `Float` | `150.0` | Beam height in blocks. |
| `dynamicHeight` | `Boolean` | `true` | Extends the beam to cover height differences between player and target. |
| `follow` | `WaypointBeamFollowConfig` | object | Beam follow behavior. |
| `fadeStart` | `Double` | `10.0` | Starts fading near the objective. |
| `fadeEnd` | `Double` | `3.0` | Fully hidden at this distance. |

### `beam.follow`

| Field | Type | Default | Description |
|---|---|---:|---|
| `staticRange` | `Double` | `30.0` | Keeps the beam on the waypoint inside this distance. |
| `followRange` | `Double` | `60.0` | Moves the beam with the player past this distance. |
| `followDist` | `Double` | `55.0` | Distance ahead of the player in follow mode. |

### `bob`

| Field | Type | Default | Description |
|---|---|---:|---|
| `enabled` | `Boolean` | `true` | Enables float motion on text and icon. |
| `height` | `Double` | `0.06` | Motion range in blocks. |
| `speed` | `Double` | `1.2` | Cycles per second. |

### `routes`

List of `WaypointRoute` objects.

| Field | Type | Default | Description |
|---|---|---:|---|
| `objectiveId` | `String` | empty | Objective ID this route applies to. |
| `routeId` | `String` | empty | Shared key for route progress. Blank uses `objectiveId`. |
| `allowSkip` | `Boolean` | `true` | Allows advancing by passing any route point. |
| `resetOnObjectiveChange` | `Boolean` | `true` | Resets progress when the objective deactivates. |
| `resetOnComplete` | `Boolean` | `false` | Loops to the first route point after the last point. |
| `points` | `List<WaypointRoutePoint>` | empty | Ordered points in the path. |

### `routes.points`

| Field | Type | Default | Description |
|---|---|---:|---|
| `name` | `Var<String>` | empty | Label for the route point. Blank uses objective name. |
| `position` | `Var<Position>` | origin | World position. |
| `radius` | `Double` | `3.0` | Arrival radius to advance to the next point. |

### `integrations`

| Field | Type | Default | Description |
|---|---|---:|---|
| `entityTargets` | `List<EntityWaypointTarget>` | empty | Moving entity or NPC targets. |
| `entityGlow` | `Boolean` | `false` | Shows glow outline on entity targets. |
| `glowRange` | `Double` | `20.0` | Distance where glow activates. |

### `integrations.entityTargets`

| Field | Type | Default | Description |
|---|---|---:|---|
| `targetType` | `EntityTargetType` | `UUID` | Lookup type: `UUID`, `NAME`, `SCOREBOARD_TAG`, `TYPEWRITER_NPC`. |
| `uuid` | `String` | empty | Entity UUID for `UUID` mode. |
| `name` | `String` | empty | Entity name for `NAME` mode. |
| `tag` | `String` | empty | Scoreboard tag for `SCOREBOARD_TAG` mode. |
| `displayName` | `Var<String>` | empty | Label override. Blank uses entity name. |
| `maxDistance` | `Double` | `128.0` | Search radius for name and tag lookups. |
| `priority` | `Int` | `0` | Higher priority is shown first with `HIGHEST_PRIORITY`. |
| `npcEntryId` | `String` | empty | Typewriter NPC entry ID for `TYPEWRITER_NPC` mode. |

### `performance`

| Field | Type | Default | Description |
|---|---|---:|---|
| `lazyUpdate` | `Boolean` | `false` | Skips visual updates when nothing moves. Disable if float motion is on. |
| `cleanupOnJoin` | `Boolean` | `false` | Removes orphaned display entities on player join. |
| `cleanupRadius` | `Double` | `50.0` | Search radius for cleanup. |

## `waypoint_zone_trigger`

Runs Typewriter triggers when the player enters or leaves waypoint target areas.

| Field | Type | Default | Description |
|---|---|---:|---|
| `radius` | `Double` | `5.0` | Detection radius in blocks. |
| `targetMode` | `ZoneTriggerTargetMode` | `ANY_ACTIVE_TARGET` | Targets to monitor: `OBJECTIVES_ONLY`, `ANY_ACTIVE_TARGET`, `ACTIVE_ROUTE_POINT`. |
| `maxTargets` | `Int` | `5` | Maximum targets checked at once. |
| `selection` | `WaypointTargetSelection` | `CLOSEST` | Sort order before trimming to `maxTargets`. |
| `triggerPerTarget` | `Boolean` | `false` | Fires separate events for each target. |
| `triggerOnce` | `Boolean` | `false` | Fires once and waits until the player leaves. |
| `resetOnExit` | `Boolean` | `true` | Re-arms `triggerOnce` after exit. |
| `entityTargets` | `List<EntityWaypointTarget>` | empty | Entity targets to monitor. |
| `routes` | `List<WaypointRoute>` | empty | Route config for active route point mode. |
| `onEnter` | `TriggerableEntry` | empty | Trigger called on zone entry. |
| `onExit` | `TriggerableEntry` | empty | Trigger called on zone exit or target disappearance. |

## `waypoint_betterhud_bridge`

Sends active waypoint targets to a BetterHUD compass element.

| Field | Type | Default | Description |
|---|---|---:|---|
| `iconName` | `String` | `default` | BetterHUD compass element name. |
| `pointNamePrefix` | `String` | `waypoint_` | Prefix for generated compass point IDs. Entry ID is appended. |
| `targetMode` | `BetterHudTargetMode` | `ANY_ACTIVE_TARGET` | Targets to sync: `OBJECTIVES_ONLY`, `ENTITIES_ONLY`, `ANY_ACTIVE_TARGET`. |
| `maxTargets` | `Int` | `5` | Maximum compass points sent at once. |
| `selection` | `WaypointTargetSelection` | `CLOSEST` | Sort order before trimming to `maxTargets`. |
| `arriveRadius` | `Double` | `0.0` | Hides compass point inside this distance. `0` always shows. |
| `entityTargets` | `List<EntityWaypointTarget>` | empty | Entity or NPC targets to include. |
| `routes` | `List<WaypointRoute>` | empty | Route config for showing route points instead of final objective. |

