# Archived `tracked_locatable_waypoint` V2 renderer

`tracked_locatable_waypoint` was removed from Typewriter registration when
`static_waypoint` became the single supported visual waypoint entry.

The historical implementation remains in
`src/main/kotlin/cg/headpop/waypointrpg/TrackedLocatableWaypointEntry.kt` because
that file also contains shared target, route, direction, and entity-resolution
types. The class intentionally has no `@Entry` annotation and therefore is not
published in `extension.json`, shown in the panel, or instantiated at runtime.

Features migrated to `static_waypoint`:

- route advancement and shared route progress;
- Typewriter direction placeholders;
- rotating inner beam core;
- private Bukkit-entity glow;
- automatic multi-target lane separation;
- vertical direction glyphs;
- dynamic beam height and near-target thinning.

Features intentionally left archived:

- FOV fading and user-configurable depth flags;
- packet-only text and beam renderer;
- configurable follow ranges and performance policies;
- manual billboard/alignment controls;
- V2 diagnostic packet counters.

Pages containing `type: "tracked_locatable_waypoint"` must be migrated to
`type: "static_waypoint"`; the legacy ID no longer resolves at runtime.
