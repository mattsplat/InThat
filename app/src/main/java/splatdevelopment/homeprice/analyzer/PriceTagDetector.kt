package splatdevelopment.homeprice.analyzer

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.media.Image
import android.os.SystemClock
import androidx.compose.ui.geometry.Rect
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.interfaces.Detector
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.concurrent.Executors

/**
 * Trial on-device price tag detector: finds whole tags in camera frames with a YOLOv8n model.
 *
 * Model: "price_tag" by nimes on Roboflow Universe (dataset CC BY 4.0),
 * https://universe.roboflow.com/nimes/price_tag-kvox2
 *
 * Implements ML Kit's [Detector] so CameraX's MlKitAnalyzer can run it next to text
 * recognition and hand it the same matrix for mapping boxes into preview coordinates.
 */
class PriceTagDetector(private val context: Context) : Detector<PriceTagDetector.Result> {

    /** Tag boxes in preview pixels, and how long converting the frame and running the model took. */
    class Result(val tags: List<Rect>, val conversionMs: Long, val inferenceMs: Long)

    private val environment = OrtEnvironment.getEnvironment().apply {
        // Telemetry's startup provider is removed in the manifest; this also turns off the native side
        runCatching { setTelemetry(false) }
    }
    private val executor = Executors.newSingleThreadExecutor()
    private val input = FloatBuffer.allocate(3 * INPUT_SIZE * INPUT_SIZE)

    // Loaded on the detector thread the first time it runs, so opening the scanner doesn't stall the UI
    private val session: OrtSession by lazy {
        environment.createSession(context.assets.open(MODEL_ASSET).use { it.readBytes() })
    }

    override fun getDetectorType(): Int = Detector.TYPE_OBJECT_DETECTION

    override fun process(image: Image, rotationDegrees: Int, transform: Matrix): Task<Result> =
        Tasks.call(executor) {
            val start = SystemClock.elapsedRealtime()
            val (uprightWidth, uprightHeight) = fillInput(image, rotationDegrees)
            val converted = SystemClock.elapsedRealtime()

            val output = OnnxTensor.createTensor(environment, input, longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())).use { tensor ->
                session.run(mapOf(session.inputNames.first() to tensor)).use { results ->
                    val buffer = (results[0] as OnnxTensor).floatBuffer
                    FloatArray(buffer.remaining()).also { buffer.get(it) }
                }
            }

            val tags = decodeDetections(output).map { box ->
                // Model space -> upright frame pixels -> preview pixels
                val scaleX = uprightWidth.toFloat() / INPUT_SIZE
                val scaleY = uprightHeight.toFloat() / INPUT_SIZE
                val mapped = RectF(box.left * scaleX, box.top * scaleY, box.right * scaleX, box.bottom * scaleY)
                transform.mapRect(mapped)
                Rect(mapped.left, mapped.top, mapped.right, mapped.bottom)
            }
            Result(tags, conversionMs = converted - start, inferenceMs = SystemClock.elapsedRealtime() - converted)
        }

    /**
     * Converts the YUV camera frame into the model's input: upright, stretched to 640x640,
     * RGB, channels-first, scaled to 0..1. Returns the upright frame's width and height.
     */
    private fun fillInput(image: Image, rotationDegrees: Int): Pair<Int, Int> {
        val width = image.width
        val height = image.height
        val isSideways = rotationDegrees % 180 != 0
        val uprightWidth = if (isSideways) height else width
        val uprightHeight = if (isSideways) width else height

        val (yPlane, uPlane, vPlane) = image.planes
        val yBuffer: ByteBuffer = yPlane.buffer
        val uBuffer: ByteBuffer = uPlane.buffer
        val vBuffer: ByteBuffer = vPlane.buffer
        val area = INPUT_SIZE * INPUT_SIZE

        for (outY in 0 until INPUT_SIZE) {
            val uprightY = outY * uprightHeight / INPUT_SIZE
            for (outX in 0 until INPUT_SIZE) {
                val uprightX = outX * uprightWidth / INPUT_SIZE

                // Find the source pixel that lands at this upright position after rotation
                // (two plain whens rather than a Pair, to avoid ~400k allocations per frame)
                val sourceX = when (rotationDegrees) {
                    90 -> uprightY
                    180 -> width - 1 - uprightX
                    270 -> width - 1 - uprightY
                    else -> uprightX
                }
                val sourceY = when (rotationDegrees) {
                    90 -> height - 1 - uprightX
                    180 -> height - 1 - uprightY
                    270 -> uprightX
                    else -> uprightY
                }

                val y = yBuffer.get(sourceY * yPlane.rowStride + sourceX).toInt() and 0xFF
                val u = (uBuffer.get((sourceY / 2) * uPlane.rowStride + (sourceX / 2) * uPlane.pixelStride).toInt() and 0xFF) - 128
                val v = (vBuffer.get((sourceY / 2) * vPlane.rowStride + (sourceX / 2) * vPlane.pixelStride).toInt() and 0xFF) - 128

                val i = outY * INPUT_SIZE + outX
                input.put(i, ((y + 1.402f * v) / 255f).coerceIn(0f, 1f))
                input.put(area + i, ((y - 0.344f * u - 0.714f * v) / 255f).coerceIn(0f, 1f))
                input.put(2 * area + i, ((y + 1.772f * u) / 255f).coerceIn(0f, 1f))
            }
        }
        input.rewind()
        return uprightWidth to uprightHeight
    }

    override fun process(bitmap: Bitmap, rotationDegrees: Int): Task<Result> =
        throw UnsupportedOperationException("Only camera frames are supported")

    override fun process(image: Image, rotationDegrees: Int): Task<Result> =
        throw UnsupportedOperationException("A coordinate transform is required")

    override fun process(buffer: ByteBuffer, width: Int, height: Int, rotationDegrees: Int, format: Int): Task<Result> =
        throw UnsupportedOperationException("Only camera frames are supported")

    override fun close() {
        executor.execute { session.close() }
        executor.shutdown()
    }

    companion object {
        private const val MODEL_ASSET = "price_tag_detector.onnx"
        private const val INPUT_SIZE = 640

        /**
         * Decodes YOLOv8 output laid out as [cx…, cy…, w…, h…, score…] (one row per value,
         * one column per candidate) into boxes in model input pixels, dropping low scores
         * and overlapping duplicates.
         */
        internal fun decodeDetections(output: FloatArray, minScore: Float = 0.4f, maxOverlap: Float = 0.5f): List<Rect> {
            val candidates = output.size / 5
            val scored = (0 until candidates)
                .filter { output[4 * candidates + it] >= minScore }
                .sortedByDescending { output[4 * candidates + it] }
                .map { i ->
                    val cx = output[i]
                    val cy = output[candidates + i]
                    val w = output[2 * candidates + i]
                    val h = output[3 * candidates + i]
                    Rect(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
                }

            val kept = mutableListOf<Rect>()
            for (box in scored) {
                if (kept.none { overlap(it, box) > maxOverlap }) kept += box
            }
            return kept
        }

        private fun overlap(a: Rect, b: Rect): Float {
            val intersection = a.intersect(b)
            if (intersection.isEmpty) return 0f
            val shared = intersection.width * intersection.height
            return shared / (a.width * a.height + b.width * b.height - shared)
        }
    }
}
