package com.facebook.openwifi.rrm.optimizers.tpc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.facebook.openwifi.cloudsdk.models.ap.State;
import com.google.gson.JsonObject;

public class RadioResolutionTest {
	@Test
	void sparsePhyAndLegacyReferences() {
		State state = new State();
		State.Radio first = new State.Radio();
		first.phy = "phy0";
		State.Radio second = new State.Radio();
		second.phy = "phy2";
		state.radios = new State.Radio[]{first, second};
		State.Interface.SSID ssid = new State.Interface.SSID();
		ssid.radio = new JsonObject();
		ssid.radio.addProperty("$ref", "#/radios/2");
		ssid.phy = "phy2";
		assertSame(second, MeasurementBasedApApTPC.resolveRadio(state, ssid));
		ssid.radio.addProperty("$ref", "#/radios/0");
		assertSame(second, MeasurementBasedApApTPC.resolveRadio(state, ssid));
		ssid.phy = "phy9";
		assertNull(MeasurementBasedApApTPC.resolveRadio(state, ssid));
		ssid.phy = null;
		assertSame(first, MeasurementBasedApApTPC.resolveRadio(state, ssid));
		for (String ref : new String[]{"#/radios/2", "#/radios/-1", "#/radios/bad"}) {
			ssid.radio.addProperty("$ref", ref);
			assertNull(MeasurementBasedApApTPC.resolveRadio(state, ssid));
		}
		ssid.radio = null;
		assertNull(MeasurementBasedApApTPC.resolveRadio(state, ssid));
		assertNull(MeasurementBasedApApTPC.resolveRadio(null, ssid));
	}
}
