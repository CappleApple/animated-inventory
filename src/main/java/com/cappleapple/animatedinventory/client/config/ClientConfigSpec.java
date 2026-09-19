package com.cappleapple.animatedinventory.client.config;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

/** Client JSON settings with the same keys, defaults and bounds as the original mod. */
public final class ClientConfigSpec {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Value<?>> values;
    private final Path path = FabricLoader.getInstance().getConfigDir().resolve("animatedinventory-client.json");
    private ClientConfigSpec(Map<String, Value<?>> values) { this.values = Map.copyOf(values); }
    public Collection<Value<?>> values() { return values.values().stream().sorted(Comparator.comparing(Value::key)).toList(); }
    public void load() {
        if (!Files.exists(path)) { save(); return; }
        try (var reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Value<?> value : values.values()) {
                String[] parts = value.key.split("\\.");
                if (root.has(parts[0]) && root.get(parts[0]).isJsonObject()) {
                    JsonObject section = root.getAsJsonObject(parts[0]);
                    if (section.has(parts[1])) {
                        try { value.parse(section.get(parts[1]).getAsString()); }
                        catch (RuntimeException error) { LogUtils.getLogger().warn("Animated Inventory ignored invalid config value {}", value.key); }
                    }
                }
            }
        } catch (IOException | RuntimeException error) { LogUtils.getLogger().warn("Animated Inventory could not load {}", path, error); }
    }
    public void save() {
        JsonObject root = new JsonObject();
        for (Value<?> value : values()) {
            String[] parts = value.key.split("\\.");
            if (!root.has(parts[0])) root.add(parts[0], new JsonObject());
            root.getAsJsonObject(parts[0]).add(parts[1], JSON.toJsonTree(value.get()));
        }
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            try (var writer = Files.newBufferedWriter(temporary)) { JSON.toJson(root, writer); }
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) { LogUtils.getLogger().warn("Animated Inventory could not save {}", path, error); }
    }
    public static class Value<T> {
        private final String key;
        private final T defaultValue;
        private T value;
        private final Function<String, T> parser;
        private Value(String key, T value, Function<String,T> parser) { this.key = key; this.value = value; this.defaultValue = value; this.parser = parser; }
        public T get() { return value; }
        public void set(T value) { this.value = parser.apply(String.valueOf(value)); }
        public String key() { return key; }
        public void reset() { value = defaultValue; }
        public void parse(String input) { value = parser.apply(input); }
    }
    public static final class BooleanValue extends Value<Boolean> {
        private BooleanValue(String key, boolean value) { super(key, value, input -> { if (!input.equalsIgnoreCase("true") && !input.equalsIgnoreCase("false")) throw new IllegalArgumentException(input); return Boolean.parseBoolean(input); }); }
    }
    public static final class IntValue extends Value<Integer> {
        private IntValue(String key, int value, int min, int max) { super(key, value, input -> { int result = Integer.parseInt(input); if (result < min || result > max) throw new IllegalArgumentException(input); return result; }); }
    }
    public static final class DoubleValue extends Value<Double> {
        private DoubleValue(String key, double value, double min, double max) { super(key, value, input -> { double result = Double.parseDouble(input); if (!Double.isFinite(result) || result < min || result > max) throw new IllegalArgumentException(input); return result; }); }
    }
    public static final class EnumValue<T extends Enum<T>> extends Value<T> {
        private EnumValue(String key, T value) { super(key, value, input -> Enum.valueOf(value.getDeclaringClass(), input.toUpperCase(Locale.ROOT))); }
    }
    public static final class Builder {
        private final Map<String, Value<?>> values = new LinkedHashMap<>();
        private String section;
        public Builder push(String section) { this.section = section; return this; }
        public Builder pop() { section = null; return this; }
        private String key(String name) { return Objects.requireNonNull(section) + "." + name; }
        private <T extends Value<?>> T add(T value) { values.put(value.key(), value); return value; }
        public BooleanValue define(String name, boolean value) { return add(new BooleanValue(key(name), value)); }
        public IntValue defineInRange(String name, int value, int min, int max) { return add(new IntValue(key(name), value, min, max)); }
        public DoubleValue defineInRange(String name, double value, double min, double max) { return add(new DoubleValue(key(name), value, min, max)); }
        public <T extends Enum<T>> EnumValue<T> defineEnum(String name, T value) { return add(new EnumValue<>(key(name), value)); }
        public ClientConfigSpec build() { return new ClientConfigSpec(values); }
    }
}
