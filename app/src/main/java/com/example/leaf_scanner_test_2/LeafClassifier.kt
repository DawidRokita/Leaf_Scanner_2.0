import android.content.Context
import android.graphics.Bitmap
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils

class LeafClassifier private constructor(context: Context) {

    private val module = LiteModuleLoader.load(
        assetFilePath(context, "model.ptl")
    )

    private val classes = listOf("Diseased", "Healthy")

    fun classify(bitmap: Bitmap): String {
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            resized,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )

        val output = module.forward(IValue.from(inputTensor)).toTensor()
        val scores = output.dataAsFloatArray

        return if (scores[0] > scores[1]) classes[0] else classes[1]
    }

    companion object {
        @Volatile private var INSTANCE: LeafClassifier? = null

        fun get(context: Context): LeafClassifier =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LeafClassifier(context).also { INSTANCE = it }
            }

        private fun assetFilePath(context: Context, assetName: String): String {
            val file = java.io.File(context.filesDir, assetName)
            if (file.exists() && file.length() > 0) return file.absolutePath

            context.assets.open(assetName).use { input ->
                java.io.FileOutputStream(file).use { output ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            return file.absolutePath
        }
    }
}
