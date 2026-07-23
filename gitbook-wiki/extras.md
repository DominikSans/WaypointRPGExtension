# Extras

This page covers small features that are useful while configuring waypoint visuals.

## Direction arrows

`{direction}` uses Typewriter snippets.

The defaults are written to `plugins/Typewriter/snippets.yml` the first time they are used.

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

After editing:

```txt
/tw reload
```

You can use normal Unicode arrows or glyphs from a resource pack.

## Custom icon glyphs

The icon uses MiniMessage, so custom fonts work normally.

```txt
<white><font:my_pack:icons>★</font></white>
```

Put that value in:

```txt
symbol.text
```

## Text placeholders

Common placeholders for `label.text`:

| Placeholder | Shows |
|---|---|
| `{name}` | Target name. |
| `{distance}` | Distance to target. |
| `{direction}` | Direction arrow. |
| `{index}` | Current target number. |
| `{total}` | Total visible targets. |
| `{target_type}` | Target type. |
| `{entity_name}` | Entity name. |
| `{entity_type}` | Entity type. |
| `{route_index}` | Current route point number. |
| `{route_total}` | Total route points. |
| `{route_name}` | Current route point name. |
| `{route_remaining}` | Remaining route points. |

## PlaceholderAPI direction placeholders

The extension also exposes direction placeholders through Typewriter/PAPI format:

```txt
%typewriter_<id>:direction:up%
%typewriter_<id>:direction:north%
%typewriter_<id>:direction:northeast%
%typewriter_<id>:direction:east%
%typewriter_<id>:direction:southeast%
%typewriter_<id>:direction:south%
%typewriter_<id>:direction:southwest%
%typewriter_<id>:direction:west%
%typewriter_<id>:direction:northwest%
%typewriter_<id>:direction:down%
```

Replace `<id>` with the Typewriter entry id.

