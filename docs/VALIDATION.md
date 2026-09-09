# Validation — 2026-09-09

## TrashSlot rendering isolation - 1.0.6

The isolated TrashSlot profile loads TrashSlot 21.1.11 and Balm 21.0.65 with the combined BNS/Sophisticated/Inventory Particles/JEI/EMI installation. The unchanged 1.0.5 production code reproduced the bug: the real trash slot was empty, but its pending render read returned the first menu slot's diamonds. The fix requires the actual menu Slot object before granting native rendering ownership.

**247 recorded client checks passed** after the fix. They exercise real integrated-server Sophisticated chest, backpack and vanilla chest transfers in both directions, split merges, double-click collection, stash routing, sorting/category changes and EMI recipe filling. Additional checks require the visible TrashSlot to keep its own stack, reject menu animation transforms and leave the actual slot-zero renderer record intact. Empty and retained-item captures were visually inspected. The retained undo item is supplied through TrashSlot's native client notification; deletion and undo networking are not simulated.

The standalone Sophisticated regression passed **317 checks**, including upgrade slots and crafting, with TrashSlot absent. **92 unit tests passed**, and the dedicated server reached 40 ticks, saved and shut down normally. Production artifact inspection confirmed version 1.0.6, 57 classes in the requested namespace and no test fixtures or optional-mod classes.

- [Failing reproduction before the fix](evidence/trashslot-before.txt)
- [TrashSlot and combined transfer assertions](evidence/trashslot-client.txt)
- [Sophisticated regression](evidence/trashslot-sophisticated-regression.txt)
- [Dedicated server gate](evidence/trashslot-server.txt)
- [Empty trash slot during a pending shift click](images/trashslot-pending.png)
- [Retained trash item during a return transfer](images/trashslot-retained-item.png)

Reproduce with the stated optional jars placed in `run-validation-trash/mods`:

```powershell
.\gradlew.bat test build runClient -PclientValidation -PtransferValidation -PsophisticatedValidation -PtrashValidation
```

## Synchronized transfers, stash arrivals and recipe filling — 1.0.5

The combined runtime now uses actual integrated-server menus and real client requests. It opens a Sophisticated chest, backpack and vanilla chest, then checks authoritative BNS hotbar-first routing, reverse transfers, main-grid split merges, stowed arrivals and three-donor double-click collection. It also sends actual category/sort requests and fills recipes through EMI's registered handler. **142 recorded checks passed.** The separate JEI profile opens JEI's own RecipesGui and invokes its registered BNS transfer handler; **28 checks passed**.

Both recipe viewers fill 2x2 and 3x3 recipes from a mixture of visible and stashed planks. The assertions require exact moved quantities and source IDs, preserve the result preview without claiming ingredient consumption, and check rendering after synchronization. Category changes retrieve the selected stashed item while stowing excluded items; sorting retrieves it again. Captures use 900 ms motion for inspection.

- [Synchronized transfer and EMI assertions](evidence/synchronized-transfers-client.txt)
- [JEI recipe-fill assertions](evidence/recipe-jei-client.txt)
- [Focused EMI recipe-fill assertions](evidence/recipe-emi-client.txt)
- [Every double-click donor in flight](images/double-click-collection.png)
- [Stash arrival during category switching](images/stash-arrival.png)
- [JEI recipe ingredients](images/recipe-fill-jei.png)
- [EMI recipe ingredients](images/recipe-fill-emi.png)

The final standalone Sophisticated regression passed **317 checks**, including unchanged permission packets, large stacks, scrolling and crafting upgrades. The vanilla crafting regression passed **209 checks** with optional mods absent. The final dedicated server reached 40 ticks, saved normally and shut down without loading client mixins. [Sophisticated regression](evidence/transfers-sophisticated-regression.txt), [vanilla regression](evidence/transfers-vanilla-regression.txt), [server gate](evidence/transfers-server.txt).

These exercise real packets over the integrated server connection. High-latency remote servers and arbitrary third-party recipe handlers remain separate manual checks. EMI takes over JEI's recipe interface in the combined installation, so JEI is also tested without EMI.

## Sophisticated integration — 1.0.4

The focused standalone profile uses Sophisticated Core 1.5.1.2341, Backpacks 3.26.2.2141 and Storage 1.5.91.2127. A combined profile adds the six optional mods listed below. Both open actual ordinary/diamond backpack, chest and netherite-barrel screens. They invoke Sophisticated's real overridden screen click handler, including its normal client prediction and packet send, in a disposable world with controlled menu fixtures.

The checks cover immediate real inventory state, pickup/placement, shift transfers in both directions, 16/16 split distribution, rendering ownership before and after a frame, 1,024-item transfer with a real stack upgrade, filtered and scrolled visibility, resizing, closing/return, reduced motion, ghost filters and synchronized infinite/inaccessible slot permissions. Crafting-upgrade pickup and shift crafting use its actual result-slot path; the fixture supplies the recipe result normally synchronized by the server. Server-side recipe consumption and multiplayer correction timing are not asserted by these client fixtures.

The final standalone profile passed 313 recorded checks; the combined profile passed 281. The combined profile deliberately uses Inventory Particles' existing pulse fallback for partial merges. Captured backpack, barrel, large-count and crafting-upgrade frames were visually inspected. These fixtures use 900 ms travel for clear captures.

- [Standalone Sophisticated assertions](evidence/sophisticated-client.txt)
- [Combined optional-mod assertions](evidence/sophisticated-combined-client.txt)
- [Backpack item travel](images/sophisticated-backpack.png)
- [Large stack in a netherite barrel](images/sophisticated-storage.png)
- [Crafting upgrade with BNS and Inventory Particles installed](images/sophisticated-crafting.png)

The unchanged vanilla 2x2/3x3 crafting profile also passed 209 checks with Sophisticated absent, confirming optional loading and the existing crafting path. [Vanilla regression report](evidence/sophisticated-vanilla-regression.txt).

## Crafting ingredient flow — 1.0.3

Two focused live client profiles each passed 209 recorded checks: vanilla, and the combined BNS/Stacks Not Slots/Inventory Particles/MossyLib/JEI/EMI installation listed below. The fixtures use actual client menu clicks and the real `ResultSlot.onTake` consumption hook. They supply the result preview normally synchronized by the server; these are controlled client/render tests rather than multiplayer packet tests.

Cases cover 2x2 crafting-table pickup, 2x2 log-to-planks shift crafting, 3x3 chest pickup, 3x3 iron-block shift crafting and cake with three returned buckets. Assertions verify only consumed quantities travel, the product is immediately available, remaining ingredients stay visible, travel follows the crafted item, and completion, repeated crafting, reduced motion and closing behave correctly. Recipe placement and preview regeneration alone do not trigger ingredient flow. Captured frames were visually inspected, including the BNS/Inventory Particles case. Capture durations are lengthened to show travel; the default is 160 ms.

- [Vanilla crafting assertions](evidence/crafting-client.txt)
- [Combined crafting assertions](evidence/crafting-compat-client.txt)
- [2x2 ingredient travel](images/crafting-2x2.png)
- [Cake ingredients with returned buckets](images/crafting-3x3-cake.png)
- [BNS crafting with Inventory Particles](images/crafting-bns-particles.png)

## Stowed-slot tracking — 1.0.2

The focused BNS client cases now cover transfers into off-page storage: a chest quick-move split into 12 and 20 items across two stowed slots, BNS's own stow-main-grid transaction, destination columns, edge arrival and fading, page replacement, newly visible stowed slots, reduced motion and closing during travel. The fade check inspects the actual framebuffer for the partially transparent diamond, in addition to checking elapsed animation state.

Both profiles passed 113 recorded checks: BNS + Stacks Not Slots alone, and the full Inventory Particles/MossyLib/JEI/EMI profile. These remain controlled client fixtures; multiplayer packet ordering and exact third-party particle counts are not asserted.

- [Stowed-slot client assertions](evidence/bns-stowed-client.txt)
- [Stowed-slot assertions with Inventory Particles](evidence/bns-stowed-particles-client.txt)
- [Separate quantities moving toward their columns](images/bns-stowed-split.png)
- [A stowed item fading at the grid edge](images/bns-stowed-fade.png)

## BNS regression fix — 1.0.1

The focused live client fixture uses the actual BNS inventory screen and native client menu transactions. It verifies animated travel and single render ownership both immediately and after rendering: grid pickup/placement, grid-to-hotbar swaps in both directions, grid-to-chest quick move, chest-to-grid placement, completion and closing. It also changes the acknowledged BNS page through its public data method and checks that old animations are released without false transfers. These are controlled client fixtures, not multiplayer network tests or simulated physical key presses.

Two profiles passed 61 recorded checks each: BNS 1.4.5 + Stacks Not Slots 1.0 alone, and those mods with Inventory Particles, MossyLib, JEI and EMI. They exercise overlay rendering and native inline rendering respectively. Captured frames were inspected for moving item placement.

- [BNS overlay assertions](evidence/bns-overlay-client.txt)
- [BNS with Inventory Particles assertions](evidence/bns-inline-client.txt)
- [BNS grid movement](images/bns-grid-overlay.png)
- [BNS chest transfer with Inventory Particles](images/bns-chest-inline.png)

## Build and isolated tests

- Java 21.0.12, Minecraft 1.21.1, NeoForge 21.1.244, Gradle 9.2.1.
- `gradlew.bat test build`: successful.
- 92 JUnit tests, zero failures/errors. Ten cases cover pending prediction/correction, timeout/page cleanup, donor counts, stash retrieval/fading and recipe-fill source distribution. Tests include all easing/trajectory endpoints, 30/60/144/240 FPS sampling, continuous retargeting, count/component-aware matching, 300 randomized redistribution trials, virtualized views, equipment classification, clipping, animation caps and rendering-ownership cleanup. The 13 stowed-transfer cases cover quantity conservation, hidden-only changes, components, paging, directional geometry and bounded edge fading. The 12 crafting cases cover confirmed consumption, preview regeneration, returned containers, repeated takes, split/stowed output destinations, same-item ownership, following product/cursor movement, lifecycle rejection and budgets. Six additional Sophisticated crafting cases cover result gains, existing cursor counts, batches, failed takes, existing product redistribution and changed views.
- Production jar audited: 57 classes, all under `com/cappleapple/animatedinventory`; client mixin list and version metadata verified. No optional-mod classes or validation fixtures are bundled.
- SHA-256: `aed72e393cc892d68ada9073f04db0de3e8d4b5194ceb58cdb2302f4f15ad1d3`.

## Live development client

The opt-in validation source set runs in a real NeoForge client with an NVIDIA GeForce RTX 5070 Ti, creating a disposable flat world. It operates actual vanilla client menus in controlled fixtures. These are runtime/render tests, not end-to-end multiplayer inventory tests.

The 1.0.0 baseline vanilla run passed 206 recorded checks. It exercised player inventory, chest, double chest, furnace, crafting table, hopper, barrel's chest-menu representation, shulker box, beacon, anvil, smithing table, enchanting table, merchant, horse and creative screens.

Checks include immediate pickup state, partial-merge counts, native quick-move distribution of 32 items into 12/20, screen resize/init ownership renewal, removal, return to the same screen, closing while animations are active, GL error checks, custom non-Slot provider rendering/reflow, logical versus animated bounds, completion/cancellation and reduced-motion duration limits.

Client capture evidence was visually inspected:

- [White highlight behind an item, with normal tooltip](images/highlight-behind.png).
- [A moving item supplied by a custom non-Slot provider](images/custom-provider.png).

Test durations are deliberately lengthened for capture. These screenshots do not represent the release's 90–160 ms movement defaults.

[Vanilla assertions](evidence/vanilla-client.txt)

## Combined optional-mod client

The separate compatibility profile loads:

| Mod | Version |
| --- | --- |
| Bundled Not Siloed | 1.4.5 |
| Stacks Not Slots | 1.0 |
| Inventory Particles | 3.0.0+1.21.1+neoforge |
| MossyLib | 1.5.0+1.21.1+neoforge |
| JEI | 19.53.0.426 |
| EMI | 1.1.24+1.21.1+neoforge |

The 1.0.0 baseline combined run passed 212 recorded checks across the same container fixtures with these mods installed. That baseline intentionally excluded BNS's main grid and therefore did not establish BNS movement support. The focused 1.0.1 regression tests above replace that exclusion check with actual movement and rendering-ownership assertions.

EMI's public `EmiApi.displayAllRecipes()` is used only by the optional test fixture to exercise an actual recipe screen and return. The production mod has no EMI API dependency. The native NeoForge configuration screen is also opened and rendered by the fixture. A focused final run verified the packaged category/option translations and GL state, and its screenshot was visually checked. [Configuration screenshot](images/configuration.png).

[Combined assertions](evidence/combined-client.txt) and [log excerpt](evidence/combined-client-log-excerpt.txt).

EMI reports duplicate JEI tag-recipe IDs during the combined recipe reload. This run is not a clean-log claim for the third-party recipe stack. No Animated Inventory visual-failure or mixin-application errors were observed in the completed gates.

## Dedicated server

The final 1.0.4 isolated server loaded all three Sophisticated mods, reached `Done`, passed the 40-tick validation gate, saved its dimensions and shut down normally. Animated Inventory's optional mixins remained client-only. [Sophisticated server report](evidence/sophisticated-server.txt) and [log excerpt](evidence/sophisticated-server-log-excerpt.txt).

The 1.0.3 isolated dedicated-server gate also passed: the server reached `Done`, ran 40 ticks, saved all dimensions and shut down normally with the new crafting mixin remaining client-only. [Current server result](evidence/crafting-server.txt) and [startup/shutdown excerpt](evidence/crafting-server-log-excerpt.txt).

The 1.0.0 baseline fresh isolated NeoForge dedicated server discovered the mod, reached `Done`, ran 40 ticks, saved all dimensions and shut down normally. Animated Inventory's client entrypoint and client mixins did not load. This follows NeoForge's [physical-side isolation guidance](https://docs.neoforged.net/docs/1.21.1/concepts/sides/).

[Server result](evidence/dedicated-server.txt) and [startup/shutdown excerpt](evidence/dedicated-server-log-excerpt.txt).

## Limits of the evidence

The following still require interactive gameplay validation and are not claimed as completed:

- High-latency remote multiplayer packet timing, rapid repeated sorting, number/offhand-key input, drag distribution and drop handling across arbitrary modded menus. Their inferred transitions and cleanup are covered by unit tests, not every physical input path.
- Actual BNS category editing, search/filter entry, capacity changes, long scrolling and BNS settings navigation. The focused tests cover acknowledged page replacement; search's separate aggregate renderer retains BNS rendering.
- End-to-end Sophisticated server recipe consumption/corrections, its sort/transfer buttons, nested or linked backpacks, and every specialized upgrade control. The client harness uses actual predicted result takes and native menu transitions, with controlled synchronized state.
- Exact particle counts/origins, long glint/custom-model sessions, shader/resource-pack combinations, multi-monitor/fullscreen changes and sustained high-FPS performance measurements.
- Every JEI/EMI/BNS installation permutation, third-party transfer handlers, and recipe-book/third-party overlays outside the base container render.
- Every screen effect/easing combination observed by a human. The mathematics and several real render paths have automated coverage.

## Reproduce

```powershell
.\gradlew.bat test build
.\gradlew.bat runClient -PclientValidation
.\gradlew.bat runClient -PclientValidation -PsophisticatedValidation -PcompatValidation -PtransferValidation
.\gradlew.bat runClient -PclientValidation -PjeiValidation -PstashValidation
.\gradlew.bat runClient -PclientValidation -PcompatValidation
.\gradlew.bat runClient -PclientValidation -PbnsValidation
.\gradlew.bat runClient -PclientValidation -PbnsValidation -PcompatValidation
.\gradlew.bat runClient -PclientValidation -PcraftingValidation
.\gradlew.bat runClient -PclientValidation -PcraftingValidation -PcompatValidation
.\gradlew.bat runClient -PclientValidation -PsophisticatedValidation
.\gradlew.bat runClient -PclientValidation -PsophisticatedValidation -PcompatValidation
.\gradlew.bat runServer -PserverValidation
.\gradlew.bat runServer -PserverValidation -PsophisticatedValidation
```

The synchronized transfer profile uses the same combined Sophisticated directory. The JEI recipe profile uses `run-validation-recipe-jei`, with BNS, Stacks Not Slots, Inventory Particles, MossyLib and JEI installed (plus the three Sophisticated mods in the tested setup); omit EMI from this profile. The older BNS/combined controlled-menu commands below document their release baselines; use the new synchronized transfer profile for current BNS routing checks.

The Sophisticated commands use `run-validation-sophisticated`, `run-validation-sophisticated-compat` and `run-server-sophisticated`. Install the listed optional jars into each profile's `mods` folder before reproducing those checks. The vanilla crafting command uses `run-validation-crafting`; the combined crafting command uses `run-validation-compat`. The BNS-only regression command expects BNS and Stacks Not Slots in `run-validation-bns/mods`. The compatibility command expects the listed legally obtained jars in `run-validation-compat/mods`; they are not redistributed. Client assertions and screenshots are saved under the run directory's `validation` folder. Gradle fails a client validation run if its current report is missing or contains a failed assertion. A normal development launch is `gradlew.bat runClient`.

The server gate uses `run-server` and requires your normal Minecraft server EULA acceptance/configuration. Validation source classes are excluded from binary and sources release jars.
