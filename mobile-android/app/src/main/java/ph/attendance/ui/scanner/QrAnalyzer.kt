package ph.attendance.ui.scanner

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Decodes QR codes from the camera stream.
 *
 * Restricted to QR format alone: the app has no use for a product barcode, and narrowing the
 * formats makes the detector both faster and less likely to fire on something irrelevant in the
 * background.
 *
 * The decoded string is handed straight to the repository, which validates it before it reaches a
 * query — the analyser deliberately makes no judgement about what a valid student number looks
 * like, so there is only one place that rule lives.
 */
class QrAnalyzer(private val onBarcode: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build(),
    )

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { it.rawValue }?.let(onBarcode)
            }
            // The frame must be closed on every path, or the pipeline stalls after a few images.
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() = scanner.close()
}
