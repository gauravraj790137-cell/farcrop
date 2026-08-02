package com.example.farcrop.gatekeeper

import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * Detects whether an image is too blurry to be sent to the server.
 *
 * Algorithm: Variance of Laplacian
 *  1. Downscale the bitmap to [MAX_SIDE] px on the longest side (speed).
 *  2. Convert to greyscale.
 *  3. Apply a 3×3 Laplacian kernel (second-order edge detector).
 *  4. Compute the variance of the resulting pixel values.
 *     - Sharp image → lots of edges → high variance
 *     - Blurry image → smooth gradients → low variance
 *  5. Reject if variance < [BLUR_THRESHOLD].
 *
 * Threshold tuning guide:
 *  - Capture 10 sharp crop-leaf photos  → note their variance values
 *  - Capture 10 blurry ones             → note their variance values
 *  - Set threshold in the gap between the two groups
 *  - Starting point: ~120.0 for 320px downscaled images
 */
class BlurDetector {

    /**
     * Evaluate the sharpness of [bitmap].
     *
     * @return [GatekeeperResult.pass]  if the image is sharp enough
     *         [GatekeeperResult.fail]  with a user-facing message if it is blurry
     */
    fun detect(bitmap: Bitmap): GatekeeperResult {
        if (!ensureOpenCvLoaded()) {
            // If OpenCV fails to load, let the image through rather than blocking the user
            return GatekeeperResult.pass()
        }

        val variance = laplacianVariance(bitmap)

        return if (variance < BLUR_THRESHOLD) {
            GatekeeperResult.fail(
                reason = "The image appears blurry. Please capture a clearer photo of a single crop leaf.",
                // confidence = how close to the threshold (0 = very blurry, 1 = just at threshold)
                confidence = (variance / BLUR_THRESHOLD).toFloat().coerceIn(0f, 1f)
            )
        } else {
            GatekeeperResult.pass(
                confidence = (variance / BLUR_THRESHOLD).toFloat().coerceIn(0f, 1f)
            )
        }
    }

    // -------------------------------------------------------------------------
    // Core algorithm
    // -------------------------------------------------------------------------

    /**
     * Compute the Variance of Laplacian for [bitmap].
     * Higher = sharper.
     */
    private fun laplacianVariance(bitmap: Bitmap): Double {
        // 1. Downscale for speed
        val scaled = scaleBitmap(bitmap)

        // 2. Convert Android Bitmap → OpenCV Mat (RGBA)
        val rgba = Mat()
        Utils.bitmapToMat(scaled, rgba)
        if (scaled !== bitmap) scaled.recycle()

        // 3. Convert to greyscale
        val grey = Mat()
        Imgproc.cvtColor(rgba, grey, Imgproc.COLOR_RGBA2GRAY)
        rgba.release()

        // 4. Apply Laplacian kernel (3×3, output in 64-bit float for precision)
        val laplacian = Mat()
        Imgproc.Laplacian(grey, laplacian, CvType.CV_64F)
        grey.release()

        // 5. Compute variance = mean of (x - mean)²
        //    OpenCV's meanStdDev gives stdDev directly; variance = stdDev²
        val mean    = MatOfDouble()
        val stdDev  = MatOfDouble()
        Core.meanStdDev(laplacian, mean, stdDev)
        laplacian.release()

        val std      = stdDev.get(0, 0)[0]
        val variance = std * std

        mean.release()
        stdDev.release()

        return variance
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Downscale [bitmap] so its longest side is at most [MAX_SIDE] pixels.
     * Returns the original bitmap unchanged if it is already small enough.
     */
    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val maxDim = maxOf(bitmap.width, bitmap.height)
        if (maxDim <= MAX_SIDE) return bitmap
        val scale = MAX_SIDE.toFloat() / maxDim.toFloat()
        val w = (bitmap.width  * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    /**
     * Initialise OpenCV on first use.
     * [OpenCVLoader.initLocal] loads the bundled native libs from the AAR —
     * no OpenCV Manager app required on the device.
     */
    private fun ensureOpenCvLoaded(): Boolean {
        if (openCvReady) return true
        openCvReady = OpenCVLoader.initLocal()
        return openCvReady
    }

    // -------------------------------------------------------------------------
    // Mat alias so callers don't need to import org.opencv.core.MatOfDouble
    // -------------------------------------------------------------------------
    private inner class MatOfDouble : org.opencv.core.MatOfDouble()

    companion object {
        /**
         * Images with a Laplacian variance below this value are considered blurry.
         *
         * Tune this with your actual test images:
         *  - Too high → sharp images are wrongly rejected
         *  - Too low  → blurry images slip through
         *
         * Good starting point for 320px downscaled crop images: ~120.0
         */
        const val BLUR_THRESHOLD = 120.0

        /** Longest side (px) to downscale to before running the Laplacian. */
        private const val MAX_SIDE = 320

        /** Cached OpenCV load state — avoid calling initLocal() on every frame. */
        @Volatile
        private var openCvReady = false
    }
}
