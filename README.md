# Animated Inventory

Animated Inventory makes item movement in Minecraft inventories visible instead of instantaneous.

When an item is picked up, shift-clicked, merged, split, crafted, sorted, equipped, or moved between containers, the mod animates a visual copy from the source to the destination while the real inventory state continues to use Minecraft's normal menus.

It is a client-side NeoForge 1.21.1 mod. A server installation is not required.

## Features

- Animated pickup, placement, quick-move, merges, splits, swaps, sorting, equipment changes, and external inventory updates.
- Crafting ingredients travel into the crafted item for 2x2 and 3x3 recipes, including shift crafting and returned containers.
- Linear, smooth, arc, spring, and snap-smooth movement styles with configurable easing.
- Cursor movement can either follow the cursor or travel toward a fixed pickup/drop point.
- Smooth hotbar selection movement, including rapid retargeting.
- Fade, scale, fade+scale, and slide effects when container screens open or close.
- Configurable animation speed and reduced-motion mode.
- A provider API for custom screens or inventories that do not use ordinary Minecraft `Slot` positions.

The animation is presentation only. Click targets, tooltips, and the actual inventory/menu logic stay at their normal logical positions.

## Compatibility

Animated Inventory has dedicated handling for a few inventory mods where the visible slot layout does not map cleanly to vanilla menus.

### Bundled Not Siloed

The normal BNS player grid animates through its real slot renderer. Transfers to off-page stowed storage travel toward the appropriate edge/column and fade out; retrieving a stowed item enters from the lower edge.

BNS's aggregate search renderer stays under BNS control rather than being forced through Animated Inventory's normal slot path.

### Sophisticated Backpacks / Storage

Normal backpack/storage transfers, scrolling, large-stack displays, upgrade slots, and crafting-upgrade ingredient flow are supported through an optional adapter.

### JEI / EMI

Recipe filling can animate ingredients from the slots or BNS storage they actually came from. The recipe viewer itself is not replaced.

### Inventory Particles

The two mods can coexist. Animated Inventory avoids taking over effects that Inventory Particles needs to render itself, especially for partial transfers where both systems would otherwise try to own the same visual item.

See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) for the exact compatibility behavior and known limits.

## Configuration

Use:

**Mods → Animated Inventory → Config**

or edit:

```text
config/animatedinventory-client.toml
```

The configuration covers animation duration, trajectory/easing, screen effects, hover/press/merge emphasis, hotbar motion, reduced motion, compatibility behavior, and debug options.

Full reference: [docs/CONFIG.md](docs/CONFIG.md).

## Resources

Animated Inventory does not replace Minecraft's item models or add a custom GUI skin.

Moving items still use their normal models, glint, durability/count decorations, resource-pack overrides, hotbar selection sprite, and slot highlighting.

## API

Mods with custom inventory layouts can provide logical item positions through the screen-space provider API rather than pretending to have vanilla slots.

The API supports destination queries, clipping, explicit transactions, layout reflow, and custom logical IDs.

See [docs/API.md](docs/API.md) for examples.

## Documentation

- [Configuration](docs/CONFIG.md)
- [Integration API](docs/API.md)
- [Compatibility notes](docs/COMPATIBILITY.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Testing and QA](docs/VALIDATION.md)

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.244 or newer compatible 21.1 build
- Java 21 for development

Install the mod in the **client's** `mods` directory.

## Building

```powershell
.\gradlew.bat test build
.\gradlew.bat runClient
```

The release jar is written to `build/libs/`.

The optional client-validation source set uses disposable development worlds and is not included in published artifacts.

## License

Animated Inventory is licensed under [CC BY-NC-SA 4.0 with a Modpack/Server Exception](LICENSE). Modpacks and Minecraft servers, including monetized ones, may use it under the additional permission in the LICENSE.
