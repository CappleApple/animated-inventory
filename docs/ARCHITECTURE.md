# Architecture

All source is original. Minecraft/NeoForge API contracts, the standard Gradle wrapper, explicitly permitted local BNS/Stacks Not Slots source and the Sophisticated author's public interoperability source were used as implementation references. No animation project code, layout, assets or algorithms were imported.

## Data flow

1. A provider describes logical IDs, defensive ItemStack copies, GUI-pixel rectangles, regions, visibility, equipment metadata and optional clip/animation overrides.
2. A before/after comparison reserves the quantity that stayed at the same identity. Remaining compatible item-and-component deltas are matched by count and distance.
3. An immutable visual transaction carries separate typed transitions. Actual menu prediction or server synchronization has already happened.
4. The animation manager accepts a bounded set of visual copies. Only accepted, still-active destination claims subtract their moved quantities from normal rendering.
5. The renderer composes position, scale, opacity, rotation and emphasis. Completion/cancellation returns full rendering ownership to the normal view.

Stacks of indistinguishable content have no globally unique identity in vanilla. Matching is deterministic inference, not authoritative inventory provenance. Provider IDs and explicit transaction IDs improve this. A compare exceeding 65,536 candidate checks yields no animations; snapshots above 4,096 entries are rejected for that screen.

## Crafting consumption

The client-only ResultSlot hook observes the stacks actually removed by `onTake` in 2x2 and 3x3 grids. It calls the original removal exactly once and records defensive copies; it does not evaluate recipes, replay crafting or modify remainders. A recipe preview or ingredient placement alone cannot trigger ingredient flow.

`CraftingFlow` combines these observations with the before/after snapshot. Consumed input cells use their final state for ordinary inference, leaving remaining ingredients, buckets and reusable tools in normal rendering. Actual output gains are attributed to the result slot even when its preview regenerates. Repeated takes aggregate consumed quantities per input, within the animation budget.

Decorative `CRAFT` transitions travel from the consumed inputs toward the product's animated position, then its destination or moving cursor, shrinking and fading on arrival. They never claim normal item counts or replace the product's coordinate query. Shift crafting selects the largest output destination when the product fills several stacks. If no output destination can be inferred, the result slot is the convergence point. Closing, resetting, page replacement and failure clear pending observations and visuals; reduced motion skips ingredient travel.

## Sophisticated's custom screen

A separate client-only mixin configuration is loaded only when `sophisticatedcore` is installed. The shared `StorageScreenBase` overrides both `renderSlot` and `slotClicked`, so it gets narrow observation wrappers of its own. Its original `renderStack` still draws the model and count label, including abbreviations and font scaling. Actual menu prediction and the original packet send execute once.

A read-only menu view includes Core's separate upgrade-slot list. The provider treats filtering, scrolling and upgrade layout changes as virtualized view replacements. Coordinates are checked against the storage viewport; filtered/sentinel cells, ghost filters, infinite/inaccessible slots and cells drawn by separate inventory controls cannot acquire transfer ownership. Additional-slot-info increments the geometry revision only when its permission, limit or filter values change. Repeated identical packets leave active transfers intact.

The crafting upgrade's result slot consumes/refills ingredients only on the server. The client adapter records the visible recipe input IDs before a result interaction, then requires a net gain of the matching output before emitting decorative ingredient usage. It does not evaluate recipes or change the grid. Existing product redistribution contributes no net gain, and failed takes or preview updates do not craft. The ordinary product movement and `CRAFT` channels then use the same engine as vanilla.

## State and lifecycle

The runtime owns only the active Screen's animations. A separate weak reference retains recipe-viewer origin coordinates only for its underlying menu and clears on unrelated navigation or disconnect. Each screen init/recreation/return gets a new numerical owner; removal clears snapshots, animations and emphasis. Menu identity, slot identities, active flags, coordinates and dimensions contribute to the vanilla layout revision. Layout validation scans coordinates without copying stacks every frame. Content revisions are checked once per client tick; snapshots are captured for changes, relevant clicks, pending synchronization or explicit provider notifications.

The first full content synchronization for a menu rebases the active vanilla-provider snapshot after all slots and the cursor are updated. This prevents the empty client menu created during opening from becoming the source of item-arrival effects. The marker belongs to the menu, so resizing does not suppress later updates. Existing player interactions and pending transfers retain their comparison snapshots.

Native slot participation, pending stack reads and animation transforms require the actual Slot object at its index in the active menu's slot list, including Sophisticated upgrade slots. Auxiliary widgets such as TrashSlot can call the same renderer with index 0 but a different Slot; their native items pass through unchanged and they cannot replace a real slot's participation record. A modded override that bypasses that renderer cannot acquire a vanilla suppression claim, preventing duplicate native/animated copies. A native stack change observed before the next tick releases its stale claim immediately.

Input snapshots compare immediately after vanilla prediction. Subsequent changed ticks can share the same transaction ID within the configured correlation window, without delaying gameplay. BNS quick transfers and observed recipe-fill/sort requests keep a defensive visual baseline while awaiting separately delivered updates. A changed BNS server revision (or changed contents without BNS), followed by a quiet 25 ms interval, confirms the comparison; a 750 ms timeout or layout/owner change releases the baseline. Pending native render reads show the original items without changing real slots. Retargeting uses the current visual source; ambiguous overlapping claims are released. FINISH_FAST releases the old claim and uses at most 45 ms for the replacement. CANCEL immediately restores all touched views.

Virtualized providers must change their layout revision when their visible list changes. Such changes rebase without inferred inventory movement. Stable-ID layout reflows use nonvirtualized snapshots and explicit layout notifications. Logical cursor movement does not imply inventory reflow.

## Narrow mixins

| Hook | Purpose |
| --- | --- |
| AbstractContainerScreen.slotClicked wrapper | Observe before/after actual menu handling; original called exactly once |
| AbstractContainerMenu.initializeContents return | Establish the first synchronized content baseline once per menu without changing its items |
| ClientCommonPacketListenerImpl.send | Observe existing recipe-fill and BNS arrangement requests without changing or sending packets |
| ResultSlot.onTake crafting-container removeItem call | Observe actual consumed ingredients after the original removal; keep crafting unchanged |
| AbstractContainerScreen.render wrapper | Begin image capture; restore the render target in cleanup |
| renderSlotContents call inside renderSlot | Adjust only visual stack count and item transform; retain vanilla drag previews and decorators |
| renderFloatingItem wrapper | Cursor-copy ownership and optional inline cursor movement |
| renderSlot/renderSlotHighlight calls inside render | Draw the native highlight once, before the item; preserve vanilla isHovering via an invoker |
| Sophisticated StorageScreenBase slotClicked/renderSlot/renderStack calls | Observe its custom prediction and item drawing; preserve original count labels |
| Sophisticated StorageContainerMenuBase additional-slot-info update | Invalidate visual ownership after synchronized slot permission changes |
| Gui.renderItemHotbar selection-sprite call | Translate only the existing selector, leaving selected-slot state unchanged |

NeoForge ScreenEvent.Init.Post, Closing, Render.Pre, mouse-press events, ClientTickEvent.Post, ContainerScreenEvent.Render.Foreground and RenderGuiEvent.Post own the remaining lifecycle.

## Rendering

Explicit invisible arrival points allow confirmed transfers into off-page storage without pretending those entries are normal rendered slots. BNS provides defensive stowed stack quantities and edge geometry. Inference emits `STOW` for matched visible losses and hidden gains, and `RETRIEVE` for explicit hidden sources matched to visible gains. Hidden-to-hidden rearrangement and unmatched hidden losses remain invisible. Retrieval fades in over its first quarter and rises from below the player grid. The departing model uses Minecraft's item renderer directly and fades over the final 35% of its bounded lifetime. Page replacement, removal, cancellation and reduced motion release or skip these visuals.

Moving representations render in the foreground event before the carried stack and tooltips. Normal item rendering supplies models, glint, component-sensitive appearance and item decorations. A reusable 64-GUI-pixel render surface composites opacity for otherwise opaque item shaders. Scissor, framebuffer, viewport and projection are restored around it.

Supported vanilla screens render into a transparent surface until the foreground boundary. The surface is composited before the native cursor and tooltips, with visual-only opening transforms. Closing keeps the last surface for a bounded duration and never calls the removed screen. At most one live and one exiting full-resolution surface are retained. If closing is NONE, capture stops after opening. No screen images persist on disk outside the opt-in test harness.

Custom renderers can preserve their own draw order and clipping through the API. Failures release visual ownership and disable this mod's effects for the current screen; the next init retries with a fresh owner.
