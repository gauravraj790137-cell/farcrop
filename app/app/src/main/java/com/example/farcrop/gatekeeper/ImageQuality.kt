package com.example.farcrop.gatekeeper

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

/**
 * Checks general image quality (resolution, exposure) before upload.
 *
 * Current implementation: PLACEHOLDER — always passes.
 *
 * Planned real checks (add each block below without touching anything else):
 *  - Minimum resolution  → reject if bitmap is smaller than 224×224 px
 *  - Brightness / exposure → reject near-black or near-white frames
 *  - Aspect-ratio sanity → reject extreme panoramic or portrait crops
 */
class ImageQuality(private val context: Context) {

    /**
     * Evaluate the quality of [bitmap] decoded from [uri].
     *
     * @return [GatekeeperResult.pass] until real checks are implemented.
     */
    fun check(uri: Uri, bitmap: Bitmap): GatekeeperResult {

        // TODO: minimum resolution check
        // if (bitmap.width < MIN_WIDTH || bitmap.height < MIN_HEIGHT) {
        //     return GatekeeperResult.fail(
        //         "Image is too small. Please take a closer photo of the leaf."
        //     )
        // }

        // TODO: brightness / exposure check
        // val brightness = computeAverageBrightness(bitmap)
        // if (brightness < DARK_THRESHOLD) {
        //     return GatekeeperResult.fail(
        //         "Image is too dark. Move to better lighting and retake."
        //     )
        // }
        // if (brightness > BRIGHT_THRESHOLD) {
        //     return GatekeeperResult.fail(
        //         "Image is overexposed. Avoid direct sunlight on the lens."
        //     )
        // }

        return GatekeeperResult.pass()
    }

    companion object {
        private const val MIN_WIDTH        = 224
        private const val MIN_HEIGHT       = 224
        private const val DARK_THRESHOLD   = 30f    // 0–255 average pixel brightness
        private const val BRIGHT_THRESHOLD = 235f
    }
}
