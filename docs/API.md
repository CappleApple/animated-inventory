# Public client API

Namespace: `com.cappleapple.animatedinventory.api`. Call this API on Minecraft's client thread, after Animated Inventory has initialized. Guard optional integrations with the actual mod ID `animatedinventory` and isolate client classes from common/server initialization.

## Describe a view

Implement `InventoryViewProvider`:

```java
public interface InventoryViewProvider {
    String id();
    boolean supports(Screen screen);
    long revision(Screen screen);
    InventoryVisualSnapshot capture(Screen screen, long owner, double mouseX, double mouseY);
    default boolean controlsNormalRendering() { return false; }
}
```

The registration is `AutoCloseable`; close it to unregister. Provider IDs must be unique. The opt-in development fixture [CustomViewFixture.java](../src/validation/java/com/cappleapple/animatedinventory/validation/CustomViewFixture.java) is a complete, functioning non-Slot example. A custom registered provider takes precedence over the vanilla provider. Do not retain a Screen from supports/capture/revision. Store persistent data in your own screen or model, not in this mod's provider instance.

`revision` is a cheap probe, called at most once per client tick except explicit interaction/invalidation checks. Change it whenever content or layout changes. Return a snapshot with exactly the supplied owner and your provider ID. Each `VisualItem` has:

| Field | Meaning |
| --- | --- |
| id | Stable logical ID within this view; can be unrelated to menu indices |
| stack | Defensive ItemStack copy; snapshot consumers must treat it as read-only |
| bounds | Current logical GUI-pixel rectangle |
| region | Your logical visual-region name |
| visible / mayAnimate | Whether endpoints are meaningful and allowed to animate |
| equipment | General equipment classification; no fixed slot list required |
| clipRegion | Nullable GUI-pixel scissor rectangle |
| destinationBounds | Optional fallback for disappearance, or an explicit arrival point for invisible storage |
| sourceBounds | Optional visible origin for retrieval from invisible storage; legacy constructors default to null |
| transactionId | Optional logical transaction identifier |
| overrides | Optional duration, easing, style, alpha, scale, rotation, arc, z and cursor-follow settings |

The entire snapshot's `layoutRevision` changes with layout. Mark a snapshot virtualized when page/filter/catalog changes are presentation replacements; a layout change in a virtualized view is silently rebased. For stable logical IDs moving due to a genuine layout reflow, use a nonvirtualized snapshot.

Call `AnimatedInventoryApi.notifyLayoutChanged(screen, false)` for a hard invalidation or virtual-page change. Use `true` for a stable-ID layout reflow. Removed destinations are not kept alive. An invisible entry may opt into bounded arrival animation by setting `visible = false`, `mayAnimate = true`, and a non-null visible `destinationBounds`. Matching visible losses against increases in that entry produces `TransitionType.STOW`; unrelated hidden updates produce no animation. Its ordinary logical/animated bounds queries remain empty, and no normal slot rendering is suppressed. The departing representation fades out at the arrival point; reduced motion skips the travel.

An invisible entry may independently set `sourceBounds` to opt into retrieval. A matching loss from that entry and visible gain produces `RETRIEVE`, beginning at that origin and fading in. Hidden-to-hidden changes remain unanimated. Existing `VisualItem` constructors remain available; the extended constructor appends `sourceBounds` after `overrides`.

## Rendering ownership

Return `controlsNormalRendering() == true` only if your renderer applies the ownership contract:

```java
ItemStack displayed = AnimatedInventoryApi.normalRenderStack(screen, itemId, actualStack);
Bounds normal = AnimatedInventoryApi.getNormalRenderBounds(screen, itemId).orElse(logicalBounds);
graphics.renderItem(displayed, (int)normal.x(), (int)normal.y());
graphics.renderItemDecorations(font, displayed, (int)normal.x(), (int)normal.y());
```

Never put that adjusted stack back into your inventory. For a partial merge, normal rendering shows the final count minus the quantities currently in transit; each moving representation shows its own quantity. On completion, cancellation or disabling, normal rendering immediately regains the full real stack.

An AbstractContainerScreen already gets an animation layer in the NeoForge foreground event. For a custom non-container Screen, call:

```java
AnimatedInventoryApi.renderAnimations(this, graphics);
```

Call it once after drawing items and before cursor/tooltips, using absolute GUI coordinates. Respect your own scissor stack. Providers that do not opt into ownership remain queryable but do not receive detached inferred animations.

## Explicit transitions

Use observed authoritative/client-synchronized state to supply a completed transaction. Do not wait for an animation before updating gameplay.

```java
long owner = AnimatedInventoryApi.owner(screen);
AnimationOptions options = AnimationOptions.move(140, Easing.EASE_OUT_CUBIC, MovementStyle.ARC);
ItemTransition transfer = new ItemTransition(
    TransitionType.MERGE, sourceId, destinationId, movedStack,
    sourceBounds, destinationBounds, clipBounds, options);
List<Long> handles = AnimatedInventoryApi.notifyTransaction(screen,
    new InventoryVisualTransaction(owner, transactionId, true, List.of(transfer)));
```

`movedStack` is the transferred quantity, not the final destination count. Destination IDs must correspond to visible animatable items in the current provider snapshot, or to an explicit invisible arrival point for a `STOW` transition with a known source. For decorative direct animations with no normal destination to replace, destinationId may be null.

`TransitionType.CRAFT` is a decorative consumed-ingredient representation. Its stack is the consumed ingredient and may differ from the destination's crafted product. Supply the ingredient's source ID/bounds and the product's destination ID/bounds, which may be an explicit invisible arrival point. These transitions never subtract normal item counts or become the destination's queried representation; they follow the product's current movement and fade near completion. The native 2x2/3x3 integration emits them only after an actual result take. Custom providers must likewise base them on observed consumption. Reduced motion skips these transitions.

Convenience methods include `animateItemMove`, `animateItemAppear`, `animateItemDisappear`, `animateItemMerge` and `animateItemSplit`. For splits, provide one transition per resulting destination. `TransitionType.CUSTOM` uses the same options and channels as other direct transitions.

Null transition options use config defaults. Global speed, reduced motion, animation caps and safety checks still apply. Explicit transactions refresh the automatic comparison baseline so the same change is not inferred twice.

## Query coordinates and cancel

```java
Optional<Bounds> logical = AnimatedInventoryApi.getLogicalBounds(screen, itemId);
Optional<Bounds> animated = AnimatedInventoryApi.getAnimatedBounds(screen, itemId);
boolean enabled = AnimatedInventoryApi.isEnabled();
AnimatedInventoryApi.cancel(handle);
```

Bounds use GUI-scaled screen pixels. The animated query follows the most recently accepted incoming representation for the ID; multiple incoming split/merge representations can have different positions. It reports the position rectangle, before hover/emphasis scale and screen entrance transforms. Providers can use separate logical IDs when each representation needs an independently queryable identity.

IDs and owners are scoped to the active view. Queries for removed screens return empty. Passing an old transaction owner is rejected. The API accepts clipping, never changes hitboxes, and creates no tooltips of its own.
