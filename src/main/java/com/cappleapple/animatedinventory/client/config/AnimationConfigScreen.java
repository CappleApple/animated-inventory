package com.cappleapple.animatedinventory.client.config;

import com.cappleapple.animatedinventory.client.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Paged editor for every client setting; invalid input keeps the editor open. */
public final class AnimationConfigScreen extends Screen {
    private final Screen parent;
    private final List<ClientConfigSpec.Value<?>> settings = new ArrayList<>(ClientConfig.SPEC.values());
    private final Map<ClientConfigSpec.Value<?>, EditBox> fields = new LinkedHashMap<>();
    private int page;
    private String error = "";
    private int rows() { return Math.max(1, (height - 105) / 32); }
    public AnimationConfigScreen(Screen parent) { super(Component.literal("Animated Inventory")); this.parent = parent; }
    @Override protected void init() {
        fields.clear();
        int count = rows();
        page = Math.min(page, (settings.size() - 1) / count);
        for (int i = page * count; i < Math.min(settings.size(), (page + 1) * count); i++) {
            var value = settings.get(i);
            var field = new EditBox(font, width / 2 + 65, 42 + (i % count) * 32, 105, 20, Component.literal(value.key()));
            field.setMaxLength(80);
            field.setValue(String.valueOf(value.get()));
            addRenderableWidget(field);
            fields.put(value, field);
        }
        addRenderableWidget(Button.builder(Component.literal("Previous"), button -> { if (apply()) { page--; rebuildWidgets(); } }).bounds(width / 2 - 170, height - 30, 100, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose()).bounds(width / 2 - 50, height - 30, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Next"), button -> { if (apply()) { page++; rebuildWidgets(); } }).bounds(width / 2 + 70, height - 30, 100, 20).build()).active = (page + 1) * count < settings.size();
    }
    private boolean apply() {
        Map<ClientConfigSpec.Value<?>, String> previous = new LinkedHashMap<>();
        for (var entry : fields.entrySet()) {
            previous.put(entry.getKey(), String.valueOf(entry.getKey().get()));
            try { entry.getKey().parse(entry.getValue().getValue()); }
            catch (RuntimeException invalid) {
                previous.forEach(ClientConfigSpec.Value::parse);
                error = "Invalid value: " + entry.getKey().key();
                return false;
            }
        }
        ClientConfig.SPEC.save(); error = ""; return true;
    }
    @Override public void onClose() { if (apply()) minecraft.setScreen(parent); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xffffff);
        graphics.drawCenteredString(font, "Page " + (page + 1) + " / " + ((settings.size() + rows() - 1) / rows()), width / 2, 26, 0xaaaaaa);
        int row = 0;
        for (var entry : fields.entrySet()) {
            String key = entry.getKey().key(); int dot = key.indexOf('.');
            graphics.drawString(font, key.substring(dot + 1), width / 2 - 170, 42 + row * 32, 0xffffff, false);
            graphics.drawString(font, key.substring(0, dot), width / 2 - 170, 52 + row * 32, 0x999999, false);
            if (entry.getKey().get() instanceof Enum<?> option && entry.getValue().isHovered()) {
                String allowed = Arrays.toString(option.getDeclaringClass().getEnumConstants());
                graphics.renderTooltip(font, Component.literal(allowed), mouseX, mouseY);
            }
            row++;
        }
        if (!error.isEmpty()) graphics.drawCenteredString(font, error, width / 2, height - 48, 0xff6666);
    }
}
