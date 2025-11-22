package com.example.leaf_scanner_test_2

// MainActivity.kt
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            openPredictionScreen(it.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // tu zrobisz layout wg makiety

        findViewById<Button>(R.id.btnFromFile).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<Button>(R.id.btnFromCamera).setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        findViewById<Button>(R.id.btnPreviousResults).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun openPredictionScreen(imageUri: String) {
        val intent = Intent(this, PredictionActivity::class.java)
        intent.putExtra(PredictionActivity.EXTRA_IMAGE_URI, imageUri)
        startActivity(intent)
    }
}
