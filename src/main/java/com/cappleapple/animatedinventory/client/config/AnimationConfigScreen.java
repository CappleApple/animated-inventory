package com.cappleapple.animatedinventory.client.config;

import com.cappleapple.animatedinventory.client.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;
import java.util.*;

/** Paged, localized controls for the client configuration. */
public final class AnimationConfigScreen extends Screen {
    private static final String PREFIX = "animatedinventory.configuration.";
    private static final int ROW_HEIGHT = 36;
    private final Screen parent;
    private final List<ClientConfigSpec.Value<?>> settings = new ArrayList<>(ClientConfig.SPEC.values());
    private final Map<ClientConfigSpec.Value<?>, EditBox> fields = new LinkedHashMap<>();
    private final Map<ClientConfigSpec.Value<?>, AbstractWidget> controls = new LinkedHashMap<>();
    private int page;
    private Component error = Component.empty();
    private int rows() { return Math.max(1, (height - 104) / ROW_HEIGHT); }
    private int panelWidth() { return Math.min(width - 24, 600); }
    private int left() { return (width - panelWidth()) / 2; }
    private int valueWidth() { return Math.max(92, Math.min(180, panelWidth() / 3)); }
    private int controlX() { return left() + panelWidth() - valueWidth(); }
    public AnimationConfigScreen(Screen parent) { super(Component.translatable(PREFIX + "title")); this.parent = parent; }

    public static Component label(ClientConfigSpec.Value<?> value) {
        return Component.translatable(PREFIX + value.key().substring(value.key().indexOf('.') + 1));
    }
    public static Component section(ClientConfigSpec.Value<?> value) {
        return Component.translatable(PREFIX + value.key().substring(0, value.key().indexOf('.')));
    }
    public static Component valueLabel(Object value) {
        if (value instanceof Boolean enabled) return Component.translatable(enabled ? "options.on" : "options.off");
        if (value instanceof Enum<?> option) return Component.translatable(PREFIX + "value." + option.name().toLowerCase(Locale.ROOT));
        return Component.literal(String.valueOf(value));
    }
    public static Component description(ClientConfigSpec.Value<?> value) {
        String key = PREFIX + value.key().substring(value.key().indexOf('.') + 1) + ".tooltip";
        var text = label(value).copy();
        if (Language.getInstance().has(key)) text.append("\n").append(Component.translatable(key));
        text.append("\n").append(Component.translatable(PREFIX + "ui.default", valueLabel(value.defaultValue())));
        if (value.minimum() != null) text.append("\n").append(Component.translatable(PREFIX + "ui.range", value.minimum(), value.maximum()));
        return text;
    }
    @Override protected void init() {
        fields.clear(); controls.clear();
        int count = rows();
        page = Math.min(page, (settings.size() - 1) / count);
        for (int i = page * count; i < Math.min(settings.size(), (page + 1) * count); i++) {
            var value = settings.get(i);
            int y = 42 + (i % count) * ROW_HEIGHT;
            AbstractWidget control;
            if (value.get() instanceof Boolean || value.get() instanceof Enum<?>) {
                control = Button.builder(value == ClientConfig.BNS ? Component.translatable(PREFIX + "ui.unavailable") : valueLabel(value.get()), button -> {
                    if (value == ClientConfig.BNS || !apply()) return;
                    if (value.get() instanceof Boolean enabled) value.parse(Boolean.toString(!enabled));
                    else {
                        Enum<?> current = (Enum<?>) value.get();
                        Enum<?>[] options = current.getDeclaringClass().getEnumConstants();
                        value.parse(options[(current.ordinal() + 1) % options.length].name());
                    }
                    ClientConfig.SPEC.save(); button.setMessage(valueLabel(value.get()));
                }).bounds(controlX(), y, valueWidth(), 20).build();
            } else {
                var field = new EditBox(font, controlX(), y, valueWidth(), 20, label(value));
                field.setMaxLength(80); field.setValue(String.valueOf(value.get()));
                fields.put(value, field); control = field;
            }
            control.active = value != ClientConfig.BNS;
            control.setTooltip(Tooltip.create(description(value)));
            controls.put(value, addRenderableWidget(control));
        }
        int gap = 6, buttonWidth = (panelWidth() - gap * 3) / 4;
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + "ui.previous"), button -> {
            if (apply()) { page--; rebuildWidgets(); }
        }).bounds(left(), height - 28, buttonWidth, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + "ui.reset_page"), button -> {
            controls.keySet().forEach(ClientConfigSpec.Value::reset);
            ClientConfig.SPEC.save(); error = Component.empty(); rebuildWidgets();
        }).bounds(left() + (buttonWidth + gap), height - 28, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
            .bounds(left() + 2 * (buttonWidth + gap), height - 28, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + "ui.next"), button -> {
            if (apply()) { page++; rebuildWidgets(); }
        }).bounds(left() + 3 * (buttonWidth + gap), height - 28, buttonWidth, 20).build()).active = (page + 1) * count < settings.size();
    }
    private boolean apply() {
        Map<ClientConfigSpec.Value<?>, String> previous = new LinkedHashMap<>();
        for (var entry : fields.entrySet()) {
            previous.put(entry.getKey(), String.valueOf(entry.getKey().get()));
            try { entry.getKey().parse(entry.getValue().getValue()); }
            catch (RuntimeException invalid) {
                previous.forEach(ClientConfigSpec.Value::parse);
                error = Component.translatable(PREFIX + "ui.invalid_value", label(entry.getKey()));
                return false;
            }
        }
        ClientConfig.SPEC.save(); error = Component.empty(); return true;
    }
    @Override public void onClose() { if (apply()) minecraft.setScreen(parent); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xffffffff);
        graphics.drawCenteredString(font, Component.translatable(PREFIX + "ui.page", page + 1, (settings.size() + rows() - 1) / rows()), width / 2, 25, 0xffaaaaaa);
        int row = 0;
        for (var value : controls.keySet()) {
            int y = 42 + row * ROW_HEIGHT;
            var lines = font.split(label(value), Math.max(20, controlX() - left() - 10));
            for (int line = 0; line < Math.min(2, lines.size()); line++) graphics.drawString(font, lines.get(line), left(), y + line * 9, 0xffffffff, false);
            graphics.drawString(font, section(value), left(), y + 21, 0xffaaaaaa, false);
            if (mouseX >= left() && mouseX < controlX() && mouseY >= y && mouseY < y + ROW_HEIGHT) {
                graphics.renderTooltip(font, font.split(description(value), 300), mouseX, mouseY);
            }
            row++;
        }
        if (!error.getString().isEmpty()) graphics.drawCenteredString(font, error, width / 2, height - 45, 0xffff6666);
    }
}
