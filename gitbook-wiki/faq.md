# FAQ

## Do players need a client mod?

No. The extension is server-side.

## Where do I install the jar?

Place it in:

```txt
plugins/Typewriter/extensions/
```

## The beam is not visible.

Check that `beam.outer` and `beam.inner` are valid block materials.

## The text disappears near the waypoint.

Check `label.hideRange` and `general.hideOnArrive`.

## `{direction}` does not show the arrow I want.

Edit `plugins/Typewriter/snippets.yml` and reload Typewriter.

## Can I use custom font icons?

Yes. Put the glyph in `symbol.text` using MiniMessage and your resource pack font.

Example:

```txt
<white><font:my_pack:icons>★</font></white>
```

## Why does it feel different from a Fabric mod?

The visuals are controlled by the server. A client mod can predict and render locally; a server-side plugin cannot fully do that.

## Does BetterHUD come included?

No. BetterHUD is optional and must be installed separately.

