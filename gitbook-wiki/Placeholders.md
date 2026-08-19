# Placeholders

## Label placeholders

`static_waypoint` currently replaces these values in both `label.text` and `symbol.text`:

| Placeholder | Shows |
|---|---|
| `{name}` | Objective, route point, or entity display name. |
| `{distance}` | Distance to the target. |
| `{direction}` | Direction glyph. |
| `{index}` | One-based target index. |
| `{total}` | Total visible targets. |

Server reference:

```text
<white>{name}</white><newline>{distance}<newline><white>{direction}</white>
```

## Direction arrows

`{direction}` uses Typewriter snippets:

```yaml
waypoint:
  direction:
    up: "▲"
    down: "▼"
    north: "↑"
    northeast: "↗"
    east: "→"
    southeast: "↘"
    south: "↓"
    southwest: "↙"
    west: "←"
    northwest: "↖"
```

Reload Typewriter after changing snippets.

The entry also exposes direction values through its Typewriter placeholder parser:

```text
%typewriter_<entry_id>:direction:up%
%typewriter_<entry_id>:direction:north%
%typewriter_<entry_id>:direction:northeast%
%typewriter_<entry_id>:direction:east%
%typewriter_<entry_id>:direction:southeast%
%typewriter_<entry_id>:direction:south%
%typewriter_<entry_id>:direction:southwest%
%typewriter_<entry_id>:direction:west%
%typewriter_<entry_id>:direction:northwest%
%typewriter_<entry_id>:direction:down%
```

## Custom symbol glyphs

The headpop server reference uses:

```text
<white><font:images_rpg:emojis>佚</font></white>
```

This requires the matching `images_rpg:emojis` font in the final resource pack. A portable fallback is:

```text
<gold>◆</gold>
```

Use `symbol.shadow` to control glyph shadow separately from `label.shadow`.

## Resource-pack and animated symbols

`symbol.text` and a theme's symbol text remain MiniMessage rendered by a `TextDisplay`.
They can use a custom font glyph or an image tag supplied by another resource-pack
system, for example:

```text
<image:waypoints:quest_tracked>
```

If the final resource pack defines that image with a valid animated texture and
`.png.mcmeta`, Minecraft controls the texture frames. The extension's one-shot focus
acquisition only changes display scale and does not replace or restart that animation.

This is not an item model. `itemMaterial`, `customModelData`, `itemModel`, and
`itemScale` are intentionally absent because ItemDisplay symbols cannot use the same
protected TextDisplay see-through behavior.

## BetterHUD icon IDs

`waypoint_theme.betterHudIcon` is a BetterHUD `custom-icon` ID, not the MiniMessage
content used by `symbol.text`. If it is blank, `waypoint_betterhud_bridge.iconName` is
used as the fallback.
