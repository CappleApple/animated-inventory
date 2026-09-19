package com.cappleapple.animatedinventory.client;

import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.api.inventory.ItemTransition;
import com.cappleapple.animatedinventory.client.config.ClientConfigSpec;

public final class ClientConfig {
    public enum ScreenEffect { NONE, FADE, SCALE, FADE_SCALE, SLIDE }
    public enum ItemEffect { NONE, FADE, SCALE, FADE_SCALE, POP }
    public enum CursorMode { FIXED, FOLLOW_CURSOR }
    public enum Interruption { RETARGET, FINISH_FAST, CANCEL }
    private static final ClientConfigSpec.Builder B = new ClientConfigSpec.Builder();
    static { B.push("general"); }
    public static final ClientConfigSpec.BooleanValue ENABLED = bool("animations_enabled", true);
    public static final ClientConfigSpec.DoubleValue SPEED = number("animation_speed_multiplier", 1, .1, 10);
    public static final ClientConfigSpec.BooleanValue REDUCE_MOTION = bool("reduce_motion", false);
    public static final ClientConfigSpec.IntValue MAX = integer("max_simultaneous_item_animations", 128, 1, 512);
    public static final ClientConfigSpec.EnumValue<Interruption> INTERRUPTION = B.defineEnum("animation_interruption_mode", Interruption.RETARGET);
    static { B.pop().push("movement"); }
    public static final ClientConfigSpec.BooleanValue MOVEMENT = bool("item_movement_enabled", true);
    public static final ClientConfigSpec.IntValue MOVE_MS = duration("movement_duration_ms", 140);
    public static final ClientConfigSpec.EnumValue<MovementStyle> STYLE = B.defineEnum("movement_style", MovementStyle.SMOOTH);
    public static final ClientConfigSpec.EnumValue<Easing> MOVE_EASING = easing("movement_easing", Easing.EASE_OUT_CUBIC);
    public static final ClientConfigSpec.DoubleValue ARC = number("movement_arc_height", 8, 0, 64);
    public static final ClientConfigSpec.EnumValue<Easing> SWAP_EASING = easing("swap_easing", Easing.EASE_IN_OUT_CUBIC);
    static { B.pop().push("crafting"); }
    public static final ClientConfigSpec.BooleanValue CRAFT = bool("crafting_ingredient_animation", true);
    public static final ClientConfigSpec.IntValue CRAFT_MS = duration("crafting_animation_duration_ms", 160);
    static { B.pop().push("quick_move"); }
    public static final ClientConfigSpec.BooleanValue QUICK = bool("quick_move_enabled", true);
    public static final ClientConfigSpec.IntValue QUICK_MS = duration("quick_move_duration_ms", 160);
    public static final ClientConfigSpec.IntValue CORRELATION_MS = integer("transaction_correlation_window_ms", 80, 0, 250);
    static { B.pop().push("cursor"); }
    public static final ClientConfigSpec.BooleanValue PICKUP = bool("cursor_pickup_animation", true);
    public static final ClientConfigSpec.EnumValue<CursorMode> CURSOR_MODE = B.defineEnum("cursor_pickup_mode", CursorMode.FOLLOW_CURSOR);
    public static final ClientConfigSpec.IntValue PICKUP_MS = duration("cursor_pickup_duration_ms", 90);
    public static final ClientConfigSpec.EnumValue<Easing> PICKUP_EASING = easing("cursor_pickup_easing", Easing.EASE_OUT_CUBIC);
    public static final ClientConfigSpec.BooleanValue PLACEMENT = bool("cursor_placement_animation", true);
    public static final ClientConfigSpec.IntValue PLACE_MS = duration("cursor_placement_duration_ms", 100);
    public static final ClientConfigSpec.EnumValue<Easing> PLACE_EASING = easing("cursor_placement_easing", Easing.EASE_OUT_CUBIC);
    static { B.pop().push("hover"); }
    public static final ClientConfigSpec.BooleanValue HOVER = bool("slot_hover_animation", true);
    public static final ClientConfigSpec.DoubleValue HOVER_SCALE = number("slot_hover_scale", 1.08, 1, 1.5);
    public static final ClientConfigSpec.IntValue HOVER_MS = duration("slot_hover_duration_ms", 80);
    public static final ClientConfigSpec.EnumValue<Easing> HOVER_EASING = easing("slot_hover_easing", Easing.EASE_OUT_CUBIC);
    public static final ClientConfigSpec.DoubleValue HOVER_Z = number("slot_hover_raise_z", 10, 0, 50);
    static { B.pop().push("click"); }
    public static final ClientConfigSpec.BooleanValue CLICK = bool("click_feedback_enabled", true);
    public static final ClientConfigSpec.DoubleValue CLICK_SCALE = number("click_scale", .92, .7, 1);
    public static final ClientConfigSpec.IntValue CLICK_MS = duration("click_duration_ms", 70);
    static { B.pop().push("merge"); }
    public static final ClientConfigSpec.BooleanValue MERGE = bool("merge_pulse_enabled", true);
    public static final ClientConfigSpec.DoubleValue MERGE_SCALE = number("merge_pulse_scale", 1.12, 1, 1.5);
    public static final ClientConfigSpec.IntValue MERGE_MS = duration("merge_pulse_duration_ms", 100);
    static { B.pop().push("hotbar"); }
    public static final ClientConfigSpec.BooleanValue HOTBAR = bool("animate_hotbar_selector", true);
    public static final ClientConfigSpec.IntValue HOTBAR_MS = duration("hotbar_animation_duration_ms", 110);
    public static final ClientConfigSpec.EnumValue<Easing> HOTBAR_EASING = easing("hotbar_easing", Easing.EASE_OUT_CUBIC);
    public static final ClientConfigSpec.DoubleValue HOTBAR_SPRING = number("hotbar_spring_strength", 0, 0, 1);
    static { B.pop().push("screens"); }
    public static final ClientConfigSpec.EnumValue<ScreenEffect> OPEN = B.defineEnum("screen_open_animation", ScreenEffect.FADE_SCALE);
    public static final ClientConfigSpec.IntValue OPEN_MS = duration("screen_open_duration_ms", 120);
    public static final ClientConfigSpec.EnumValue<Easing> OPEN_EASING = easing("screen_open_easing", Easing.EASE_OUT_CUBIC);
    public static final ClientConfigSpec.DoubleValue OPEN_SCALE = number("screen_open_scale_start", .96, .8, 1);
    public static final ClientConfigSpec.DoubleValue OPEN_SLIDE = number("screen_open_slide_distance", 8, -64, 64);
    public static final ClientConfigSpec.EnumValue<ScreenEffect> CLOSE = B.defineEnum("screen_close_animation", ScreenEffect.FADE_SCALE);
    public static final ClientConfigSpec.IntValue CLOSE_MS = duration("screen_close_duration_ms", 100);
    public static final ClientConfigSpec.EnumValue<Easing> CLOSE_EASING = easing("screen_close_easing", Easing.EASE_OUT_CUBIC);
    public static final ClientConfigSpec.DoubleValue CLOSE_SCALE = number("screen_close_scale_end", .96, .8, 1);
    public static final ClientConfigSpec.DoubleValue CLOSE_SLIDE = number("screen_close_slide_distance", 8, -64, 64);
    static { B.pop().push("items"); }
    public static final ClientConfigSpec.EnumValue<ItemEffect> APPEAR = B.defineEnum("item_appear_animation", ItemEffect.FADE_SCALE);
    public static final ClientConfigSpec.EnumValue<ItemEffect> DISAPPEAR = B.defineEnum("item_disappear_animation", ItemEffect.FADE_SCALE);
    static { B.pop().push("layout_reflow"); }
    public static final ClientConfigSpec.BooleanValue REFLOW = bool("animate_layout_reflow", true);
    public static final ClientConfigSpec.IntValue REFLOW_MS = duration("layout_reflow_duration_ms", 140);
    public static final ClientConfigSpec.EnumValue<Easing> REFLOW_EASING = easing("layout_reflow_easing", Easing.EASE_IN_OUT_CUBIC);
    static { B.pop().push("compatibility"); }
    public static final ClientConfigSpec.BooleanValue BNS = bool("bns_integration", true);
    public static final ClientConfigSpec.BooleanValue PARTICLES = bool("inventory_particles_integration", true);
    static { B.pop().push("debug"); }
    public static final ClientConfigSpec.BooleanValue DEBUG = bool("debug_overlay", false);
    public static final ClientConfigSpec.BooleanValue LOG = bool("debug_logging", false);
    public static final ClientConfigSpec SPEC = B.pop().build();

    private static ClientConfigSpec.BooleanValue bool(String key, boolean def) { return B.define(key, def); }
    private static ClientConfigSpec.DoubleValue number(String key, double def, double min, double max) { return B.defineInRange(key, def, min, max); }
    private static ClientConfigSpec.IntValue integer(String key, int def, int min, int max) { return B.defineInRange(key, def, min, max); }
    private static ClientConfigSpec.IntValue duration(String key, int def) { return integer(key, def, 10, 2000); }
    private static ClientConfigSpec.EnumValue<Easing> easing(String key, Easing def) { return B.defineEnum(key, def); }
    public static long nanos(long millis) { return Math.max(1, (long)(millis * 1_000_000.0 / SPEED.get())); }
    public static AnimationOptions options(ItemTransition t, boolean quick) {
        if (t.options() != null) return t.options();
        if (t.type() == TransitionType.CRAFT) return new AnimationOptions(CRAFT_MS.get(), MOVE_EASING.get(), STYLE.get(),
                ARC.get(), 1, .25, 1, 1, 0, 0, 140, false);
        int duration = quick ? QUICK_MS.get() : MOVE_MS.get();
        Easing easing = t.type() == TransitionType.SWAP ? SWAP_EASING.get() : MOVE_EASING.get();
        boolean cursor = "cursor".equals(t.destinationId());
        if (cursor) { duration = PICKUP_MS.get(); easing = PICKUP_EASING.get(); }
        else if ("cursor".equals(t.sourceId())) { duration = PLACE_MS.get(); easing = PLACE_EASING.get(); }
        if (t.type() == TransitionType.LAYOUT_REFLOW) { duration = REFLOW_MS.get(); easing = REFLOW_EASING.get(); }
        double s0 = 1, s1 = 1, a0 = 1, a1 = 1;
        ItemEffect effect = t.type() == TransitionType.APPEAR ? APPEAR.get() : t.type() == TransitionType.DISAPPEAR ? DISAPPEAR.get() : ItemEffect.NONE;
        if (effect != ItemEffect.NONE) {
            boolean appearing = t.type() == TransitionType.APPEAR;
            double scale = effect == ItemEffect.FADE ? 1 : .85;
            double alpha = effect == ItemEffect.SCALE || effect == ItemEffect.POP ? 1 : 0;
            if (appearing) { s0 = scale; a0 = alpha; } else { s1 = scale; a1 = alpha; }
            if (effect == ItemEffect.POP) easing = Easing.BACK_OUT;
        }
        return new AnimationOptions(duration, easing, STYLE.get(), ARC.get(), s0, s1, a0, a1, 0, 0,
                cursor ? 200 : 150, cursor && CURSOR_MODE.get() == CursorMode.FOLLOW_CURSOR);
    }
    private ClientConfig() { }
}
