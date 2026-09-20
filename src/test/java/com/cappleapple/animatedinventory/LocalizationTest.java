package com.cappleapple.animatedinventory;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class LocalizationTest {
    @Test void everyDeclaredSettingHasEnglishLabelAndTooltip() throws Exception {
        var catalog = catalog();
        String source = Files.readString(Path.of("src/main/java/com/cappleapple/animatedinventory/client/ClientConfig.java"));
        var settings = Pattern.compile("(?:bool|number|integer|duration|easing|defineEnum)\\(\"([^\"]+)\"").matcher(source);
        int count = 0;
        while (settings.find()) {
            String key = "animatedinventory.configuration." + settings.group(1);
            assertTrue(catalog.has(key), "Missing label " + key); assertTrue(catalog.has(key + ".tooltip"), "Missing tooltip " + key); count++;
        }
        assertEquals(57, count);
        var sections = Pattern.compile("push\\(\"([^\"]+)\"").matcher(source);
        while (sections.find()) assertTrue(catalog.has("animatedinventory.configuration." + sections.group(1)));
    }
    @Test void translationsContainNoUnresolvedKeysOrRawEnumValues() throws Exception {
        var catalog = catalog();
        for (var entry : catalog.entrySet()) {
            assertFalse(entry.getValue().getAsString().isBlank(), entry.getKey());
            assertFalse(entry.getValue().getAsString().contains("animatedinventory.configuration."), entry.getKey());
        }
        for (var path : java.util.List.of("api/animation/Easing.java", "api/animation/MovementStyle.java", "client/ClientConfig.java")) {
            String source = Files.readString(Path.of("src/main/java/com/cappleapple/animatedinventory/" + path));
            var enums = Pattern.compile("enum \\w+\\s*\\{([^;}]*)", Pattern.DOTALL).matcher(source);
            while (enums.find()) {
                var names = Pattern.compile("\\b[A-Z][A-Z_]+\\b(?=\\s*[,({])").matcher(enums.group(1) + ",");
                while (names.find()) assertTrue(catalog.has("animatedinventory.configuration.value." + names.group().toLowerCase(java.util.Locale.ROOT)), names.group());
            }
        }
    }
    private static JsonObject catalog() throws Exception {
        try (var input = LocalizationTest.class.getResourceAsStream("/assets/animatedinventory/lang/en_us.json")) {
            assertNotNull(input); return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
