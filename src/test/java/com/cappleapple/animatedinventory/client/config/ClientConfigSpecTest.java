package com.cappleapple.animatedinventory.client.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ClientConfigSpecTest {
    @TempDir Path directory;
    enum Choice { FIRST, SECOND }
    @Test void savedValuesRoundTripAndPreserveDeclarationOrder() throws Exception {
        var builder = new ClientConfigSpec.Builder().push("general");
        var enabled = builder.define("enabled", true);
        var number = builder.defineInRange("number", 1.0, .1, 10);
        var choice = builder.defineEnum("choice", Choice.FIRST);
        var spec = builder.build(directory.resolve("client.json"));
        enabled.set(false); number.set(2.25); choice.set(Choice.SECOND); spec.save();
        enabled.reset(); number.reset(); choice.reset(); spec.load();
        assertFalse(enabled.get()); assertEquals(2.25, number.get()); assertEquals(Choice.SECOND, choice.get());
        assertEquals(List.of("general.enabled", "general.number", "general.choice"), spec.values().stream().map(ClientConfigSpec.Value::key).toList());
        assertEquals("0.1", number.minimum()); assertEquals("10.0", number.maximum()); assertEquals(1.0, number.defaultValue());
    }
    @Test void invalidMissingAndMalformedValuesCannotRetainStaleSettings() throws Exception {
        var builder = new ClientConfigSpec.Builder().push("general");
        var enabled = builder.define("enabled", true);
        var number = builder.defineInRange("number", 1, 0, 10);
        var path = directory.resolve("client.json"); var spec = builder.build(path);
        enabled.set(false); number.set(8);
        Files.writeString(path, "{\"general\":{\"number\":900}}"); spec.load();
        assertTrue(enabled.get()); assertEquals(1, number.get());
        enabled.set(false); number.set(9); Files.writeString(path, "broken JSON"); spec.load();
        assertTrue(enabled.get()); assertEquals(1, number.get());
    }
    @Test void rejectsNonFiniteAndOutOfRangeEditsWithoutChangingCurrentValue() {
        var builder = new ClientConfigSpec.Builder().push("general");
        var number = builder.defineInRange("number", 1.0, .1, 10);
        for (String input : List.of("NaN", "Infinity", "-Infinity", "0", "11", "text")) {
            assertThrows(RuntimeException.class, () -> number.parse(input)); assertEquals(1.0, number.get());
        }
    }
}
