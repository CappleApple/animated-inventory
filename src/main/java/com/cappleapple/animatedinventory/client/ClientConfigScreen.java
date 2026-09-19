package com.cappleapple.animatedinventory.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import java.lang.reflect.Field;
import java.util.*;

/** Small paged editor backed by the same validated Forge config values as the TOML file. */
public final class ClientConfigScreen extends Screen {
    private record Entry(ForgeConfigSpec.ConfigValue<?> config, Component label) { }
    private final Screen parent;
    private final List<Entry> entries = new ArrayList<>();
    private final Map<Entry, Object> pending = new LinkedHashMap<>();
    private final Set<Entry> invalid = new HashSet<>();
    private int page, rows;
    private Button save;
    public ClientConfigScreen(Screen parent) {
        super(Component.translatable("animatedinventory.configuration.title"));
        this.parent = parent;
        try {
            for (Field field : ClientConfig.class.getFields()) {
                if (field.get(null) instanceof ForgeConfigSpec.ConfigValue<?> value) {
                    List<String> path = value.getPath();
                    Entry entry = new Entry(value, Component.translatable("animatedinventory.configuration." + path.get(path.size() - 1)));
                    entries.add(entry); pending.put(entry, value.get());
                }
            }
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    @Override protected void init() {
        invalid.clear();
        rows = Math.max(1, (height - 90) / 30);
        int pages = (entries.size() + rows - 1) / rows;
        page = Math.min(page, pages - 1);
        for (int i = page * rows; i < Math.min(entries.size(), (page + 1) * rows); i++) {
            Entry entry = entries.get(i);
            Object value = pending.get(entry);
            int y = 35 + (i % rows) * 30;
            int x = width / 2 + 30;
            if (value instanceof Boolean || value instanceof Enum<?>) {
                addRenderableWidget(Button.builder(Component.literal(value.toString()), button -> {
                    Object current = pending.get(entry), next;
                    if (current instanceof Boolean flag) next = !flag;
                    else { Enum<?> e = (Enum<?>) current; Object[] options = e.getDeclaringClass().getEnumConstants(); next = options[(e.ordinal() + 1) % options.length]; }
                    pending.put(entry, next); button.setMessage(Component.literal(next.toString()));
                }).bounds(x, y, Math.min(180, width / 2 - 40), 20).build());
            } else {
                EditBox input = new EditBox(font, x, y, Math.min(180, width / 2 - 40), 20, entry.label());
                input.setValue(value.toString());
                input.setResponder(text -> {
                    try {
                        Object parsed;
                        if (value instanceof Integer) parsed = Integer.valueOf(text);
                        else parsed = Double.valueOf(text);
                        ForgeConfigSpec.ValueSpec spec = ClientConfig.SPEC.getSpec().get(entry.config().getPath());
                        if (!spec.test(parsed)) throw new IllegalArgumentException();
                        pending.put(entry, parsed); invalid.remove(entry); input.setTextColor(0xFFFFFF);
                    } catch (IllegalArgumentException error) { invalid.add(entry); input.setTextColor(0xFF5555); }
                    save.active = invalid.isEmpty();
                });
                addRenderableWidget(input);
            }
        }
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); }).bounds(width / 2 - 160, height - 28, 30, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); }).bounds(width / 2 - 125, height - 28, 30, 20).build()).active = page + 1 < pages;
        save = addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> save()).bounds(width / 2 - 85, height - 28, 110, 20).build());
        save.active = invalid.isEmpty();
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(width / 2 + 30, height - 28, 110, 20).build());
    }
    @SuppressWarnings({"rawtypes", "unchecked"}) private void save() {
        if (!invalid.isEmpty()) return;
        pending.forEach((entry, value) -> ((ForgeConfigSpec.ConfigValue) entry.config()).set(value));
        ClientConfig.SPEC.save(); onClose();
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        for (int i = page * rows; i < Math.min(entries.size(), (page + 1) * rows); i++) {
            graphics.drawString(font, font.substrByWidth(entries.get(i).label(), Math.max(70, width / 2)).getString(), 12, 41 + (i % rows) * 30, 0xFFFFFF);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
