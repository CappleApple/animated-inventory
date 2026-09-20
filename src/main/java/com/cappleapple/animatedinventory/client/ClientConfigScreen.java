package com.cappleapple.animatedinventory.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import java.lang.reflect.Field;
import java.util.*;

/** Paged, translated editor backed by the same validated values as the TOML file. */
public final class ClientConfigScreen extends Screen {
    private record Entry(ForgeConfigSpec.ConfigValue<?> config, String key, String section) {
        Component label() { return Component.translatable(key); }
        Component tooltip() { return label().copy().append("\n").append(Component.translatable(key + ".tooltip")); }
    }
    private final Screen parent;
    private final List<Entry> entries = new ArrayList<>();
    private final List<List<Entry>> pages = new ArrayList<>();
    private final Map<Entry, Object> pending = new LinkedHashMap<>();
    private final Set<Entry> invalid = new HashSet<>();
    private int page, left, labelWidth, valueWidth;
    private Button save, previous, next;

    public ClientConfigScreen(Screen parent) {
        super(Component.translatable("animatedinventory.configuration.title"));
        this.parent = parent;
        try {
            for (Field field : ClientConfig.class.getFields()) {
                if (field.get(null) instanceof ForgeConfigSpec.ConfigValue<?> value) {
                    List<String> path = value.getPath();
                    Entry entry = new Entry(value, "animatedinventory.configuration." + path.get(path.size() - 1), path.get(0));
                    entries.add(entry); pending.put(entry, value.get());
                }
            }
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    public static Component valueLabel(Object value) {
        if (value instanceof Boolean flag) return Component.translatable(flag ? "options.on" : "options.off");
        if (value instanceof Enum<?> option) return Component.translatable(enumKey(option));
        return Component.literal(value.toString());
    }

    public static String enumKey(Enum<?> value) {
        return "animatedinventory.configuration.enum." + value.getDeclaringClass().getSimpleName().toLowerCase(Locale.ROOT)
                + "." + value.name().toLowerCase(Locale.ROOT);
    }

    @Override protected void init() {
        invalid.clear();
        pages.clear();
        int rows = Math.max(1, (height - 120) / 24);
        List<Entry> current = null;
        String section = "";
        for (Entry entry : entries) {
            if (!entry.section().equals(section) || current == null || current.size() == rows) {
                current = new ArrayList<>(); pages.add(current); section = entry.section();
            }
            current.add(entry);
        }
        page = Math.min(page, pages.size() - 1);
        int tableWidth = Math.min(620, width - 24);
        left = (width - tableWidth) / 2;
        valueWidth = Math.min(180, tableWidth / 2 - 8);
        labelWidth = tableWidth - valueWidth - 12;
        List<Entry> visible = pages.get(page);
        for (int i = 0; i < visible.size(); i++) {
            Entry entry = visible.get(i);
            Object value = pending.get(entry);
            int y = 55 + i * 24, x = left + labelWidth + 12;
            if (value instanceof Boolean || value instanceof Enum<?>) {
                Button input = addRenderableWidget(Button.builder(valueLabel(value), button -> {
                    Object selected = pending.get(entry), replacement;
                    if (selected instanceof Boolean flag) replacement = !flag;
                    else { Enum<?> e = (Enum<?>) selected; Object[] choices = e.getDeclaringClass().getEnumConstants(); replacement = choices[(e.ordinal() + 1) % choices.length]; }
                    pending.put(entry, replacement); button.setMessage(valueLabel(replacement));
                }).bounds(x, y, valueWidth, 20).tooltip(Tooltip.create(entry.tooltip())).build());
                if (entry.config() == ClientConfig.BNS) { input.active = false; input.setMessage(Component.translatable("animatedinventory.configuration.unavailable")); }
            } else {
                EditBox input = new EditBox(font, x, y, valueWidth, 20, entry.label());
                input.setValue(value.toString());
                input.setTooltip(Tooltip.create(entry.tooltip()));
                input.setResponder(text -> {
                    try {
                        Object parsed;
                        if (value instanceof Integer) parsed = Integer.valueOf(text);
                        else parsed = Double.valueOf(text);
                        ForgeConfigSpec.ValueSpec spec = ClientConfig.SPEC.getSpec().get(entry.config().getPath());
                        if (!spec.test(parsed)) throw new IllegalArgumentException();
                        pending.put(entry, parsed); invalid.remove(entry); input.setTextColor(0xFFFFFF);
                        input.setTooltip(Tooltip.create(entry.tooltip()));
                    } catch (IllegalArgumentException error) {
                        invalid.add(entry); input.setTextColor(0xFF5555);
                        input.setTooltip(Tooltip.create(Component.translatable("animatedinventory.configuration.invalid_number")));
                    }
                    refreshActions();
                });
                addRenderableWidget(input);
            }
        }
        previous = addRenderableWidget(Button.builder(Component.translatable("animatedinventory.configuration.previous"), b -> { page--; rebuildWidgets(); }).bounds(width / 2 - 156, height - 27, 70, 20).build());
        next = addRenderableWidget(Button.builder(Component.translatable("animatedinventory.configuration.next"), b -> { page++; rebuildWidgets(); }).bounds(width / 2 - 82, height - 27, 70, 20).build());
        save = addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> save()).bounds(width / 2 - 8, height - 27, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(width / 2 + 76, height - 27, 80, 20).build());
        refreshActions();
    }

    private void refreshActions() {
        if (save == null || previous == null || next == null) return;
        save.active = invalid.isEmpty();
        previous.active = invalid.isEmpty() && page > 0;
        next.active = invalid.isEmpty() && page + 1 < pages.size();
    }

    @SuppressWarnings({"rawtypes", "unchecked"}) private void save() {
        if (!invalid.isEmpty()) return;
        pending.forEach((entry, value) -> ((ForgeConfigSpec.ConfigValue) entry.config()).set(value));
        ClientConfig.SPEC.save(); onClose();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("animatedinventory.configuration." + pages.get(page).get(0).section()), width / 2, 33, 0xFFFFA0);
        graphics.drawCenteredString(font, Component.translatable("animatedinventory.configuration.page", page + 1, pages.size()), width / 2, height - 43, 0xAAAAAA);
        List<Entry> visible = pages.get(page);
        for (int i = 0; i < visible.size(); i++)
            graphics.drawString(font, font.substrByWidth(visible.get(i).label(), labelWidth).getString(), left, 61 + i * 24, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
        for (int i = 0; i < visible.size(); i++) {
            if (mouseX >= left && mouseX < left + labelWidth && mouseY >= 55 + i * 24 && mouseY < 75 + i * 24)
                graphics.renderTooltip(font, font.split(visible.get(i).tooltip(), Math.min(width - 32, 320)), mouseX, mouseY);
        }
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
