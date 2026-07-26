package com.gistapp.ui.create

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gistapp.R
import com.gistapp.data.model.CreateGistRequest
import com.gistapp.data.model.GistFileContent
import com.gistapp.data.remote.RetrofitClient
import com.gistapp.data.repository.GistRepository
import com.gistapp.databinding.ActivityCreateGistBinding
import com.gistapp.util.TokenManager
import kotlinx.coroutines.launch

/**
 * Halaman create / edit gist.
 * Edit mode: menerima edit_gist_id, edit_gist_description,
 *            edit_gist_public, edit_gist_filename, edit_gist_content.
 */
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

        // Cek mode edit
        editGistId = intent.getStringExtra("edit_gist_id")
        isEditMode = !editGistId.isNullOrEmpty()

        if (isEditMode) {
            binding.toolbar.title = "Edit Gist"
            binding.btnCreate.text = "Update Gist"

            // Pre-fill data lama
            editOldFilename = intent.getStringExtra("edit_gist_filename") ?: ""
            val oldContent = intent.getStringExtra("edit_gist_content") ?: ""

            binding.etFilename.setText(editOldFilename)
            binding.etContent.setText(oldContent)
            binding.etDescription.setText(intent.getStringExtra("edit_gist_description") ?: "")
            binding.switchPublic.isChecked = intent.getBooleanExtra("edit_gist_public", true)

            // Kalau content kosong (truncated), beri hint
            if (oldContent.isEmpty() && !editOldFilename.isNullOrEmpty()) {
                binding.tilContent.helperText = "⚠ Konten asli tidak tersedia (terpotong). Edit tetap bisa dilakukan."
            }
        } else {
            binding.toolbar.title = "Buat Gist Baru"
        }

        binding.btnCreate.setOnClickListener { createOrUpdateGist() }
    }

    private fun createOrUpdateGist() {
        val filename = binding.etFilename.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val content = binding.etContent.text.toString()
        val isPublic = binding.switchPublic.isChecked

        // Validasi
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

        // Build file map (nullable value = delete file → untuk rename)
        val filesMap = mutableMapOf<String, GistFileContent?>()
        filesMap[filename] = GistFileContent(content)

        if (isEditMode && !editOldFilename.isNullOrEmpty() && editOldFilename != filename) {
            // User rename file → hapus file lama
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
            val result = if (isEditMode) {
                repository.updateGist(editGistId!!, request)
            } else {
                repository.createGist(request)
            }

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
                Toast.makeText(
                    this@CreateGistActivity,
                    "Gagal: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
