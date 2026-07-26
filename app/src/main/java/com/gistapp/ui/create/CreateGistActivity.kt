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
 * Jika menerima "edit_gist_id" di intent, mode edit.
 */
class CreateGistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGistBinding
    private lateinit var repository: GistRepository
    private lateinit var tokenManager: TokenManager

    private var editGistId: String? = null
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
            binding.etDescription.setText(intent.getStringExtra("edit_gist_description") ?: "")
            binding.switchPublic.isChecked = intent.getBooleanExtra("edit_gist_public", true)
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

        val request = CreateGistRequest(
            description = description.ifEmpty { null },
            isPublic = isPublic,
            files = mapOf(filename to GistFileContent(content))
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
