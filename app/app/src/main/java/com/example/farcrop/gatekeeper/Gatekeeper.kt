package com.example.farcrop.gatekeeper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Orchestrates all on-device image checks before an image is uploaded.
 *
 * The preferred entry point is [analyzeBytes], which accepts image bytes that have
 * already been read from the URI. This avoids opening the URI stream a second time,
 * which causes zero-byte reads on gallery and cloud-storage URIs.
 *
 * [analyze] (URI-based) is kept for any future caller that only has a URI and has
 * not yet read the bytes.
 */
class Gatekeeper(private val context: Context) {

    private val blurDetector = BlurDetector()

    /**
     * Run all checks on [imageBytes] that were already read from the source URI.
     * This is the preferred entry point — the URI stream is never opened here.
     */
    fun analyzeBytes(imageBytes: ByteArray): GatekeeperResult {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return GatekeeperResult.fail("Could not decode the image. Please try again.")
        return runChecks(bitmap)
    }

    /**
     * Decode [uri] to a Bitmap and run all checks.
     * Use [analyzeBytes] instead whenever the bytes have already been read.
     */
    fun analyze(uri: Uri): GatekeeperResult {
        val bitmap = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        } ?: return GatekeeperResult.fail("Could not read the image. Please try again.")

        return runChecks(bitmap)
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun runChecks(bitmap: Bitmap): GatekeeperResult {
        // ── 1. Blur detection ─────────────────────────────────────────────────
        val blurResult = blurDetector.detect(bitmap)
        if (!blurResult.passed) return blurResult

        // ── 2. [Future] Additional checks — add here, nothing else changes ────
        // val qualityResult = imageQuality.check(bitmap)
        // if (!qualityResult.passed) return qualityResult

        return GatekeeperResult.pass(confidence = blurResult.confidence)
    }
}
