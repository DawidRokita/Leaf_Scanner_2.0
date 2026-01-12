package com.example.leaf_scanner_test_2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "extra_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val image = findViewById<ImageView>(R.id.imageLeaf)
        val tvPrediction = findViewById<TextView>(R.id.tvPrediction)
        val tvDate = findViewById<TextView>(R.id.tvDate)

        val id = intent.getLongExtra(EXTRA_ID, -1)

        lifecycleScope.launch {
            val dao = AppDatabase.get(this@DetailActivity).scanResultDao()
            val result = withContext(Dispatchers.IO) { dao.getById(id) } ?: return@launch

            tvPrediction.text = result.prediction

            val status = extractStatus(result.prediction)
            tvPrediction.setTextColor(
                if (status.equals("Healthy", true)) 0xFF2E7D32.toInt() else 0xFFC62828.toInt()
            )

            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            tvDate.text = sdf.format(Date(result.timestamp))

            val bitmap = loadBitmapWithRotation(result.imageUri)
            image.setImageBitmap(bitmap)
        }
    }

    private fun extractStatus(predictionText: String): String {
        return predictionText.lines()
            .firstOrNull { it.trim().startsWith("Status:", ignoreCase = true) }
            ?.substringAfter(":", "")
            ?.trim()
            ?: "-"
    }

    private fun loadBitmapWithRotation(path: String): Bitmap {
        val file = File(path)

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
            ?: throw IllegalArgumentException("Cannot decode bitmap from file")

        if (rotation == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
