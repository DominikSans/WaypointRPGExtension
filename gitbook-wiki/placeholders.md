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
