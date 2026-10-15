# Power-control dispatch

Nearby-AP measurements can legitimately reduce RRM transmit-power targets.
That reduction is not itself a radio fault.

Before sending configuration, ConfigManager now rejects null, non-positive,
and above-30 dBm RRM targets. This applies to automatic, persisted and manual
RRM overrides. The existing requested power is retained, with a warning in
the RRM log. It does not substitute an unverified hardware minimum, disable
TPC, change channels, or override a valid positive power target.

The deployed ath11k per-VIF path skips zero-power requests. Previously RRM
could therefore replace an operator's 17 dBm request with 0 dBm while the
radio continued at 17 dBm. A later operator push could be overwritten again.

The guard prevents future unsupported overrides; it does not repair an
already-delivered configuration automatically. A normal operator config
push can restore that configuration. No AP flash or reboot is required.

Tests: PowerDispatchGuardTest invokes the actual config listener using
synthetic radio configuration and no network client. The full Maven reactor
must pass, including existing PHY resolution and live-regulatory guards.
