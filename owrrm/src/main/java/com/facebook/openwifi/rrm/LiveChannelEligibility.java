package com.facebook.openwifi.rrm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.facebook.openwifi.cloudsdk.models.ap.Capabilities;
import com.facebook.openwifi.cloudsdk.models.ap.State;
import com.facebook.openwifi.cloudsdk.models.ap.UCentralSchema;

/** Live regulatory authority, separate from cached hardware capabilities. */
public final class LiveChannelEligibility {
	private LiveChannelEligibility() {}
	public static List<Integer> channels(UCentralSchema.Radio config, State state,
		Map<String, Capabilities.Phy> capabilities, long now) {
		if (config == null || Boolean.FALSE.equals(config.enable) || config.country == null ||
			state == null || state.radios == null || capabilities == null) return Collections.emptyList();
		List<Integer> result = null;
		for (State.Radio radio : state.radios) {
			if (radio == null || radio.phy == null) continue;
			Capabilities.Phy phy = capabilities.get(radio.phy);
			if (phy == null || phy.band == null || !Arrays.asList(phy.band).contains(config.band)) continue;
			// A switchable PHY cannot be identified by the first advertised band.
			if (phy.band.length != 1) return Collections.emptyList();
			State.Radio.Regulatory live = radio.regulatory;
			if (live == null || live.channels == null || live.country == null ||
				!config.country.equalsIgnoreCase(live.country) || live.timestamp <= 0 ||
				live.timestamp > now + 30 || now - live.timestamp > 300) return Collections.emptyList();
			List<Integer> eligible = new ArrayList<>();
			for (int channel : live.channels) {
				if (channel <= 0) continue;
				if (Boolean.FALSE.equals(config.allowDfs) && live.dfs_channels != null &&
					Arrays.stream(live.dfs_channels).anyMatch(dfs -> dfs == channel)) continue;
				eligible.add(channel);
			}
			if (result == null) result = eligible;
			else result.retainAll(eligible); // Never authorize one same-band PHY using another PHY's list.
		}
		if (result == null) return Collections.emptyList();
		List<Integer> primary = new ArrayList<>();
		for (int channel : result) {
			int width = config.channelWidth;
			if (width == 20) primary.add(channel);
			else if ("2G".equals(config.band) && width == 40 &&
				result.contains(channel <= 7 ? channel + 4 : channel - 4)) primary.add(channel);
			else if ("5G".equals(config.band) && (width == 40 || width == 80 || width == 160)) {
				int[] bases = width == 40 ? new int[] {36,44,52,60,100,108,116,124,132,140,149,157} :
					width == 80 ? new int[] {36,52,100,116,132,149} : new int[] {36,100};
				if (Arrays.stream(bases).noneMatch(base -> base == channel)) continue;
				boolean complete = true;
				for (int offset = 0; offset < width / 20; offset++) complete &= result.contains(channel + offset * 4);
				if (complete) primary.add(channel);
			}
		}
		if (config.validChannels != null) {
			List<Integer> requested = new ArrayList<>();
			for (int channel : config.validChannels) requested.add(channel);
			primary.retainAll(requested);
		}
		return primary;
	}
}
