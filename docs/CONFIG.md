# Client configuration

File: `config/animatedinventory-client.json`. Press **F8** to open the configuration editor. The editor validates numeric and enum values before saving and applies accepted changes immediately. Missing or invalid entries retain their defaults; invalid entries produce a log warning. The NeoForge TOML file is not imported.

Durations are in milliseconds. Every duration is divided by `animation_speed_multiplier`: 0.5 is half speed and 2.0 is double speed. Reduced motion removes movement/overshoot and uses short fades (up to 60 ms); hover and emphasis scaling are reduced. The particle-safe inline path uses emphasis instead of additional fades.

The following JSON contains the generated defaults. Sections are nested JSON objects; setting names are the canonical keys. Numeric bounds and enum choices follow the example.

```json
{
  "general": {
    "animations_enabled": true,
    "animation_speed_multiplier": 1.0,
    "reduce_motion": false,
    "max_simultaneous_item_animations": 128,
    "animation_interruption_mode": "RETARGET"
  },
  "movement": {
    "item_movement_enabled": true,
    "movement_duration_ms": 140,
    "movement_style": "SMOOTH",
    "movement_easing": "EASE_OUT_CUBIC",
    "movement_arc_height": 8.0,
    "swap_easing": "EASE_IN_OUT_CUBIC"
  },
  "crafting": {
    "crafting_ingredient_animation": true,
    "crafting_animation_duration_ms": 160
  },
  "quick_move": {
    "quick_move_enabled": true,
    "quick_move_duration_ms": 160,
    "transaction_correlation_window_ms": 80
  },
  "cursor": {
    "cursor_pickup_animation": true,
    "cursor_pickup_mode": "FOLLOW_CURSOR",
    "cursor_pickup_duration_ms": 90,
    "cursor_pickup_easing": "EASE_OUT_CUBIC",
    "cursor_placement_animation": true,
    "cursor_placement_duration_ms": 100,
    "cursor_placement_easing": "EASE_OUT_CUBIC"
  },
  "hover": {
    "slot_hover_animation": true,
    "slot_hover_scale": 1.08,
    "slot_hover_duration_ms": 80,
    "slot_hover_easing": "EASE_OUT_CUBIC",
    "slot_hover_raise_z": 10.0
  },
  "click": {
    "click_feedback_enabled": true,
    "click_scale": 0.92,
    "click_duration_ms": 70
  },
  "merge": {
    "merge_pulse_enabled": true,
    "merge_pulse_scale": 1.12,
    "merge_pulse_duration_ms": 100
  },
  "hotbar": {
    "animate_hotbar_selector": true,
    "hotbar_animation_duration_ms": 110,
    "hotbar_easing": "EASE_OUT_CUBIC",
    "hotbar_spring_strength": 0.0
  },
  "screens": {
    "screen_open_animation": "FADE_SCALE",
    "screen_open_duration_ms": 120,
    "screen_open_easing": "EASE_OUT_CUBIC",
    "screen_open_scale_start": 0.96,
    "screen_open_slide_distance": 8.0,
    "screen_close_animation": "FADE_SCALE",
    "screen_close_duration_ms": 100,
    "screen_close_easing": "EASE_OUT_CUBIC",
    "screen_close_scale_end": 0.96,
    "screen_close_slide_distance": 8.0
  },
  "items": {
    "item_appear_animation": "FADE_SCALE",
    "item_disappear_animation": "FADE_SCALE"
  },
  "layout_reflow": {
    "animate_layout_reflow": true,
    "layout_reflow_duration_ms": 140,
    "layout_reflow_easing": "EASE_IN_OUT_CUBIC"
  },
  "compatibility": {
    "bns_integration": true,
    "inventory_particles_integration": true
  },
  "debug": {
    "debug_overlay": false,
    "debug_logging": false
  }
}
```

| Setting | Range or allowed values |
| --- | --- |
| `general.animation_speed_multiplier` | 0.1 to 10.0 |
| `general.max_simultaneous_item_animations` | 1 to 512 |
| `general.animation_interruption_mode` | RETARGET, FINISH_FAST, CANCEL |
| `movement.movement_duration_ms` | 10 to 2000 |
| `movement.movement_style` | LINEAR, SMOOTH, ARC, SPRING, SNAP_SMOOTH |
| `movement.movement_easing` | LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP |
| `movement.movement_arc_height` | 0.0 to 64.0 |
| `movement.swap_easing` | LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP |
| `crafting.crafting_animation_duration_ms` | 10 to 2000 |
| `quick_move.quick_move_duration_ms` | 10 to 2000 |
| `quick_move.transaction_correlation_window_ms` | 0 to 250 |
| `cursor.cursor_pickup_mode` | FIXED, FOLLOW_CURSOR |
| `cursor.cursor_pickup_duration_ms` | 10 to 2000 |
| `cursor.cursor_pickup_easing` | LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP |
| `cursor.cursor_placement_duration_ms` | 10 to 2000 |
| `cursor.cursor_placement_easing` | LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP |
| `hover.slot_hover_scale` | 1.0 to 1.5 |
| `hover.slot_hover_duration_ms` | 10 to 2000 |
| `hover.slot_hover_easing` | LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP |
| `hover.slot_hover_raise_z` | 0.0 to 50.0 |
| `click.click_scale` | 0.7 to 1.0 |
| `click.click_duration_ms` | 10 to 2000 |
| `merge.merge_pulse_scale` | 1.0 to 1.5 |
| `merge.merge_pulse_duration_ms` | 10 to 2000 |
| `hotbar.hotbar_animation_duration_ms` | 10 to 2000 |
| `hotbar.hotbar_easing` | LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP |
| `hotbar.hotbar_spring_strength` | 0.0 to 1.0 |
| `screens.screen_open_animation` | NONE, FADE, SCALE, FADE_SCALE, SLIDE |
| `screens.screen_open_duration_ms` | 10 to 2000 |
| `screens.screen_open_easing` | LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP |
| `screens.screen_open_scale_start` | 0.8 to 1.0 |
| `screens.screen_open_slide_distance` | -64.0 to 64.0 |
| `screens.screen_close_animation` | NONE, FADE, SCALE, FADE_SCALE, SLIDE |
| `screens.screen_close_duration_ms` | 10 to 2000 |
| `screens.screen_close_easing` | LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP |
| `screens.screen_close_scale_end` | 0.8 to 1.0 |
| `screens.screen_close_slide_distance` | -64.0 to 64.0 |
| `items.item_appear_animation` | NONE, FADE, SCALE, FADE_SCALE, POP |
| `items.item_disappear_animation` | NONE, FADE, SCALE, FADE_SCALE, POP |
| `layout_reflow.layout_reflow_duration_ms` | 10 to 2000 |
| `layout_reflow.layout_reflow_easing` | LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP |

Changes apply to new animations. Disabling the master toggle releases active rendering ownership. Hover hitboxes and selected hotbar slots never change. Setting screen close to NONE avoids retaining/capturing a screen image after its entrance completes.

`hover.slot_hover_raise_z` controls elevated rendering order in the extracted GUI. A positive value draws enlarged items and their decorations after ordinary slots; zero keeps them in the ordinary slot layer.

Labels and tooltips are resource-pack overrideable at `assets/animatedinventory/lang/en_us.json`.
