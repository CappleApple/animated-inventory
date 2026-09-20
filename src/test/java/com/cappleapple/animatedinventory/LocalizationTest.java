package com.cappleapple.animatedinventory;

import com.cappleapple.animatedinventory.client.ClientConfig;
import com.google.gson.*;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TranslatableEnum;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalizationTest {
    private static JsonObject catalog() throws Exception {
        try (var stream = LocalizationTest.class.getResourceAsStream("/assets/animatedinventory/lang/en_us.json")) {
            assertNotNull(stream);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    @Test void everyRegisteredSettingAndSectionHasLabelsAndTooltips() throws Exception {
        var translations = catalog(); int settings = 0; Set<String> sections = new HashSet<>();
        for (var field : ClientConfig.class.getFields()) if (field.get(null) instanceof ModConfigSpec.ConfigValue<?> value) {
            settings++; var path = value.getPath(); sections.add(path.getFirst());
            label(translations, path.getLast()); label(translations, path.getLast() + ".tooltip");
        }
        assertEquals(57, settings);
        for (String section : sections) { label(translations, section); label(translations, section + ".tooltip"); label(translations, section + ".button"); }
        label(translations, "title"); label(translations, "section.animatedinventory.client.toml"); label(translations, "section.animatedinventory.client.toml.title");
    }
    @Test void nativeEnumControlsUseLocalizedComponentsForEveryAllowedChoice() throws Exception {
        var translations = catalog(); Set<Class<?>> classes = new HashSet<>();
        for (var field : ClientConfig.class.getFields()) if (field.get(null) instanceof ModConfigSpec.ConfigValue<?> value
            && value.getDefault() instanceof Enum<?> current && classes.add(current.getDeclaringClass())) {
            for (Object choice : current.getDeclaringClass().getEnumConstants()) {
                var translated = assertInstanceOf(TranslatableEnum.class, choice);
                var contents = assertInstanceOf(TranslatableContents.class, translated.getTranslatedName().getContents());
                String key = "animatedinventory.configuration.value." + ((Enum<?>)choice).name().toLowerCase(Locale.ROOT);
                assertEquals(key, contents.getKey());
                assertTrue(translations.has(key), key);
                assertFalse(translations.get(key).getAsString().contains("_"), key);
            }
        }
        assertEquals(6, classes.size());
    }
    private static void label(JsonObject translations, String suffix) {
        String key = "animatedinventory.configuration." + suffix;
        assertTrue(translations.has(key), key);
        assertFalse(translations.get(key).getAsString().isBlank(), key);
    }
}