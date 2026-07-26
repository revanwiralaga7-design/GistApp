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
                // Sudah ada konten → langsung tampilkan
                binding.etContent.setText(preContent)
            } else {
                // Konten kosong (mungkin dari list yg truncated) → fetch ulang
                binding.etContent.setText("⏳ Memuat konten...")
                binding.etContent.isEnabled = false
                binding.btnCreate.isEnabled = false
                fetchContentForEdit()
            }
        } else {
            binding.toolbar.title = "Buat Gist Baru"
        }

        binding.btnCreate.setOnClickListener { createOrUpdateGist() }
    }

    /** Fetch full gist content saat edit mode tapi konten kosong */
    private fun fetchContentForEdit() {
        binding.tilContent.helperText = "Mengambil konten dari server..."

        lifecycleScope.launch {
            val result = repository.getGist(editGistId!!)

            result.onSuccess { gist ->
                val firstFile = gist.files.values.firstOrNull()

                if (firstFile?.truncated == true && !firstFile.rawUrl.isNullOrEmpty()) {
                    // File truncated → fetch raw
                    val raw = repository.fetchRawContent(firstFile.rawUrl!!, tokenManager.getToken())
                    if (raw.isNotEmpty()) {
                        binding.etContent.setText(raw)
                    } else {
                        binding.etContent.setText(firstFile.content ?: "(konten tidak tersedia)")
                    }
                } else {
                    // Konten normal
                    binding.etContent.setText(firstFile?.content ?: "")
                }

                // Update filename juga (mungkin berbeda dari list)
                val actualFilename = gist.files.keys.firstOrNull()
                if (!actualFilename.isNullOrEmpty() && actualFilename != editOldFilename) {
                    editOldFilename = actualFilename
                    binding.etFilename.setText(actualFilename)
                }

                binding.tilContent.helperText = null
            }.onFailure { error ->
                binding.etContent.setText("")
                binding.tilContent.helperText = "⚠ Gagal memuat: ${error.message}"
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
                Toast.makeText(
                    this@CreateGistActivity, "Gagal: ${error.message}", Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
