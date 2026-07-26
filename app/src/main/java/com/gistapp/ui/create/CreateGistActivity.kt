package com.gistapp.ui.create

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gistapp.data.model.CreateGistRequest
import com.gistapp.data.model.GistFileContent
import com.gistapp.data.remote.RetrofitClient
import com.gistapp.data.repository.GistRepository
import com.gistapp.databinding.ActivityCreateGistBinding
import com.gistapp.util.TokenManager
import kotlinx.coroutines.launch

class CreateGistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGistBinding
    private lateinit var repository: GistRepository
    private lateinit var tokenManager: TokenManager
    private var editGistId: String? = null
    private var editOldFilename: String? = null
    private var isEditMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        val apiService = RetrofitClient.getApiService(tokenManager)
        repository = GistRepository(apiService)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        editGistId = intent.getStringExtra("edit_gist_id")
        isEditMode = !editGistId.isNullOrEmpty()

        if (isEditMode) {
            binding.toolbar.title = "Edit Gist"
            binding.btnCreate.text = "Update Gist"

            editOldFilename = intent.getStringExtra("edit_gist_filename") ?: ""
            val preContent = intent.getStringExtra("edit_gist_content") ?: ""

            binding.etFilename.setText(editOldFilename)
            binding.etDescription.setText(intent.getStringExtra("edit_gist_description") ?: "")
            binding.switchPublic.isChecked = intent.getBooleanExtra("edit_gist_public", true)

            if (preContent.isNotEmpty()) {
                binding.etContent.setText(preContent)
            } else {
                // Konten kosong → fetch dari API (file truncated)
                binding.etContent.setText("Memuat konten...")
                binding.etContent.isEnabled = false
                binding.btnCreate.isEnabled = false
                fetchContentForEdit()
            }
        } else {
            binding.toolbar.title = "Buat Gist Baru"
        }

        binding.btnCreate.setOnClickListener { createOrUpdateGist() }
    }

    /** Fetch content dari API — TAPI JANGAN overwrite filename */
    private fun fetchContentForEdit() {
        binding.tilContent.helperText = "Mengambil konten dari server..."

        lifecycleScope.launch {
            val result = repository.getGist(editGistId!!)
            result.onSuccess { gist ->
                // Ambil konten untuk file yang SAMA dengan editOldFilename
                val file = gist.files[editOldFilename]
                if (file != null) {
                    if (file.truncated == true && !file.rawUrl.isNullOrEmpty()) {
                        val raw = repository.fetchRawContent(file.rawUrl!!, tokenManager.getToken())
                        binding.etContent.setText(raw.ifEmpty { file.content ?: "(tidak tersedia)" })
                    } else {
                        binding.etContent.setText(file.content ?: "")
                    }
                } else {
                    // File tidak ditemukan di gist — fallback ke file pertama
                    val first = gist.files.values.firstOrNull()
                    binding.etContent.setText(first?.content ?: "(tidak tersedia)")
                    binding.tilContent.helperText = "⚠ File '${editOldFilename}' tidak ditemukan, menampilkan file pertama"
                }
            }.onFailure { error ->
                binding.etContent.setText("")
                binding.tilContent.helperText = "Gagal memuat: ${error.message}"
            }

            binding.etContent.isEnabled = true
            binding.btnCreate.isEnabled = true
        }
    }

    private fun createOrUpdateGist() {
        val filename = binding.etFilename.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val content = binding.etContent.text.toString()
        val isPublic = binding.switchPublic.isChecked

        if (TextUtils.isEmpty(filename)) {
            binding.tilFilename.error = "Nama file wajib diisi"
            return
        }
        binding.tilFilename.error = null

        if (TextUtils.isEmpty(content)) {
            binding.tilContent.error = "Konten tidak boleh kosong"
            return
        }
        binding.tilContent.error = null

        val filesMap = mutableMapOf<String, GistFileContent?>()
        filesMap[filename] = GistFileContent(content)

        // Rename: hapus file lama
        if (isEditMode && !editOldFilename.isNullOrEmpty() && editOldFilename != filename) {
            filesMap[editOldFilename!!] = null
        }

        val request = CreateGistRequest(
            description = description.ifEmpty { null },
            isPublic = isPublic,
            files = filesMap
        )

        binding.progressBar.visibility = View.VISIBLE
        binding.btnCreate.isEnabled = false

        lifecycleScope.launch {
            val result = if (isEditMode) repository.updateGist(editGistId!!, request)
            else repository.createGist(request)

            binding.progressBar.visibility = View.GONE
            binding.btnCreate.isEnabled = true

            result.onSuccess {
                Toast.makeText(
                    this@CreateGistActivity,
                    if (isEditMode) "Gist berhasil diupdate!" else "Gist berhasil dibuat!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }.onFailure { error ->
                Toast.makeText(this@CreateGistActivity, "Gagal: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
