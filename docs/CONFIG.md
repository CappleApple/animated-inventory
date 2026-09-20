# Client configuration

File: `config/animatedinventory-client.toml`. Open **Mods → Animated Inventory → Config** for the built-in Forge configuration screen. Settings are grouped into translated sections. Hover a label or control for help. Done saves the current draft; Cancel discards it. Invalid numeric values block saving and page navigation.

Durations are in milliseconds. Every duration is divided by `animation_speed_multiplier`: 0.5 is half speed and 2.0 is double speed. Reduced motion removes movement/overshoot and uses short fades (up to 60 ms); hover and emphasis scaling are reduced. The particle-safe inline path uses emphasis instead of additional fades.

The following are the actual generated defaults, including supported enum values and ranges. The names below are the canonical TOML keys; the screen displays translated names and enum choices.

```toml
[general]
	animations_enabled = true
	# Default: 1.0
	# Range: 0.1 ~ 10.0
	animation_speed_multiplier = 1.0
	reduce_motion = false
	# Default: 128
	# Range: 1 ~ 512
	max_simultaneous_item_animations = 128
	#Allowed Values: RETARGET, FINISH_FAST, CANCEL
	animation_interruption_mode = "RETARGET"

[movement]
	item_movement_enabled = true
	# Default: 140
	# Range: 10 ~ 2000
	movement_duration_ms = 140
	#Allowed Values: LINEAR, SMOOTH, ARC, SPRING, SNAP_SMOOTH
	movement_style = "SMOOTH"
	#Allowed Values: LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP
	movement_easing = "EASE_OUT_CUBIC"
	# Default: 8.0
	# Range: 0.0 ~ 64.0
	movement_arc_height = 8.0
	#Allowed Values: LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP
	swap_easing = "EASE_IN_OUT_CUBIC"

[crafting]
    crafting_ingredient_animation = true
    # Default: 160
    # Range: 10 ~ 2000
    crafting_animation_duration_ms = 160

[quick_move]
	quick_move_enabled = true
	# Default: 160
	# Range: 10 ~ 2000
	quick_move_duration_ms = 160
	# Default: 80
	# Range: 0 ~ 250
	transaction_correlation_window_ms = 80

[cursor]
	cursor_pickup_animation = true
	#Allowed Values: FIXED, FOLLOW_CURSOR
	cursor_pickup_mode = "FOLLOW_CURSOR"
	# Default: 90
	# Range: 10 ~ 2000
	cursor_pickup_duration_ms = 90
	#Allowed Values: LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP
	cursor_pickup_easing = "EASE_OUT_CUBIC"
	cursor_placement_animation = true
	# Default: 100
	# Range: 10 ~ 2000
	cursor_placement_duration_ms = 100
	#Allowed Values: LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP
	cursor_placement_easing = "EASE_OUT_CUBIC"

[hover]
	slot_hover_animation = true
	# Default: 1.08
	# Range: 1.0 ~ 1.5
	slot_hover_scale = 1.08
	# Default: 80
	# Range: 10 ~ 2000
	slot_hover_duration_ms = 80
	#Allowed Values: LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP
	slot_hover_easing = "EASE_OUT_CUBIC"
	# Default: 10.0
	# Range: 0.0 ~ 50.0
	slot_hover_raise_z = 10.0

[click]
	click_feedback_enabled = true
	# Default: 0.92
	# Range: 0.7 ~ 1.0
	click_scale = 0.92
	# Default: 70
	# Range: 10 ~ 2000
	click_duration_ms = 70

[merge]
	merge_pulse_enabled = true
	# Default: 1.12
	# Range: 1.0 ~ 1.5
	merge_pulse_scale = 1.12
	# Default: 100
	# Range: 10 ~ 2000
	merge_pulse_duration_ms = 100

[hotbar]
	animate_hotbar_selector = true
	# Default: 110
	# Range: 10 ~ 2000
	hotbar_animation_duration_ms = 110
	#Allowed Values: LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP
	hotbar_easing = "EASE_OUT_CUBIC"
	# Default: 0.0
	# Range: 0.0 ~ 1.0
	hotbar_spring_strength = 0.0

[screens]
	#Allowed Values: NONE, FADE, SCALE, FADE_SCALE, SLIDE
	screen_open_animation = "FADE_SCALE"
	# Default: 120
	# Range: 10 ~ 2000
	screen_open_duration_ms = 120
	#Allowed Values: LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP
	screen_open_easing = "EASE_OUT_CUBIC"
	# Default: 0.96
	# Range: 0.8 ~ 1.0
	screen_open_scale_start = 0.96
	# Default: 8.0
	# Range: -64.0 ~ 64.0
	screen_open_slide_distance = 8.0
	#Allowed Values: NONE, FADE, SCALE, FADE_SCALE, SLIDE
	screen_close_animation = "FADE_SCALE"
	# Default: 100
	# Range: 10 ~ 2000
	screen_close_duration_ms = 100
	#Allowed Values: LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP
	screen_close_easing = "EASE_OUT_CUBIC"
	# Default: 0.96
	# Range: 0.8 ~ 1.0
	screen_close_scale_end = 0.96
	# Default: 8.0
	# Range: -64.0 ~ 64.0
	screen_close_slide_distance = 8.0

[items]
	#Allowed Values: NONE, FADE, SCALE, FADE_SCALE, POP
	item_appear_animation = "FADE_SCALE"
	#Allowed Values: NONE, FADE, SCALE, FADE_SCALE, POP
	item_disappear_animation = "FADE_SCALE"

[layout_reflow]
	animate_layout_reflow = true
	# Default: 140
	# Range: 10 ~ 2000
	layout_reflow_duration_ms = 140
	#Allowed Values: LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC, EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP
	layout_reflow_easing = "EASE_IN_OUT_CUBIC"

[compatibility]
	bns_integration = true
	inventory_particles_integration = true

[debug]
	debug_overlay = false
	debug_logging = false
```

Changes apply to new animations. Disabling the master toggle releases active rendering ownership. Hover hitboxes and selected hotbar slots never change. Setting screen close to NONE avoids retaining/capturing a screen image after its entrance completes.

The Bundled Not Siloed control is disabled because its NeoForge attachment adapter is unavailable on this Forge version. Its TOML key is retained for configuration parity.

Labels, section headings, enum choices and tooltips can be overridden through `assets/animatedinventory/lang/en_us.json`. English is bundled; other selected languages use the normal English fallback unless a resource pack supplies translations.
