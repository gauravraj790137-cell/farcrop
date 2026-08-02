package com.example.farcrop.gatekeeper

/**
 * The result returned by [Gatekeeper.analyze].
 *
 * @param passed     true  → image is acceptable, proceed with upload
 *                   false → image was rejected, show [reason] to the user
 * @param reason     Human-readable explanation. Empty string when [passed] is true.
 * @param confidence Optional score (0.0–1.0) from the check that made the decision.
 *                   Null when no confidence value is available (e.g. placeholder checks).
 */
data class GatekeeperResult(
    val passed: Boolean,
    val reason: String = "",
    val confidence: Float? = null
) {
    companion object {
        /** Convenience constructor for a passing result. */
        fun pass(confidence: Float? = null) = GatekeeperResult(
            passed = true,
            reason = "",
            confidence = confidence
        )

        /** Convenience constructor for a failing result. */
        fun fail(reason: String, confidence: Float? = null) = GatekeeperResult(
            passed = false,
            reason = reason,
            confidence = confidence
        )
    }
}
