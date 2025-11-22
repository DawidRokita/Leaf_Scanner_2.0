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

    private var prediction: String? = null
    private var finalPath: String? = null
    private var leafName: String = "Leaf"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prediction)

        imageLeaf = findViewById(R.id.imageLeaf)
        tvPrediction = findViewById(R.id.tvPrediction)
        btnSave = findViewById(R.id.btnSave)

        val inputUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val inputUri = Uri.parse(inputUriString)

        // ===== DETECT WHETHER FROM CAMERA OR GALLERY =====
        val fromCamera = inputUri.scheme == "file"

        // ===== ALWAYS COPY TO CACHE IF FROM GALLERY =====
        finalPath = if (fromCamera) {
            // kamera → ścieżka już jest poprawna
            inputUri.path
        } else {
            // galeria → kopiujemy
            copyToCache(inputUri)
        }

        val bitmap = loadBitmapWithRotation(File(finalPath!!))
        imageLeaf.setImageBitmap(bitmap)

        // ===== CLASSIFY =====
        lifecycleScope.launch {
            val result = withContext(Dispatchers.Default) {
                LeafClassifier.get(this@PredictionActivity).classify(bitmap)
            }
            prediction = result
            tvPrediction.text = "Prediction result: $result"
        }

        btnSave.setOnClickListener { saveResult() }
    }


    private fun copyToCache(uri: Uri): String {
        val file = File(externalCacheDir, "leaf_${System.currentTimeMillis()}.jpg")

        contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return file.absolutePath
    }


    private fun loadBitmapWithRotation(file: File): Bitmap {
        val exif = ExifInterface(file.absolutePath)

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalArgumentException("Cannot decode bitmap")

        if (rotation == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }


    private fun saveResult() {
        val pred = prediction ?: return
        val path = finalPath ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.get(this@PredictionActivity).scanResultDao()

            dao.insert(
                ScanResult(
                    imageUri = path,
                    leafName = leafName,
                    prediction = pred,
                    timestamp = System.currentTimeMillis()
                )
            )
            finish()
        }
    }
}
