# Compatibility

## Sophisticated Backpacks and Storage

Tested with the user's installed NeoForge 1.21.1 versions: Sophisticated Core **1.5.1.2341**, Backpacks **3.26.2.2141**, and Storage **1.5.91.2127**. Both use Core's [shared inventory screen](https://github.com/P3pp3rF1y/SophisticatedCore/blob/1.21.x/src/main/java/net/p3pp3rf1y/sophisticatedcore/client/gui/StorageScreenBase.java), which overrides vanilla's item drawing and click handling. The optional adapter attaches to those actual paths and retains the original transaction, count renderer and slot decorators.

Pickup, placement, shift transfers, split/merge quantities, oversized stacks and visible scrolled rows use normal animations. The provider also tracks the menu's separate upgrade-slot list. Search/filter changes, scrolling, resizing, tab layout changes and synchronized slot permissions invalidate old geometry. Hidden offscreen cells, ghost filter icons, infinite/inaccessible slots and cells replaced by custom controls do not acquire transfer ownership. Offscreen Sophisticated slots currently have no grid-edge arrival animation.

Crafting-upgrade ingredient flow uses successful client-predicted result takes and net product gains, including shift crafting. The visible recipe inputs supply the ingredient representations; Sophisticated alone consumes and refills the grid on the server. No recipe evaluation, inventory mutation or extra network messages are added. Recipe previews and failed takes alone do not trigger the effect.

The dedicated client fixtures cover ordinary/diamond backpacks, chests, netherite barrels, 1,024-item movement, scrolling/filtering and crafting/pickup upgrade tabs. A second profile adds BNS, Stacks Not Slots, Inventory Particles, MossyLib, JEI and EMI. The 1.0.5 server-backed fixtures also verify BNS transfers in both directions, main-grid split merges, stowed destinations and double-click collection. Nested/linked backpacks and every specialized upgrade control remain outside this fixture coverage.

Only the author's public source was read for interoperability, and installed bytecode signatures were checked. No Sophisticated implementation, assets or jars are bundled or redistributed.

## Bundled Not Siloed

Detected mod ID: `bundlednotsiloed`. Tested against local BNS 1.4.5.

BNS's ordinary 27-cell main grid uses Minecraft's native Slots and item renderer. Those cells participate in the standard animation path, including pickup, placement, transfers within the grid, hotbar swaps and transfers to/from containers. No BNS update or BNS-side animation hook is required for these cells. Its 2x2 crafting grid also uses the normal result-slot consumption path; ingredient travel was exercised in the real BNS screen, including shift crafting, with Inventory Particles installed.

The optional adapter reads BNS's acknowledged logical-slot mapping through its public `ModAttachments.PLAYER_DATA`, `PlayerInventoryData.inventoryWindow()` and `InventorySlotWindow` methods. A changed page rebases the visual snapshot and releases prior animations instead of treating replacement contents as item transfers. For stowed transfers it also reads defensive logical stacks and the storage revision from BNS's public storage methods. It also reads the independent client-sync server revision, holding a bounded visual baseline across quick-transfer prediction and the final server delta. It never mutates BNS data, sends packets, or reflects private screen fields. These are version-checked public implementation members, not a guaranteed BNS integration API; if unavailable, the adapter logs once and native Slot animation remains enabled.

BNS's search results use a separate aggregate renderer and count labels. Those aggregate cells retain BNS rendering; their item movement is not animated by this release. A provider that honors the rendering ownership API in [API.md](API.md) can support that separate renderer. Hotbar and ordinary container cells remain eligible while searching. Screen image effects remain disabled with the BNS compatibility setting to preserve its independently rendered regions.

Off-page stowed destinations keep their true logical index. Their column is retained and their row selects the upper or lower edge of the current grid. A matching visible loss and hidden quantity increase create stowing travel. Matching hidden losses and visible gains create retrieval from below the grid, retaining the logical column. Hidden-only rearrangement, unchanged contents and page replacements do not create travel. A visible stowed slot uses ordinary native animation. If the logical extent exceeds the remaining 4,096-entry snapshot budget, the extra hidden tracking is skipped while normal slots continue animating.

No BNS files were modified.

## Inventory Particles

Verified installed metadata: `InventoryParticles-3.0.0+1.21.1+neoforge.jar`, mod ID `inventory_particles`; requires MossyLib 1.0.5+. Its code license is CC-BY-ND-4.0 and textures are ARR. Only metadata and published project documentation were read; no implementation or textures were copied or reverse-engineered.

The [author's project page](https://www.curseforge.com/minecraft/mc-mods/inventory-particles) documents particle/resource-pack configuration but does not document a Java integration contract used here.

With the guarded compatibility setting enabled:

- Whole-stack movement transforms the existing native slot or cursor rendering once.
- Known-source partial merges and double-click collection use detached model copies, with each moved quantity withheld from the native destination until arrival. Appearances and unknown-source transfers retain the pulse fallback.
- Detached disappearance representations are skipped. Confirmed stowed/retrieved transfers, partial merges and consumed crafting ingredients use Minecraft's item model renderer for the departing visual, without re-entering `GuiGraphics.renderItem` or synthesizing interaction events; normal item decorations are retained.
- Screen framebuffer effects are skipped, preventing a global transform from displacing independent particle layers.
- No inventory clicks, interaction events or particle events are synthesized.

Without a supported particle-coordinate hook, particles keep their logical origin. The public logical/animated bounds queries allow an effects author to opt into moving origins. Stowed arrivals use their separate visible edge destination. This is a coexistence policy, not a claim that every particle follows an animated item or an exact particle-count guarantee.

## JEI, EMI and other recipe viewers

No viewer classes are linked and none are required. Existing recipe-fill packets from BNS, JEI, EMI and the vanilla recipe book are observed without alteration or additional sends. Screen replacement, return, recreation and init all renew animation ownership. While a JEI/EMI recipe screen is open, a weak reference to the same underlying menu preserves its visual origin coordinates; quantities are refreshed at the actual fill request. Only a return to that same screen/menu within the bounded request window may inherit the origin snapshot. Overlays that leave the underlying screen alive do not change its logical coordinates. There is no viewer-specific redirect or blanket screen replacement. A live combined-mod fixture opened EMI's RecipeScreen through its public API and returned successfully; The 1.0.5 JEI-only profile exercises JEI's own recipe screen and registered BNS transfer handler; the combined profile exercises EMI's actual fill operation. Both cover 2x2/3x3 grids with visible and stashed ingredients. Other specialized recipe handlers remain untested.

## TrashSlot

Tested with TrashSlot **21.1.11** and Balm **21.0.65**, alongside BNS, Inventory Particles and Sophisticated Storage/Backpacks. TrashSlot's separate widget shares the native slot renderer and defaults to index 0, but is not a member of the open menu. Native animation ownership now requires both that index and the exact menu Slot object. Empty trash slots keep their trash icon, and retained undo items keep their own native representation during pending transfers and active animations.

This is a general slot ownership check with no TrashSlot dependency or additional mixin. Installed bytecode was inspected for interoperability; no TrashSlot implementation or assets are bundled. The live fixture reproduces the former pending-diamond substitution, then checks empty and retained-item rendering across actual server transfers. Retained-item checks use TrashSlot's native client content notification; they do not claim an end-to-end delete/undo test.

## Creative inventory

The built-in provider excludes catalog cells and the delete slot by tracking only slots backed by the player's Inventory, plus the carried stack. Catalog scrolling/search/tab changes cannot create hundreds of fake acquisitions. Slot-list/layout changes rebase virtualized views.

## General modded containers

AbstractContainerScreen subclasses retain the normal slot item pipeline. If a mod overrides that pipeline or draws items outside Slots, it must register a provider and honor rendering ownership. Otherwise the safe behavior is normal rendering without inferred movement for that region.

Screen effects use a vanilla-class allowlist; arbitrary removed screens are never rerendered. Custom item renderers with unusual GL side effects may fall back to normal rendering for the screen if they fail.

## Servers

There are no packets, channels, server handlers or common initialization paths that load client classes. The mod entry point is restricted to Dist.CLIENT and the mixin list is client-only. Clients do not require it on the server.
