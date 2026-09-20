package com.cappleapple.animatedinventory.validation;

import com.cappleapple.animatedinventory.client.ClientConfig;
import com.cappleapple.animatedinventory.client.config.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.input.*;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import com.google.gson.*;
import java.nio.file.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Drives production config widgets and language reload inside the running client. */
public final class ConfigValidation {
    private static final String PREFIX = "animatedinventory.configuration.";
    private static int stage, pages;
    private static long next;
    private static AnimationConfigScreen screen;
    private static CompletableFuture<Void> reload;
    private static String englishDone;
    private static final Map<ClientConfigSpec.Value<?>, String> original = new LinkedHashMap<>();
    public static boolean tick() throws Exception {
        var mc = Minecraft.getInstance();
        if (System.nanoTime() < next || mc.gui.overlay() != null) return false;
        switch (stage) {
            case 0 -> {
                for (var value : ClientConfig.SPEC.values()) original.put(value, String.valueOf(value.get()));
                openF8();
                require(screen.getTitle().getContents() instanceof TranslatableContents, "config title uses a translation component");
                validateCatalog(); englishDone = I18n.get("gui.done");
                stage++;
            }
            case 1 -> {
                require(Minecraft.getInstance().gui.screen() == screen, "localized config rendered after actual F8 keyboard dispatch");
                capture();
                boolean enabled = ClientConfig.ENABLED.get(); click(control(ClientConfig.ENABLED));
                require(ClientConfig.ENABLED.get() != enabled, "localized boolean control changes actual config value");
                click(control(ClientConfig.ENABLED));
                fields().get(ClientConfig.SPEED).setValue("1.75"); click(button("gui.done"));
                require(mc.gui.screen() != screen && ClientConfig.SPEED.get() == 1.75, "Done commits numeric input and closes editor");
                ClientConfig.SPEED.set(.25); ClientConfig.SPEC.load();
                require(ClientConfig.SPEED.get() == 1.75, "saved numeric configuration reloads from JSON");
                openF8(); fields().get(ClientConfig.SPEED).setValue("NaN"); click(button("gui.done"));
                require(mc.gui.screen() == screen && ClientConfig.SPEED.get() == 1.75, "invalid numeric input stays in editor without corrupting config");
                Component error = (Component) field("error").get(screen);
                require(error.getContents() instanceof TranslatableContents && !error.getString().contains(PREFIX), "invalid-input error resolves its translation");
                stage++;
            }
            case 2 -> {
                capture(); fields().get(ClientConfig.SPEED).setValue("1.25"); click(button(PREFIX + "ui.next"));
                require(ClientConfig.SPEED.get() == 1.25, "page navigation saves valid numeric edits");
                while (!controls().containsKey(ClientConfig.INTERRUPTION)) click(button(PREFIX + "ui.next"));
                var previous = ClientConfig.INTERRUPTION.get(); click(control(ClientConfig.INTERRUPTION));
                require(ClientConfig.INTERRUPTION.get() != previous, "localized enum button cycles real enum value");
                require(control(ClientConfig.INTERRUPTION).getMessage().getString().equals(AnimationConfigScreen.valueLabel(ClientConfig.INTERRUPTION.get()).getString()), "enum widget displays localized choice");
                click(button(PREFIX + "ui.reset_page"));
                require(ClientConfig.INTERRUPTION.get() == ClientConfig.INTERRUPTION.defaultValue(), "reset-page control restores default enum");
                while (button(PREFIX + "ui.previous").active) click(button(PREFIX + "ui.previous"));
                stage++;
            }
            case 3 -> {
                validateWidgets(); pages++;
                if (pages == 1 || !button(PREFIX + "ui.next").active) capture();
                if (button(PREFIX + "ui.next").active) click(button(PREFIX + "ui.next"));
                else {
                    pass("all " + pages + " config pages rendered with translated labels, controls and tooltips");
                    while (button(PREFIX + "ui.previous").active) click(button(PREFIX + "ui.previous"));
                    require(mc.getLanguageManager().getLanguage("fr_fr") != null, "French is available for language-reload coverage");
                    mc.options.languageCode = "fr_fr"; mc.getLanguageManager().setSelected("fr_fr");
                    reload = mc.reloadResourcePacks(); stage++;
                }
            }
            case 4 -> {
                if (!reload.isDone()) return false; reload.join();
                require(!I18n.get("gui.done").equals(englishDone), "vanilla controls switch language after resource reload");
                validateCatalog(); validateWidgets();
                require(AnimationConfigScreen.label(ClientConfig.ENABLED).getString().equals("Enable animations"), "untranslated mod strings fall back to supplied English catalog");
                pass("live language switch retains localized mod fallback and translated native controls");
                stage++;
            }
            case 5 -> {
                capture();
                mc.options.languageCode = "en_us"; mc.getLanguageManager().setSelected("en_us");
                reload = mc.reloadResourcePacks(); stage++;
            }
            case 6 -> {
                if (!reload.isDone()) return false; reload.join();
                Path file = FabricLoader.getInstance().getConfigDir().resolve("animatedinventory-client.json");
                JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                json.getAsJsonObject("general").remove("animation_speed_multiplier");
                json.getAsJsonObject("general").addProperty("reduce_motion", "invalid");
                Files.writeString(file, json.toString());
                ClientConfig.SPEED.set(8.0); ClientConfig.REDUCE_MOTION.set(true); ClientConfig.SPEC.load();
                require(ClientConfig.SPEED.get().equals(ClientConfig.SPEED.defaultValue())
                    && ClientConfig.REDUCE_MOTION.get().equals(ClientConfig.REDUCE_MOTION.defaultValue()), "missing and invalid JSON values reset to defaults on reload");
                original.forEach(ClientConfigSpec.Value::parse); ClientConfig.SPEC.save();
                mc.gui.setScreen(null); pass("configuration and localization complete"); return true;
            }
            default -> { return true; }
        }
        next = System.nanoTime() + 200_000_000L; return false;
    }
    private static void validateCatalog() throws Exception {
        var language = Language.getInstance();
        for (var value : ClientConfig.SPEC.values()) {
            String leaf = value.key().substring(value.key().indexOf('.') + 1);
            String section = value.key().substring(0, value.key().indexOf('.'));
            require(language.has(PREFIX + leaf) && language.has(PREFIX + section) && language.has(PREFIX + leaf + ".tooltip"), "runtime localization covers " + value.key());
            require(AnimationConfigScreen.label(value).getContents() instanceof TranslatableContents
                && !AnimationConfigScreen.description(value).getString().contains(PREFIX), "localized label and tooltip resolve for " + value.key());
            if (value.get() instanceof Enum<?> option) for (Enum<?> choice : option.getDeclaringClass().getEnumConstants()) {
                require(language.has(PREFIX + "value." + choice.name().toLowerCase(Locale.ROOT)), "localized enum " + choice.name());
            }
        }
    }
    private static void validateWidgets() throws Exception {
        for (var entry : controls().entrySet()) {
            var widget = entry.getValue();
            require(widget.getX() >= 0 && widget.getY() >= 0 && widget.getX() + widget.getWidth() <= screen.width
                && widget.getY() + widget.getHeight() <= screen.height - 40, "config control fits viewport " + entry.getKey().key());
            require(!widget.getMessage().getString().contains(PREFIX), "visible widget label resolves " + entry.getKey().key());
        }
    }
    private static void openF8() throws Exception {
        var mc = Minecraft.getInstance();
        var method = KeyboardHandler.class.getDeclaredMethod("keyPress", long.class, int.class, KeyEvent.class); method.setAccessible(true);
        method.invoke(mc.keyboardHandler, mc.getWindow().handle(), InputConstants.PRESS, new KeyEvent(InputConstants.KEY_F8, 0, 0));
        require(mc.gui.screen() instanceof AnimationConfigScreen, "actual F8 key handler opens config editor");
        screen = (AnimationConfigScreen) mc.gui.screen();
    }
    private static AbstractWidget control(ClientConfigSpec.Value<?> value) throws Exception { return controls().get(value); }
    @SuppressWarnings("unchecked") private static Map<ClientConfigSpec.Value<?>, AbstractWidget> controls() throws Exception { return (Map<ClientConfigSpec.Value<?>, AbstractWidget>) field("controls").get(screen); }
    @SuppressWarnings("unchecked") private static Map<ClientConfigSpec.Value<?>, EditBox> fields() throws Exception { return (Map<ClientConfigSpec.Value<?>, EditBox>) field("fields").get(screen); }
    private static Field field(String name) throws Exception { var f = AnimationConfigScreen.class.getDeclaredField(name); f.setAccessible(true); return f; }
    private static Button button(String key) { String text = Component.translatable(key).getString(); return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b -> b.getMessage().getString().equals(text)).findFirst().orElseThrow(); }
    private static void click(AbstractWidget widget) {
        var event = new MouseButtonEvent(widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }
    private static void capture() { if (Boolean.getBoolean("animatedinventory.captureValidation")) net.minecraft.client.Screenshot.grab(Minecraft.getInstance(), false); }
    private static void require(boolean value, String message) throws Exception { if (!value) throw new AssertionError(message); pass(message); }
    private static void pass(String message) throws Exception { Files.writeString(Path.of("validation.txt"), "PASS " + message + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
    private ConfigValidation() { }
}
