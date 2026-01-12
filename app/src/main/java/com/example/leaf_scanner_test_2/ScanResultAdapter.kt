package com.example.leaf_scanner_test_2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File

class ScanResultAdapter(
    private val onClick: (ScanResult) -> Unit,
    private val onDeleteRequest: (ScanResult, Int) -> Unit
) : ListAdapter<ScanResult, ScanResultAdapter.VH>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<ScanResult>() {
        override fun areItemsTheSame(old: ScanResult, new: ScanResult) = old.id == new.id
        override fun areContentsTheSame(old: ScanResult, new: ScanResult) = old == new
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.imgAvatar)
        val title: TextView = view.findViewById(R.id.tvItemTitle)
        val subtitle: TextView = view.findViewById(R.id.tvItemSubtitle)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        var loadJob: Job? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_result, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        holder.title.text = item.leafName
        holder.subtitle.text = buildStyledSubtitle(item.prediction, item.timestamp)

        holder.avatar.setImageResource(R.drawable.avatar_mask)

        holder.loadJob?.cancel()
        holder.loadJob = CoroutineScope(Dispatchers.IO).launch {
            val bmp = decodeThumbnail(item.imageUri, 150)
            withContext(Dispatchers.Main) {
                if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                    bmp?.let { holder.avatar.setImageBitmap(it) }
                }
            }
        }

        holder.itemView.setOnClickListener { onClick(item) }

        holder.btnDelete.setOnClickListener {
            val index = holder.adapterPosition
            if (index != RecyclerView.NO_POSITION) onDeleteRequest(item, index)
        }
    }

    private fun buildStyledSubtitle(predictionText: String, timestamp: Long): CharSequence {
        val statusPl = extractLineValue(predictionText, "Status") ?: "-"
        val confidence = extractLineValue(predictionText, "Confidence") ?: "—"
        val date = formatDate(timestamp)

        val isHealthy = statusPl.equals("Healthy", true)
        val statusEn = if (isHealthy) "Healthy" else "Diseased"

        val full = "$statusEn ($confidence) • $date"
        val spannable = SpannableString(full)

        val color = if (isHealthy) 0xFF2E7D32.toInt() else 0xFFC62828.toInt()
        spannable.setSpan(
            ForegroundColorSpan(color),
            0,
            statusEn.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannable
    }

    private fun extractLineValue(predictionText: String, key: String): String? {
        val line = predictionText.lines()
            .firstOrNull { it.trim().startsWith("$key:", ignoreCase = true) }
            ?: return null
        return line.substringAfter(":", "").trim().ifEmpty { null }
    }

    private fun decodeThumbnail(path: String, reqSize: Int): Bitmap? {
        return try {
            val file = File(path)
            if (!file.exists()) return null

            val exif = ExifInterface(path)
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

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)

            var scale = 1
            while (options.outWidth / scale > reqSize && options.outHeight / scale > reqSize) {
                scale *= 2
            }

            val finalOptions = BitmapFactory.Options().apply { inSampleSize = scale }
            val bitmap = BitmapFactory.decodeFile(path, finalOptions) ?: return null

            if (rotation == 0f) return bitmap
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm")
        return sdf.format(java.util.Date(timestamp))
    }
}
