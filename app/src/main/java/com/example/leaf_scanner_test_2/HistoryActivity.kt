package com.example.leaf_scanner_test_2

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class HistoryActivity : AppCompatActivity() {

    private lateinit var adapter: ScanResultAdapter
    private lateinit var dao: ScanResultDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        dao = AppDatabase.get(this).scanResultDao()

        adapter = ScanResultAdapter(
            onClick = { item ->
                val i = Intent(this, DetailActivity::class.java)
                i.putExtra(DetailActivity.EXTRA_ID, item.id)
                startActivity(i)
            },
            onDeleteRequest = { item, _ ->
                showDeleteDialog(item)
            }

        )

        val recycler = findViewById<RecyclerView>(R.id.recyclerHistory)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        lifecycleScope.launch {
            dao.getAll().collectLatest { list ->
                adapter.submitList(list)
            }
        }
    }

    private fun showDeleteDialog(item: ScanResult) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete?")
            .setMessage("Are you sure you want to delete?")
            .setPositiveButton("Delete") { _: DialogInterface, _: Int ->
                deleteItem(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem(item: ScanResult) {
        lifecycleScope.launch(Dispatchers.IO) {

            // 1) Usuń plik zdjęcia z pamięci
            try {
                val file = File(item.imageUri)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2) Usuń rekord z bazy
            dao.delete(item)
        }
    }
}
