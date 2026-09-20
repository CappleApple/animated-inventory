package com.cappleapple.animatedinventory;

import com.cappleapple.animatedinventory.client.ClientConfig;
import com.cappleapple.animatedinventory.client.ClientConfigScreen;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ConfigLocalizationTest {
    private static JsonObject catalog() {
        var resource=ConfigLocalizationTest.class.getResourceAsStream("/assets/animatedinventory/lang/en_us.json");
        assertNotNull(resource,"English language resource is packaged");
        return JsonParser.parseReader(new InputStreamReader(resource,StandardCharsets.UTF_8)).getAsJsonObject();
    }
    private static void present(JsonObject catalog,String key) {
        assertTrue(catalog.has(key),"Missing translation: "+key);
        assertFalse(catalog.get(key).getAsString().isBlank(),"Empty translation: "+key);
        assertNotEquals(key,catalog.get(key).getAsString());
    }
    @Test void everyRegisteredSettingHasLabelTooltipAndSection() throws Exception {
        var catalog=catalog();
        for(var field:ClientConfig.class.getFields()) if(field.get(null) instanceof ForgeConfigSpec.ConfigValue<?> value) {
            var path=value.getPath(); String key="animatedinventory.configuration."+path.get(path.size()-1);
            present(catalog,key); present(catalog,key+".tooltip"); present(catalog,"animatedinventory.configuration."+path.get(0));
        }
    }
    @Test void everySelectableEnumValueHasATranslatedButtonLabel() throws Exception {
        var catalog=catalog();
        for(var field:ClientConfig.class.getFields()) if(field.get(null) instanceof ForgeConfigSpec.ConfigValue<?> value) {
            ForgeConfigSpec.ValueSpec spec=ClientConfig.SPEC.getSpec().get(value.getPath());
            if(spec.getDefault() instanceof Enum<?> option)
                for(var choice:option.getDeclaringClass().getEnumConstants()) present(catalog,ClientConfigScreen.enumKey((Enum<?>)choice));
        }
        for(String key:new String[]{"previous","next","page","invalid_number","unavailable"}) present(catalog,"animatedinventory.configuration."+key);
    }
}
