# Testing and QA

This file describes the current test setup for Animated Inventory and the scenarios that are worth rerunning when inventory/rendering behavior changes.

Raw logs and screenshots are kept under `docs/evidence/` and `docs/images/` for debugging regressions. They are supporting artifacts, not a claim that every mod/menu combination has been exhaustively tested.

## Automated tests

Run with Java 21:

```powershell
.\gradlew.bat test build
```

The unit suite covers the animation bookkeeping that is easiest to break without noticing visually:

- easing and trajectory endpoints;
- item/count/component matching;
- partial merges and redistribution;
- interruption/cancellation;
- animation ownership and cleanup;
- virtual/off-page destinations;
- crafting ingredient consumption/returned containers;
- BNS stowed-slot routing; and
- Sophisticated crafting/transfer edge cases.

The normal build also verifies that validation fixtures and optional-mod implementation classes are not bundled into the release jar.

## Client validation

The opt-in client fixture runs in a disposable development world:

```powershell
.\gradlew.bat runClient -PclientValidation
```

Additional flags enable focused compatibility scenarios. Current fixtures cover normal player/container movement, crafting, BNS, Sophisticated Backpacks/Storage, recipe viewers, Inventory Particles, and TrashSlot regressions.

The client checks are meant to answer two things:

1. Did the real menu/inventory state change correctly?
2. Did exactly the intended visual copy own the moving item during the animation?

That distinction matters because an animation can look correct while temporarily hiding or duplicating the wrong slot.

## Core manual scenarios

Before a release that changes movement/render ownership, check at least:

- pickup and placement between player/container slots;
- shift-click in both directions;
- split stacks and partial merges;
- double-click collection;
- number-key/hotbar swaps;
- closing/reopening a screen while motion is active;
- screen resize/re-init during an animation;
- reduced-motion mode; and
- crafting with both normal ingredients and returned containers.

Useful baseline screenshots and assertion logs are under `docs/images/` and `docs/evidence/`.

## Bundled Not Siloed

BNS needs separate QA because some storage positions are not currently visible as ordinary slots.

Check:

- main-grid ↔ hotbar/container movement;
- transfers into off-page stowed storage;
- retrieval from the stash;
- category/sort changes that replace the visible page;
- split arrivals into more than one destination; and
- recipe filling from a mix of visible and stowed items.

Old animations should be released when the acknowledged BNS page changes rather than continuing toward stale coordinates.

## Sophisticated Backpacks / Storage

Use actual backpack/storage screens rather than a mock menu. Important cases include:

- normal and upgraded storage;
- scrolling/filtering;
- large stack counts;
- upgrade slots;
- crafting upgrades; and
- closing or changing the visible layout during travel.

The production integration is optional, so vanilla/client startup should also be tested with Sophisticated absent.

## JEI / EMI

Recipe-fill checks should verify both the moved quantities and the source locations. Filling the recipe grid should animate placement only; actual ingredient consumption belongs to the later crafting action.

Because EMI can take over JEI recipe presentation in a combined install, JEI should also be checked in a profile without EMI.

## Inventory Particles

When Inventory Particles is present, check that the two mods do not both suppress/render the same moving stack. Partial merges are especially useful because the mods may intentionally use different fallback paths there.

Particle positions themselves are owned by Inventory Particles unless it explicitly asks Animated Inventory for coordinates.

## TrashSlot regression

The TrashSlot regression that motivated 1.0.6 came from granting animation rendering ownership based on the wrong menu slot.

When changing slot lookup/ownership logic, rerun the TrashSlot profile and confirm:

- the visible trash slot keeps its own retained stack;
- slot zero is not accidentally substituted for the custom slot;
- pending animations do not transform the trash renderer; and
- undo/return visuals remain attached to the correct slot.

## Dedicated-server smoke check

Animated Inventory is client-side, but a dedicated NeoForge server smoke run is still useful for catching accidental client-class loading.

The server should discover the mod metadata, reach `Done`, save, and shut down without loading client mixins/entrypoints.

## Known manual gaps

Some behavior remains better suited to real gameplay than the disposable fixtures:

- high-latency remote multiplayer timing;
- arbitrary third-party menus/recipe handlers;
- rapid physical drag/drop/key-input combinations;
- unusual shader/resource-pack/model combinations; and
- long-running performance under many simultaneous animations.

Treat a passing fixture as regression coverage for the cases it exercises, not as blanket compatibility certification for every inventory mod.
