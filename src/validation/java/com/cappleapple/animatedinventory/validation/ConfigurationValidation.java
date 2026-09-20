package com.cappleapple.animatedinventory.validation;

import com.cappleapple.animatedinventory.client.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TranslatableEnum;
import java.nio.file.*;
import java.util.*;

/** Exercises NeoForge's actual configuration editor and file watcher after language resources load. */
public final class ConfigurationValidation {
    private static int stage, section;
    private static long next, reloadDeadline;
    private static Screen root;
    private static final List<String> sections = new ArrayList<>();
    private static java.util.concurrent.CompletableFuture<Void> resources;
    private static final String PREFIX = "animatedinventory.configuration.";
    public static boolean tick() throws Exception {
        var mc = Minecraft.getInstance();
        if (System.nanoTime() < next) return false;
        switch (stage) {
            case 0 -> {
                if (Boolean.getBoolean("animatedinventory.validateJar")) {
                    String origin = com.cappleapple.animatedinventory.AnimatedInventory.class.getResource("AnimatedInventory.class").toExternalForm();
                    require(origin.contains(".jar"), "production mod loaded from built JAR: " + origin);
                }
                int values = 0, enumChoices = 0;
                for (var field : ClientConfig.class.getFields()) {
                    if (!(field.get(null) instanceof ModConfigSpec.ConfigValue<?> value)) continue;
                    var path = value.getPath(); String key = path.getLast();
                    translated(PREFIX + key); translated(PREFIX + key + ".tooltip"); values++;
                    if (!sections.contains(path.getFirst())) { sections.add(path.getFirst()); translated(PREFIX + path.getFirst()); translated(PREFIX + path.getFirst() + ".tooltip"); }
                    if (value.getDefault() instanceof Enum<?> current) for (Object choice : current.getDeclaringClass().getEnumConstants()) {
                        require(choice instanceof TranslatableEnum, "native enum implements translation contract: " + choice);
                        String keyName = PREFIX + "value." + ((Enum<?>)choice).name().toLowerCase(Locale.ROOT);
                        translated(keyName);
                        require(((TranslatableEnum)choice).getTranslatedName().getString().equals(Language.getInstance().getOrDefault(keyName)), "native enum label resolves: " + choice);
                        enumChoices++;
                    }
                }
                require(values == 57 && sections.size() == 14, "all 57 setting labels/tooltips and 14 sections resolve in live language resources");
                pass("all " + enumChoices + " configured enum choices resolve through native labels");
                translated(PREFIX + "title"); translated(PREFIX + "section.animatedinventory.client.toml"); translated(PREFIX + "section.animatedinventory.client.toml.title");
                open(); stage = 1;
            }
            case 1 -> { root = mc.gui.screen(); openSection("general"); stage = 2; }
            case 2 -> {
                inspect(mc.gui.screen()); capture();
                EditBox speed = row(mc.gui.screen(), Language.getInstance().getOrDefault(PREFIX + "animation_speed_multiplier")).stream()
                    .filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
                speed.setValue("1.75"); require(ClientConfig.SPEED.get() == 1.75, "native config editor changes valid speed immediately");
                speed.setValue("-1"); require(ClientConfig.SPEED.get() == 1.75, "native config editor rejects out-of-range speed");
                speed.setValue("1.75");
                mc.gui.screen().onClose(); mc.gui.screen().onClose(); stage = 3;
            }
            case 3 -> {
                Path file = Path.of("config/animatedinventory-client.toml"); String saved = Files.readString(file);
                require(saved.contains("animation_speed_multiplier = 1.75"), "closing native editor persists setting to TOML");
                Files.writeString(file, saved.replace("animation_speed_multiplier = 1.75", "animation_speed_multiplier = 2.25"));
                reloadDeadline = System.nanoTime() + 15_000_000_000L; stage = 4;
            }
            case 4 -> {
                if (ClientConfig.SPEED.get() != 2.25) { if (System.nanoTime() > reloadDeadline) throw new AssertionError("NeoForge config file watcher did not reload speed"); return false; }
                pass("NeoForge file watcher reloads edited TOML into active settings");
                ClientConfig.SPEED.set(1.0); ClientConfig.SPEC.save(); open(); stage = 5;
            }
            case 5 -> {
                root = mc.gui.screen();
                if (section == sections.size()) {
                    mc.getLanguageManager().setSelected("de_de");
                    resources = mc.reloadResourcePacks(); stage = 7;
                } else { openSection(sections.get(section)); stage = 6; }
            }
            case 6 -> {
                inspect(mc.gui.screen()); capture();
                pass("localized native config section rendered: " + sections.get(section++));
                mc.gui.screen().onClose(); stage = 5;
            }
            case 7 -> {
                if (!resources.isDone() || mc.gui.overlay() != null) return false;
                resources.join(); translated(PREFIX + "animations_enabled");
                require(Language.getInstance().getOrDefault(PREFIX + "animations_enabled").equals("Enable animations"), "untranslated locale uses bundled English fallback");
                open(); stage = 8;
            }
            case 8 -> { root = mc.gui.screen(); openSection("general"); stage = 9; }
            case 9 -> {
                inspect(mc.gui.screen()); capture(); pass("native configuration rendered with German UI and English mod fallback");
                mc.getLanguageManager().setSelected("en_us"); resources = mc.reloadResourcePacks(); stage = 10;
            }
            case 10 -> {
                if (!resources.isDone() || mc.gui.overlay() != null) return false;
                resources.join(); pass("configuration validation complete"); return true;
            }
            default -> { return true; }
        }
        next = System.nanoTime() + 180_000_000L;
        return false;
    }
    private static void open() {
        var mc = Minecraft.getInstance();
        mc.gui.setScreen(new ConfigurationScreen(ModList.get().getModContainerById("animatedinventory").orElseThrow(), mc.gui.screen()));
    }
    private static void openSection(String key) throws Exception {
        var widgets = row(root, Language.getInstance().getOrDefault(PREFIX + key));
        Button button = widgets.stream().filter(Button.class::isInstance).map(Button.class::cast).findFirst().orElseThrow();
        button.onPress(new net.minecraft.client.input.MouseButtonEvent(button.getX() + 2, button.getY() + 2,
            new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)));
    }
    private static List<AbstractWidget> row(GuiEventListener listener, String name) {
        if (listener instanceof ContainerEventHandler container) {
            var direct = container.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).toList();
            if (direct.stream().anyMatch(widget -> widget.getMessage().getString().contains(name))) return direct;
            for (GuiEventListener child : container.children()) { var found = row(child, name); if (!found.isEmpty()) return found; }
        }
        return List.of();
    }
    private static void inspect(GuiEventListener listener) throws Exception {
        if (listener instanceof AbstractWidget widget) {
            String text = widget.getMessage().getString();
            if (text.contains("animatedinventory.configuration.") || text.contains("EASE_OUT_") || text.contains("FADE_SCALE") || text.contains("FOLLOW_CURSOR"))
                throw new AssertionError("Unlocalized visible configuration text: " + text);
        }
        if (listener instanceof ContainerEventHandler container) for (GuiEventListener child : container.children()) inspect(child);
    }
    private static void translated(String key) { if (!Language.getInstance().has(key)) throw new AssertionError("Missing live translation: " + key); }
    private static void capture() { if (Boolean.getBoolean("animatedinventory.captureValidation")) net.minecraft.client.Screenshot.grab(Minecraft.getInstance(), false); }
    private static void require(boolean value, String message) throws Exception { if (!value) throw new AssertionError(message); pass(message); }
    private static void pass(String message) throws Exception { Files.writeString(Path.of("validation.txt"), "PASS " + message + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
    private ConfigurationValidation() { }
}