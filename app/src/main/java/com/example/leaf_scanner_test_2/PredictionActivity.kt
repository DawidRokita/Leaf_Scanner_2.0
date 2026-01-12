package com.example.leaf_scanner_test_2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PredictionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }

    private lateinit var imageLeaf: ImageView
    private lateinit var tvPrediction: TextView
    private lateinit var btnSave: Button

    private var finalPath: String? = null

    // do zapisu w bazie
    private var predictionText: String? = null
    private var leafName: String = "Leaf"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prediction)

        imageLeaf = findViewById(R.id.imageLeaf)
        tvPrediction = findViewById(R.id.tvPrediction)
        btnSave = findViewById(R.id.btnSave)

        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (uriString.isNullOrBlank()) {
            tvPrediction.text = "Photo doesn't exist"
            btnSave.isEnabled = false
            return
        }

        val uri = Uri.parse(uriString)
        finalPath = if (uri.scheme == "file") uri.path else copyToCache(uri)

        if (finalPath.isNullOrBlank()) {
            tvPrediction.text = "Cannot read file path"
            btnSave.isEnabled = false
            return
        }

        val bitmap = ensureArgb8888(loadBitmapWithRotation(File(finalPath!!)))
        imageLeaf.setImageBitmap(bitmap)

        tvPrediction.text = "Analizing..."
        btnSave.isEnabled = false

        lifecycleScope.launch {
            val pred = withContext(Dispatchers.Default) {
                LeafClassifier.get(this@PredictionActivity).predictTop1(bitmap)
            }

            if (pred == null) {
                tvPrediction.text = "Prediction error"
                btnSave.isEnabled = false
                return@launch
            }

            leafName = pred.plant

            val formatted = formatPrediction(pred)
            predictionText = formatted
            tvPrediction.text = formatted

            tvPrediction.setTextColor(
                if (pred.isHealthy) 0xFF2E7D32.toInt() else 0xFFC62828.toInt()
            )

            btnSave.isEnabled = true
        }

        btnSave.setOnClickListener { saveResult() }
    }

    private fun formatPrediction(p: LeafClassifier.Prediction): String {
        val status = if (p.isHealthy) "Healthy" else "Diseased"
        val disease = p.disease ?: "—"
        val conf = "%.2f%%".format(p.confidence * 100f)

        return """
            Plant: ${p.plant}
            Status: $status
            Disease: $disease
            Confidence: $conf
        """.trimIndent()
    }

    private fun saveResult() {
        val predText = predictionText ?: run {
            tvPrediction.text = "Nothing to write."
            return
        }
        val path = finalPath ?: run {
            tvPrediction.text = "missing file path."
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.get(this@PredictionActivity).scanResultDao()
            dao.insert(
                ScanResult(
                    imageUri = path,
                    leafName = leafName,
                    prediction = predText,
                    timestamp = System.currentTimeMillis()
                )
            )
            finish()
        }
    }

    private fun ensureArgb8888(bmp: Bitmap): Bitmap =
        if (bmp.config == Bitmap.Config.ARGB_8888) bmp else bmp.copy(Bitmap.Config.ARGB_8888, false)

    private fun copyToCache(uri: Uri): String {
        val file = File(externalCacheDir, "leaf_${System.currentTimeMillis()}.jpg")
        contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file.absolutePath
    }

    private fun loadBitmapWithRotation(file: File): Bitmap {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalArgumentException("Cannot decode bitmap")

        val exif = try { ExifInterface(file.absolutePath) } catch (e: Exception) { return bitmap }

        val rotation = when (
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        if (rotation == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
