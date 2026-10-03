package com.facebook.openwifi.rrm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import com.facebook.openwifi.cloudsdk.models.ap.Capabilities;
import com.facebook.openwifi.cloudsdk.models.ap.State;
import com.facebook.openwifi.cloudsdk.models.ap.UCentralSchema;

public class LiveChannelEligibilityTest {
	@Test
	void liveAuthorityAndFailureControls() {
		UCentralSchema.Radio config = new UCentralSchema.Radio();
		config.band = "5G"; config.country = "GB"; config.channelWidth = 40;
		Capabilities.Phy phy = new Capabilities.Phy(); phy.band = new String[] {"5G"};
		phy.channels = new int[] {36,40,149,153,157,161};
		Map<String, Capabilities.Phy> caps = Collections.singletonMap("phy7", phy);
		State state = new State(); State.Radio radio = new State.Radio(); radio.phy = "phy7";
		state.radios = new State.Radio[] {radio};
		assertTrue(LiveChannelEligibility.channels(config,state,caps,1000).isEmpty());
		radio.regulatory = new State.Radio.Regulatory();
		radio.regulatory.country = "GB"; radio.regulatory.timestamp = 1000;
		radio.regulatory.channels = new int[] {36,40,44};
		assertEquals(Arrays.asList(36),LiveChannelEligibility.channels(config,state,caps,1000));
		assertFalse(LiveChannelEligibility.channels(config,state,caps,1000).contains(149));
		assertTrue(LiveChannelEligibility.channels(config,state,caps,1301).isEmpty());
		assertTrue(LiveChannelEligibility.channels(config,state,caps,960).isEmpty());
		radio.regulatory.country = "US";
		assertTrue(LiveChannelEligibility.channels(config,state,caps,1000).isEmpty());
		radio.regulatory.country = "GB"; radio.phy = "wrong";
		assertTrue(LiveChannelEligibility.channels(config,state,caps,1000).isEmpty());
		radio.phy = "phy7"; config.enable = false;
		assertTrue(LiveChannelEligibility.channels(config,state,caps,1000).isEmpty());
		config.enable = true; config.validChannels = new int[] {36};
		assertEquals(Arrays.asList(36),LiveChannelEligibility.channels(config,state,caps,1000));
		config.validChannels = null; config.channelWidth = 80;
		assertTrue(LiveChannelEligibility.channels(config,state,caps,1000).isEmpty());
		config.channelWidth = 20; config.allowDfs = false;
		radio.regulatory.dfs_channels = new int[] {36};
		assertEquals(Arrays.asList(40,44),LiveChannelEligibility.channels(config,state,caps,1000));
		config.allowDfs = true; phy.band = new String[] {"5G","6G"};
		assertTrue(LiveChannelEligibility.channels(config,state,caps,1000).isEmpty());
	}
	@Test
	void sameBandPhysAreIntersectedNotIndexed() {
		UCentralSchema.Radio config = new UCentralSchema.Radio();
		config.band="5G"; config.country="GB"; config.channelWidth=20;
		Capabilities.Phy phy = new Capabilities.Phy(); phy.band=new String[] {"5G"};
		State state=new State(); State.Radio a=new State.Radio(), b=new State.Radio();
		a.phy="phy8"; b.phy="phy2";
		a.regulatory=new State.Radio.Regulatory(); b.regulatory=new State.Radio.Regulatory();
		a.regulatory.country=b.regulatory.country="GB";
		a.regulatory.timestamp=b.regulatory.timestamp=1000;
		a.regulatory.channels=new int[] {36,44}; b.regulatory.channels=new int[] {44,149};
		state.radios=new State.Radio[] {b,a};
		assertEquals(Arrays.asList(44),LiveChannelEligibility.channels(config,state,Map.of("phy8",phy,"phy2",phy),1000));
	}
}
