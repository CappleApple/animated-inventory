package com.cappleapple.animatedinventory.validation;

import com.cappleapple.animatedinventory.client.ClientConfig;
import com.cappleapple.animatedinventory.client.config.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Checks displayed controls, saved values, invalid input and actual resource reload fallback. */
final class ConfigValidation {
    private static int phase;
    private static CompletableFuture<Void> reload;
    static boolean tick() throws Exception {
        var mc = Minecraft.getInstance();
        if (phase == 0) {
            var original = new LinkedHashMap<ClientConfigSpec.Value<?>,String>();
            ClientConfig.SPEC.values().forEach(v -> original.put(v, String.valueOf(v.get())));
            try {
                var screen = (AnimationConfigScreen)mc.screen;
                var speed = controls(screen).get(ClientConfig.SPEED);
                ClientSmoke.require(speed instanceof EditBox, "numeric setting uses an editable control");
                var enabled = (Button)controls(screen).get(ClientConfig.ENABLED);
                boolean before = ClientConfig.ENABLED.get(); enabled.onPress();
                ClientSmoke.require(ClientConfig.ENABLED.get() != before && enabled.getMessage().getString().equals(AnimationConfigScreen.valueLabel(!before).getString()), "boolean control changes value and translated label");
                enabled.onPress();
                ((EditBox)speed).setValue("NaN");
                int page = page(screen); navigation(screen, "animatedinventory.configuration.ui.next").onPress();
                ClientSmoke.require(page(screen) == page && !error(screen).getString().isEmpty() && !error(screen).getString().contains("animatedinventory."), "invalid numeric input keeps page open with translated error: before=" + page + ", after=" + page(screen) + ", value=" + ((EditBox)speed).getValue() + ", error=" + error(screen).getString());
                ((EditBox)speed).setValue("2.25");
                navigation(screen, "animatedinventory.configuration.ui.next").onPress();
                ClientSmoke.require(ClientConfig.SPEED.get() == 2.25, "page change applies valid numeric setting");
                int settings = 0;
                mc.setScreen(screen = new AnimationConfigScreen(null));
                while (true) {
                    for (var entry : controls(screen).entrySet()) {
                        var value = entry.getKey();
                        translated(AnimationConfigScreen.label(value)); translated(AnimationConfigScreen.section(value)); translated(AnimationConfigScreen.description(value));
                        translated(entry.getValue().getMessage());
                        if (value == ClientConfig.BNS) {
                            var unavailable = (Button)entry.getValue(); boolean previous = ClientConfig.BNS.get();
                            ((Screen)screen).mouseClicked(unavailable.getX() + unavailable.getWidth() / 2.0, unavailable.getY() + 10, 0);
                            ClientSmoke.require(!unavailable.active && unavailable.getMessage().getString().equals(Component.translatable("animatedinventory.configuration.ui.unavailable").getString()) && ClientConfig.BNS.get() == previous, "unsupported BNS control is disabled, translated and ignores mouse clicks");
                        }
                        if (value.get() instanceof Enum<?> option) {
                            for (Object constant : option.getDeclaringClass().getEnumConstants()) translated(AnimationConfigScreen.valueLabel(constant));
                            var button = (Button)entry.getValue(); button.onPress();
                            ClientSmoke.require(!value.get().equals(option), "enum button advances " + value.key());
                        }
                        settings++;
                    }
                    Button next = navigation(screen, "animatedinventory.configuration.ui.next");
                    if (!next.active) break;
                    next.onPress();
                }
                ClientSmoke.require(settings == ClientConfig.SPEC.values().size(), "all " + settings + " settings display translated labels, sections, tooltips and enum choices");
                ClientConfig.SPEC.save(); ClientConfig.SPEED.set(7.0); ClientConfig.SPEC.load();
                ClientSmoke.require(ClientConfig.SPEED.get() == 2.25, "saved configuration reloads numeric values");
                var path = Path.of("config/animatedinventory-client.json");
                Files.writeString(path, "{\"general\":{\"animation_speed_multiplier\":\"invalid\"}}");
                ClientConfig.SPEED.set(5.0); ClientConfig.ENABLED.set(false); ClientConfig.SPEC.load();
                ClientSmoke.require(ClientConfig.SPEED.get().equals(ClientConfig.SPEED.defaultValue()) && ClientConfig.ENABLED.get().equals(ClientConfig.ENABLED.defaultValue()), "invalid and missing settings reset to defaults on reload");
            } finally { original.forEach(ClientConfigSpec.Value::parse); ClientConfig.SPEC.save(); }
            mc.setScreen(new AnimationConfigScreen(null));
            ClientSmoke.capture("14-translated-config");
            phase = 1; return false;
        }
        if (phase == 1) {
            navigation(mc.screen, "animatedinventory.configuration.ui.next").onPress();
            ClientSmoke.capture("18-translated-enum-controls"); phase = 10; return false;
        }
        if (phase == 10) {
            mc.setScreen(new AnimationConfigScreen(null));
            while (!controls((AnimationConfigScreen)mc.screen).containsKey(ClientConfig.BNS)) navigation(mc.screen, "animatedinventory.configuration.ui.next").onPress();
            ClientSmoke.capture("19-unavailable-integration"); phase = 11; return false;
        }
        if (phase == 11) {
            mc.getLanguageManager().setSelected("de_de");
            reload = mc.reloadResourcePacks(); phase = 2; return false;
        }
        if (phase == 2) {
            if (!reload.isDone()) return false;
            reload.join();
            ClientSmoke.require(Component.translatable("animatedinventory.configuration.title").getString().equals("Animationsprüfung"), "language resource reload updates translated title");
            ClientSmoke.require(Component.translatable("animatedinventory.configuration.general").getString().equals("Allgemein"), "language resource reload updates translated section");
            ClientSmoke.require(AnimationConfigScreen.label(ClientConfig.SPEED).getString().equals("Animation speed"), "missing selected-language entry falls back to English");
            mc.setScreen(new AnimationConfigScreen(null)); ClientSmoke.capture("15-language-reload-fallback"); phase = 3; return false;
        }
        if (phase == 3) { mc.getLanguageManager().setSelected("en_us"); reload = mc.reloadResourcePacks(); phase = 4; return false; }
        if (!reload.isDone()) return false;
        reload.join();
        ClientSmoke.require(Component.translatable("animatedinventory.configuration.title").getString().equals("Animated Inventory"), "English resource reload restores original labels");
        mc.setScreen(new AnimationConfigScreen(null)); return true;
    }
    @SuppressWarnings("unchecked") private static Map<ClientConfigSpec.Value<?>,AbstractWidget> controls(AnimationConfigScreen screen) throws Exception {
        var field = AnimationConfigScreen.class.getDeclaredField("controls"); field.setAccessible(true); return (Map<ClientConfigSpec.Value<?>,AbstractWidget>)field.get(screen);
    }
    private static int page(AnimationConfigScreen screen) throws Exception { var field = AnimationConfigScreen.class.getDeclaredField("page"); field.setAccessible(true); return field.getInt(screen); }
    private static Component error(AnimationConfigScreen screen) throws Exception { var field = AnimationConfigScreen.class.getDeclaredField("error"); field.setAccessible(true); return (Component)field.get(screen); }
    private static Button navigation(Screen screen, String key) {
        String label = Component.translatable(key).getString();
        return screen.children().stream().filter(child -> child instanceof Button button && button.getMessage().getString().equals(label)).map(child -> (Button)child).findFirst().orElseThrow();
    }
    private static void translated(Component component) throws Exception {
        String text = component.getString();
        ClientSmoke.require(!text.isBlank() && !text.contains("animatedinventory.configuration."), "resolved UI text: " + text.replace('\n', ' '));
    }
}
