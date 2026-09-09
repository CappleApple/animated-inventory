# Animated Inventory

By [CappleApple](https://github.com/CappleApple).

[Download the latest release](https://github.com/CappleApple/animated-inventory/releases/latest) | [Report an issue](https://github.com/CappleApple/animated-inventory/issues)

An independently developed inventory animation system for **Minecraft 1.21.1 / NeoForge 21.1.244+**, written from scratch under `com.cappleapple.animatedinventory`. MIT licensed. Install `animatedinventory-1.0.6.jar` in the client's `mods` directory. A server installation and networking channel are not required.

Animated Inventory animates visual representations after inventory state changes. It never postpones inventory transactions, replaces inventory contents, or simulates clicks.

## Implemented

- Count-aware movement for pickup, placement, quick move, merges, splits, swaps, sorting, equipment and external inventory updates.
- Consumed ingredients flow into the crafted item in 2x2 and 3x3 grids, including shift-click crafting and recipes with returned containers.
- Linear, smooth, arc, spring and snap-smooth trajectories; eleven reusable easing functions.
- Cursor pickup with fixed or following destination; composable hover, press and merge emphasis.
- Smooth hotbar selection, including continuous retargeting during rapid changes.
- **Vanilla white slot highlights render behind the item and its decorations.** Logical hover, clicks and tooltips keep their normal coordinates.
- Opening and closing effects: fade, scale, combined fade/scale and slide. Closing retains an image rather than a removed Screen.
- A screen-space provider API with arbitrary logical IDs, destination queries, clipping, explicit transactions and layout reflow.
- Animation ownership with partial-count suppression, caps, interruption policies and safe cancellation.
- Client configuration, global speed, reduced motion and NeoForge's built-in Minecraft-style configuration screen.
- Sophisticated Backpacks/Storage movement, large-stack rendering, scrolling and crafting-upgrade ingredient flow.
- Optional BNS, Inventory Particles and recipe-viewer compatibility policies, plus diagnostic bounds and metrics.

JEI/EMI recipe filling animates ingredients from their visible source slots or BNS stash, including when returning from a recipe viewer. BNS quick transfers wait for authoritative destination updates before animating, avoiding an intermediate inventory-slot hop.

## Build and run

Use JDK 21:

```powershell
.\gradlew.bat test build
.\gradlew.bat runClient
```

The installable jar is `build/libs/animatedinventory-1.0.6.jar`. The companion sources jar is for developers.

```powershell
.\gradlew.bat runClient -PclientValidation
```

The optional validation source set creates its own flat world in `run-validation`, exercises container screen and menu fixtures, records screenshots and assertions, then shuts down. It is excluded from both published artifacts.

## Configuration and integration

Use **Mods → Animated Inventory → Config**, or edit `config/animatedinventory-client.toml`.

- [Complete configuration reference](docs/CONFIG.md)
- [Architecture and render hooks](docs/ARCHITECTURE.md)
- [Integration API and example provider](docs/API.md)
- [Compatibility and safe fallbacks](docs/COMPATIBILITY.md)
- [Validation evidence and remaining manual checks](docs/VALIDATION.md)

BNS's normal main inventory grid animates through the native Slot renderer, including transfers within the grid and to/from the hotbar or containers. Transfers into off-page stowed slots move toward the corresponding grid edge and destination column, then fade out. Items retrieved from the stash rise from the lower grid edge, including category changes and sorting. Page changes reset old animations. Its separate aggregate search renderer retains normal BNS rendering; see the compatibility document.

Sophisticated Backpacks and Storage share an optional adapter for their custom screen and additional upgrade slots. Normal transfers animate while preserving native count labels. Visible crafting-upgrade ingredients flow toward the taken product; filtering and scrolling reset old geometry, and ghost filters retain normal rendering.

With Inventory Particles, complete moves transform the existing native render. Known-source partial transfers and double-click collection use separate model copies, and offscreen screen effects are disabled to preserve particle rendering assumptions. Particle origins remain logical unless an effects integration queries this mod's coordinate API.

Screen image effects are intentionally limited to vanilla container screen classes. Custom screens continue normal rendering and can use the provider API. Built-in recipe-book widgets and third-party overlays outside the base container render remain stationary.

## Resources

The mod introduces no custom GUI textures. Item models, glint, durability/count decorations, the hotbar selection sprite and slot highlighting use Minecraft/NeoForge rendering. Existing resource-pack overrides continue to apply.
