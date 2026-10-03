/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.openwifi.cloudsdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;
import com.facebook.openwifi.cloudsdk.models.ap.Capabilities;
import com.facebook.openwifi.cloudsdk.models.ap.UCentralSchema;

public class UCentralUtilsTest {
	@Test
	void testAvailableChannelsMatchesBandArray() {
		UCentralSchema.Radio radio = new UCentralSchema.Radio();
		radio.band = "5G";
		Capabilities.Phy phy = new Capabilities.Phy();
		phy.band = new String[] {"5G", "6G"};
		phy.channels = new int[] {36, 44, 52};
		phy.dfs_channels = new int[] {52};
		assertEquals(Arrays.asList(36, 44), UCentralUtils.getDeviceAvailableChannels(
			Collections.singletonMap("ap", Arrays.asList(radio)),
			Collections.singletonMap("ap", Collections.singletonMap("phy0", phy)),
			UCentralUtils.AVAILABLE_CHANNELS_BAND).get("5G").get("ap"));
		phy.band = new String[] {"2G"};
		assertTrue(UCentralUtils.getDeviceAvailableChannels(
			Collections.singletonMap("ap", Arrays.asList(radio)),
			Collections.singletonMap("ap", Collections.singletonMap("phy0", phy)),
			UCentralUtils.AVAILABLE_CHANNELS_BAND).get("5G").get("ap").isEmpty());
		phy.band = null;
		assertTrue(UCentralUtils.getDeviceAvailableChannels(
			Collections.singletonMap("ap", Arrays.asList(radio)),
			Collections.singletonMap("ap", Collections.singletonMap("phy0", phy)),
			UCentralUtils.AVAILABLE_CHANNELS_BAND).get("5G").get("ap").isEmpty());
	}
	@Test
	void test_placeholder() throws Exception {
		assertEquals(3, 1 + 2);
	}

	@Test
	void test_setRadioConfigFieldChannel() throws Exception {
		final String serialNumber = "aaaaaaaaaaaa";
		final int expectedChannel = 1;
		final Map<String, Integer> newValueList = Collections
			.singletonMap(UCentralConstants.BAND_5G, expectedChannel);

		// test case where channel value is a string and not an integer
		UCentralApConfiguration config = new UCentralApConfiguration(
			"{\"interfaces\": [], \"radios\": [{\"band\": \"5G\", \"channel\": \"auto\"}]}"
		);
		boolean modified = UCentralUtils
			.setRadioConfigField(serialNumber, config, "channel", newValueList);
		assertTrue(modified);
		assertEquals(
			config.getRadioConfig(0).get("channel").getAsInt(),
			expectedChannel
		);

		// field doesn't exist
		config = new UCentralApConfiguration(
			"{\"interfaces\": [], \"radios\": [{\"band\": \"5G\"}]}"
		);
		modified = UCentralUtils
			.setRadioConfigField(serialNumber, config, "channel", newValueList);
		assertTrue(modified);
		assertEquals(
			config.getRadioConfig(0).get("channel").getAsInt(),
			expectedChannel
		);

		// normal field, not modified
		config = new UCentralApConfiguration(
			"{\"interfaces\": [], \"radios\": [{\"band\": \"5G\", \"channel\": 1}]}"
		);
		modified = UCentralUtils
			.setRadioConfigField(serialNumber, config, "channel", newValueList);
		assertFalse(modified);
		assertEquals(
			config.getRadioConfig(0).get("channel").getAsInt(),
			expectedChannel
		);

		// normal field, modified
		config = new UCentralApConfiguration(
			"{\"interfaces\": [], \"radios\": [{\"band\": \"5G\", \"channel\": 15}]}"
		);
		modified = UCentralUtils
			.setRadioConfigField(serialNumber, config, "channel", newValueList);
		assertTrue(modified);
		assertEquals(
			config.getRadioConfig(0).get("channel").getAsInt(),
			expectedChannel
		);
	}
}
