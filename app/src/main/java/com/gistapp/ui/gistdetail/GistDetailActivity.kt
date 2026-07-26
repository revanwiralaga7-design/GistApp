package com.gistapp.ui.gistdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gistapp.R
import com.gistapp.data.model.Gist
import com.gistapp.data.model.GistFile
import com.gistapp.data.remote.RetrofitClient
import com.gistapp.data.repository.GistRepository
import com.gistapp.databinding.ActivityGistDetailBinding
import com.gistapp.ui.create.CreateGistActivity
import com.gistapp.util.TokenManager
import kotlinx.coroutines.launch

/**
 * Halaman detail gist — menampilkan semua file, deskripsi, dan aksi.
 */
class GistDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGistDetailBinding
    private lateinit var repository: GistRepository
    private lateinit var tokenManager: TokenManager
    private var gist: Gist? = null
    private var gistId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGistDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        val apiService = RetrofitClient.getApiService(tokenManager)
        repository = GistRepository(apiService)

        gistId = intent.getStringExtra("gist_id")
        if (gistId == null) {
            Toast.makeText(this, "Gist ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadGistDetail()

        binding.btnShare.setOnClickListener { shareGist() }
        binding.btnEdit.setOnClickListener { editGist() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun loadGistDetail() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE

        lifecycleScope.launch {
            val result = repository.getGist(gistId!!)
            binding.progressBar.visibility = View.GONE

            result.onSuccess { gistData ->
                gist = gistData
                displayGist(gistData)
            }.onFailure { error ->
                Toast.makeText(this@GistDetailActivity, error.message, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun displayGist(gist: Gist) {
        binding.contentLayout.visibility = View.VISIBLE
        binding.toolbar.title = gist.files.values.firstOrNull()?.filename ?: "Gist Detail"

        binding.tvDescription.text = gist.description ?: "(tanpa deskripsi)"
        binding.tvOwner.text = "👤 ${gist.owner?.login ?: "anonymous"}"
        binding.tvVisibility.text = if (gist.isPublic) "🌐 Public" else "🔒 Secret"
        binding.tvFileCount.text = "${gist.files.size} file(s)"

        // Tampilkan setiap file
        val fileContainer = binding.filesContainer
        fileContainer.removeAllViews()

        gist.files.forEach { (_, file) ->
            val fileView = layoutInflater.inflate(R.layout.item_gist_file, fileContainer, false)
            val tvFileName = fileView.findViewById<android.widget.TextView>(R.id.tvFileName)
            val tvFileLang = fileView.findViewById<android.widget.TextView>(R.id.tvFileLanguage)
            val tvFileContent = fileView.findViewById<android.widget.TextView>(R.id.tvFileContent)
            val btnCopyFile = fileView.findViewById<android.widget.Button>(R.id.btnCopyFile)
            val btnOpenRaw = fileView.findViewById<android.widget.Button>(R.id.btnOpenRaw)

            tvFileName.text = file.filename ?: "(unknown)"
            tvFileLang.text = file.language ?: "text"

            // Tampilkan konten (dipotong jika panjang)
            val content = file.content ?: "(tidak ada konten)"
            tvFileContent.text = if (content.length > 2000) {
                content.take(2000) + "\n\n... (terpotong, buka raw URL untuk selengkapnya)"
            } else {
                content
            }

            btnCopyFile.setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("gist_file", content))
                Toast.makeText(this, "Konten disalin!", Toast.LENGTH_SHORT).show()
            }

            btnOpenRaw.setOnClickListener {
                file.rawUrl?.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    startActivity(intent)
                }
            }

            fileContainer.addView(fileView)
        }

        // Tombol edit/delete hanya untuk pemilik
        val isOwner = tokenManager.hasToken()
        binding.btnEdit.visibility = if (isOwner) View.VISIBLE else View.GONE
        binding.btnDelete.visibility = if (isOwner) View.VISIBLE else View.GONE
    }

    private fun shareGist() {
        gist?.htmlUrl?.let { url ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
                putExtra(Intent.EXTRA_SUBJECT, "GitHub Gist")
            }
            startActivity(Intent.createChooser(intent, "Bagikan Gist"))
        }
    }

    private fun editGist() {
        gist?.let {
            val intent = Intent(this, CreateGistActivity::class.java).apply {
                putExtra("edit_gist_id", it.id)
                putExtra("edit_gist_description", it.description ?: "")
                putExtra("edit_gist_public", it.isPublic)
            }
            startActivity(intent)
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Gist")
            .setMessage("Yakin hapus gist ini? Tindakan ini tidak bisa dibatalkan.")
            .setPositiveButton("Hapus") { _, _ -> deleteGist() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteGist() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            val result = repository.deleteGist(gistId!!)
            binding.progressBar.visibility = View.GONE

            result.onSuccess {
                Toast.makeText(this@GistDetailActivity, "Gist dihapus!", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { error ->
                Toast.makeText(this@GistDetailActivity, error.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
