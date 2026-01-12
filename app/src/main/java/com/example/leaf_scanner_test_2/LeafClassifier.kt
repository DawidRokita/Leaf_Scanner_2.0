package com.example.leaf_scanner_test_2

import android.content.Context
import android.graphics.Bitmap
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import kotlin.math.exp

class LeafClassifier private constructor(context: Context) {

    private val module = LiteModuleLoader.load(
        assetFilePath(context, "leaf_model_mobile_cpu.ptl")
    )

    private val classes = listOf(
        "Apple___Apple_scab", "Apple___Black_rot", "Apple___Cedar_apple_rust", "Apple___healthy",
        "Blueberry___healthy", "Cherry_(including_sour)___Powdery_mildew", "Cherry_(including_sour)___healthy",
        "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot", "Corn_(maize)___Common_rust_",
        "Corn_(maize)___Northern_Leaf_Blight", "Corn_(maize)___healthy", "Grape___Black_rot",
        "Grape___Esca_(Black_Measles)", "Grape___Leaf_blight_(Isariopsis_Leaf_Spot)", "Grape___healthy",
        "Orange___Haunglongbing_(Citrus_greening)", "Peach___Bacterial_spot", "Peach___healthy",
        "Pepper,_bell___Bacterial_spot", "Pepper,_bell___healthy", "Potato___Early_blight",
        "Potato___Late_blight", "Potato___healthy", "Raspberry___healthy", "Soybean___healthy",
        "Squash___Powdery_mildew", "Strawberry___Leaf_scorch", "Strawberry___healthy",
        "Tomato___Bacterial_spot", "Tomato___Early_blight", "Tomato___Late_blight", "Tomato___Leaf_Mold",
        "Tomato___Septoria_leaf_spot", "Tomato___Spider_mites Two-spotted_spider_mite", "Tomato___Target_Spot",
        "Tomato___Tomato_Yellow_Leaf_Curl_Virus", "Tomato___Tomato_mosaic_virus", "Tomato___healthy"
    )

    data class Prediction(
        val plant: String,
        val isHealthy: Boolean,
        val disease: String?,
        val confidence: Float
    )

    fun predictTop1(bitmap: Bitmap): Prediction? {
        val scores = runInference(bitmap) ?: return null
        if (scores.size != classes.size) return null

        val probs = softmax(scores)
        val bestIdx = argMax(probs)
        val rawLabel = classes[bestIdx]
        val confidence = probs[bestIdx]

        return parseLabel(rawLabel, confidence)
    }

    private fun runInference(bitmap: Bitmap): FloatArray? {
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val bmp = if (resized.config == Bitmap.Config.ARGB_8888) resized
        else resized.copy(Bitmap.Config.ARGB_8888, false)

        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            bmp,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )

        val out = module.forward(IValue.from(inputTensor))
        if (!out.isTensor) return null

        val t: Tensor = out.toTensor()
        return t.dataAsFloatArray
    }

    private fun parseLabel(label: String, confidence: Float): Prediction {
        val parts = label.split("___", limit = 2)
        val plant = prettify(parts[0])
        val second = parts.getOrNull(1) ?: ""

        val healthy = second.equals("healthy", ignoreCase = true)
        val disease = if (healthy) null else prettify(second)

        return Prediction(
            plant = plant,
            isHealthy = healthy,
            disease = disease,
            confidence = confidence
        )
    }

    private fun prettify(s: String): String =
        s.replace("_", " ")
            .replace("  ", " ")
            .replace("(", " (")
            .replace(" )", ")")
            .trim()

    private fun argMax(x: FloatArray): Int {
        var idx = 0
        var best = Float.NEGATIVE_INFINITY
        for (i in x.indices) {
            if (x[i] > best) {
                best = x[i]
                idx = i
            }
        }
        return idx
    }

    private fun softmax(x: FloatArray): FloatArray {
        val max = x.maxOrNull() ?: return x
        val exp = DoubleArray(x.size)
        var sum = 0.0
        for (i in x.indices) {
            exp[i] = kotlin.math.exp((x[i] - max).toDouble())
            sum += exp[i]
        }
        return FloatArray(x.size) { (exp[it] / sum).toFloat() }
    }

    companion object {
        @Volatile private var INSTANCE: LeafClassifier? = null

        fun get(context: Context): LeafClassifier =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LeafClassifier(context).also { INSTANCE = it }
            }

        private fun assetFilePath(context: Context, assetName: String): String {
            val file = java.io.File(context.filesDir, assetName)
            context.assets.open(assetName).use { input ->
                java.io.FileOutputStream(file, false).use { output ->
                    input.copyTo(output)
                }
            }
            return file.absolutePath
        }
    }
}
