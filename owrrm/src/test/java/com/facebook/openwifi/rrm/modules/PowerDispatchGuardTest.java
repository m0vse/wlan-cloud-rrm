package com.facebook.openwifi.rrm.modules;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import com.facebook.openwifi.cloudsdk.UCentralApConfiguration;
import com.facebook.openwifi.rrm.DeviceConfig;
import com.facebook.openwifi.rrm.DeviceDataManager;
import com.google.gson.JsonParser;

/** Exercise the actual config listener, with no client or network transport. */
public class PowerDispatchGuardTest {
    private UCentralApConfiguration apply(Integer target, boolean manual) throws Exception {
        DeviceConfig settings = new DeviceConfig();
        settings.enableRRM = true;
        settings.autoTxPowers = new HashMap<>();
        settings.autoTxPowers.put("5G", target);
        settings.autoTxPowers.put("2G", 7);
        if (manual) {
            settings.userTxPowers = new HashMap<>();
            settings.userTxPowers.put("5G", target);
        }
        DeviceDataManager devices = new DeviceDataManager() {
            @Override public DeviceConfig getDeviceConfig(String serial) { return settings; }
        };
        ConfigManager manager = new ConfigManager(null, devices, null);
        UCentralApConfiguration config = new UCentralApConfiguration(
            "{\"radios\":[{\"band\":\"5G\",\"tx-power\":17,\"channel\":\"auto\"},"
            + "{\"band\":\"6G\",\"tx-power\":14},{\"band\":\"2G\",\"tx-power\":17}]}");
        Method method = ConfigManager.class.getDeclaredMethod("applyRRMConfig", String.class, UCentralApConfiguration.class);
        method.setAccessible(true);
        assertTrue((boolean) method.invoke(manager, "fixture", config));
        assertEquals(7, power(config, 2));
        assertEquals(14, power(config, 1));
        assertEquals("auto", JsonParser.parseString(config.toString()).getAsJsonObject()
            .getAsJsonArray("radios").get(0).getAsJsonObject().get("channel").getAsString());
        assertEquals(target, settings.autoTxPowers.get("5G"));
        return config;
    }
    private int power(UCentralApConfiguration config, int index) {
        return JsonParser.parseString(config.toString()).getAsJsonObject().getAsJsonArray("radios")
            .get(index).getAsJsonObject().get("tx-power").getAsInt();
    }
    @Test void invalidAutomaticAndPersistedTargetsHoldCurrentRequest() throws Exception {
        for (Integer target : new Integer[]{null, -1, 0, 31, Integer.MAX_VALUE})
            assertEquals(17, power(apply(target, false), 0));
    }
    @Test void invalidManualTargetCannotBypassGuard() throws Exception {
        assertEquals(17, power(apply(0, true), 0));
    }
    @Test void ordinaryPowerControlAndManualPositiveTargetsRemain() throws Exception {
        for (int target : new int[]{1, 7, 17, 30}) assertEquals(target, power(apply(target, false), 0));
        assertEquals(13, power(apply(13, true), 0));
    }
}
