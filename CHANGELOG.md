# Changelog

## 1.1.1 - 2026-09-20

### Fixed

- Load the mod's language resources on Fabric without requiring a separate Fabric API installation.
- Show translated configuration labels, choices, tooltips and validation errors.
- Restore defaults for missing or invalid JSON settings instead of retaining stale values.

### Added

- Boolean toggles, named choice buttons and a reset button for each configuration page.

## 1.1 - 2026-09-19

### Added

- Ported Animated Inventory to Minecraft 26.2 Fabric.
- Added a Fabric configuration editor opened with F8.

## 1.0.7 - 2026-09-18

### Fixed
- Prevent existing items from playing arrival animations when a vanilla container first synchronizes after opening.

## 1.0.6 - 2026-09-09

### Fixed
- Prevent shift-clicked items from briefly appearing in the visible TrashSlot widget.
- Keep independent slot widgets from inheriting inventory animations or replacing real slot rendering ownership.

## 1.0.5 — 2026-09-09

### Added
- Items retrieved from BNS stash rise into the inventory when sorting or changing categories.
- JEI/EMI recipe filling animates ingredients from visible slots and the BNS stash into 2x2 and 3x3 crafting grids.

### Fixed
- Animate every contributing stack during double-click collection, including partial donors with Inventory Particles installed.
- BNS hotbar-first transfers travel directly from the original source to the confirmed destination.
- Preserve BNS/Sophisticated transfers across separate inventory updates and repeated unchanged slot-permission packets.

## 1.0.4 — 2026-09-09

### Added
- Item movement in Sophisticated Backpacks and Sophisticated Storage, including player transfers, large stacks and visible slots in scrolled inventories.
- Ingredient flow when taking or shift-crafting results from Sophisticated crafting upgrades.

### Fixed
- Keep Sophisticated's large-count labels and slot decorations in their own rendering path.
- Exclude ghost filters, infinite slots, inaccessible cells and replaced slot renderers from transfer ownership.
- Clear old animations when Sophisticated filtering, scrolling, upgrade layouts or slot permissions change.
- Render Sophisticated's normal hover highlight behind items.

## 1.0.3 — 2026-09-09

### Added
- Consumed ingredients flow into the crafted item when taking a 2x2 or 3x3 crafting result, including shift-click crafting.
- Configurable ingredient animation duration and an option to disable the effect.

### Fixed
- Preserve crafted-item travel when the result preview immediately refills, while keeping leftover ingredients and returned buckets visible.

## 1.0.2 — 2026-09-09

### Added
- Track transfers into BNS's stowed slots toward the grid edge and destination column, with a fade on arrival.
- Preserve separate moved quantities when a transfer fills multiple stowed slots.

## 1.0.1 — 2026-09-09

### Fixed
- Restored animations in BNS's normal main inventory grid and transfers to/from its hotbar and containers.
- Clear old animations when BNS changes its acknowledged inventory page.

## 1.0.0 — 2026-09-09

### Added
- Original screen-space inventory animation engine and public integration API.
- Item movement, count-aware splitting/merging, cursor transitions, equipment movement and layout reflow.
- Hover, click and merge emphasis, smooth hotbar selection and configurable screen transitions.
- Client configuration screen, reduced motion, animation limits and debug overlay.
- Guarded Bundled Not Siloed, Inventory Particles and recipe-viewer integration policies.

### Changed
- White slot hover highlights render behind items and their decorations.
