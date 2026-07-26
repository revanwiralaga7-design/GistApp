package com.gistapp.ui.gistdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gistapp.R
import com.gistapp.data.model.Gist
import com.gistapp.data.remote.RetrofitClient
import com.gistapp.data.repository.GistRepository
import com.gistapp.databinding.ActivityGistDetailBinding
import com.gistapp.ui.create.CreateGistActivity
import com.gistapp.util.TokenManager
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class GistDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGistDetailBinding
    private lateinit var repository: GistRepository
    private lateinit var tokenManager: TokenManager
    private var gist: Gist? = null
    private var gistId: String? = null
    private val fullContents = mutableMapOf<String, String>()
    private var isOwner = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGistDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        val apiService = RetrofitClient.getApiService(tokenManager)
        repository = GistRepository(apiService)
        isOwner = tokenManager.hasToken()

        gistId = intent.getStringExtra("gist_id")
        if (gistId.isNullOrEmpty()) {
            Toast.makeText(this, "Gist ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadGistDetail()

        binding.btnShare.setOnClickListener { shareGist() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (isOwner) {
            menu.add(0, 100, 0, "Edit").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 100) {
            editGistByToolbar()
            return true
        }
        return super.onOptionsItemSelected(item)
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
                fetchTruncatedFiles(gistData)
                invalidateOptionsMenu()
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
        binding.tvOwner.text = gist.owner?.login ?: "anonymous"
        binding.tvVisibility.text = if (gist.isPublic) "Public" else "Secret"
        binding.tvFileCount.text = "${gist.files.size} file(s)"

        val fileContainer = binding.filesContainer
        fileContainer.removeAllViews()

        gist.files.forEach { (fileName, file) ->
            val fileView = layoutInflater.inflate(R.layout.item_gist_file_detail, fileContainer, false)
            val tvFileName = fileView.findViewById<TextView>(R.id.tvFileName)
            val tvFileLang = fileView.findViewById<TextView>(R.id.tvFileLanguage)
            val tvFileContent = fileView.findViewById<TextView>(R.id.tvFileContent)
            val btnEditFile = fileView.findViewById<Button>(R.id.btnEditFile)
            val btnCopyFile = fileView.findViewById<Button>(R.id.btnCopyFile)
            val btnShowMore = fileView.findViewById<Button>(R.id.btnShowMore)
            val btnOpenRaw = fileView.findViewById<Button>(R.id.btnOpenRaw)
            val progressFile = fileView.findViewById<View>(R.id.progressFile)

            tvFileName.text = file.filename ?: "(unknown)"
            tvFileLang.text = file.language ?: "text"
            tvFileContent.movementMethod = ScrollingMovementMethod()

            val content = file.content ?: "(tidak ada konten)"
            val isTruncated = file.truncated == true

            if (isTruncated) {
                tvFileContent.text = content + "\n\nMemuat konten lengkap..."
                btnShowMore.visibility = View.VISIBLE
                btnShowMore.text = "Loading..."
                btnShowMore.isEnabled = false
                progressFile.visibility = View.VISIBLE
            } else {
                tvFileContent.text = content
                btnShowMore.visibility = View.GONE
                progressFile.visibility = View.GONE
            }

            // ✅ CAPTURE spesifik: gistId + filename + content — bukan ambil firstOrNull
            if (isOwner) {
                btnEditFile.visibility = View.VISIBLE
                val capturedGistId = gist.id
                val capturedFilename = fileName
                val capturedDescription = gist.description ?: ""
                val capturedPublic = gist.isPublic

                tvFileContent.setOnClickListener {
                    startEdit(capturedGistId, capturedFilename, capturedDescription, capturedPublic)
                }
                btnEditFile.setOnClickListener {
                    startEdit(capturedGistId, capturedFilename, capturedDescription, capturedPublic)
                }
            } else {
                btnEditFile.visibility = View.GONE
                tvFileContent.setOnClickListener(null)
            }

            btnShowMore.setOnClickListener {
                val full = fullContents[fileName]
                if (full != null) {
                    tvFileContent.text = full
                    btnShowMore.visibility = View.GONE
                }
            }

            btnCopyFile.setOnClickListener {
                val textToCopy = fullContents[fileName] ?: content
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("gist_file", textToCopy))
                Toast.makeText(this, "Konten disalin!", Toast.LENGTH_SHORT).show()
            }

            btnOpenRaw.setOnClickListener {
                file.rawUrl?.let { url ->
                    startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                }
            }

            fileContainer.addView(fileView)
        }

        binding.btnDelete.visibility = if (isOwner) View.VISIBLE else View.GONE
    }

    /** Edit via toolbar — ambil info dari gist yang sudah diload */
    private fun editGistByToolbar() {
        val g = gist ?: return
        val firstFile = g.files.values.firstOrNull()
        val firstFilename = g.files.keys.firstOrNull()
        val bestContent = firstFilename?.let { fullContents[it] } ?: firstFile?.content ?: ""
        startEdit(g.id, firstFilename ?: "", g.description ?: "", g.isPublic, bestContent)
    }

    /** Buka CreateGistActivity dengan data spesifik */
    private fun startEdit(
        gistId: String,
        filename: String,
        description: String,
        isPublic: Boolean,
        preContent: String? = null
    ) {
        val content = preContent ?: fullContents[filename] ?: ""
        val intent = Intent(this, CreateGistActivity::class.java).apply {
            putExtra("edit_gist_id", gistId)
            putExtra("edit_gist_description", description)
            putExtra("edit_gist_public", isPublic)
            putExtra("edit_gist_filename", filename)
            putExtra("edit_gist_content", content)
        }
        startActivity(intent)
    }

    private fun fetchTruncatedFiles(gist: Gist) {
        val token = tokenManager.getToken()

        gist.files.forEach { (filename, file) ->
            if (file.truncated == true && !file.rawUrl.isNullOrEmpty()) {
                lifecycleScope.launch {
                    val fullContent = repository.fetchRawContent(file.rawUrl!!, token)
                    if (fullContent.isNotEmpty()) {
                        fullContents[filename] = fullContent
                        updateFileContentInView(filename, fullContent)
                    } else {
                        updateButtonToRaw(filename)
                    }
                }
            }
        }
    }

    private fun updateFileContentInView(filename: String, fullContent: String) {
        val fileContainer = binding.filesContainer
        for (i in 0 until fileContainer.childCount) {
            val child = fileContainer.getChildAt(i)
            val tvFn = child.findViewById<TextView>(R.id.tvFileName)
            if (tvFn?.text == filename) {
                child.findViewById<TextView>(R.id.tvFileContent)?.text = fullContent
                child.findViewById<Button>(R.id.btnShowMore)?.visibility = View.GONE
                child.findViewById<View>(R.id.progressFile)?.visibility = View.GONE
                break
            }
        }
    }

    private fun updateButtonToRaw(filename: String) {
        val fileContainer = binding.filesContainer
        for (i in 0 until fileContainer.childCount) {
            val child = fileContainer.getChildAt(i)
            if (child.findViewById<TextView>(R.id.tvFileName)?.text == filename) {
                child.findViewById<View>(R.id.progressFile)?.visibility = View.GONE
                child.findViewById<Button>(R.id.btnShowMore)?.apply {
                    text = "Buka Raw URL"
                    isEnabled = true
                    setOnClickListener {
                        gist?.files?.get(filename)?.rawUrl?.let { url ->
                            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                        }
                    }
                }
                break
            }
        }
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

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Gist")
            .setMessage("Yakin hapus gist ini?")
            .setPositiveButton("Hapus") { _, _ -> deleteGist() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteGist() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            repository.deleteGist(gistId!!).onSuccess {
                Toast.makeText(this@GistDetailActivity, "Gist dihapus!", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure {
                Toast.makeText(this@GistDetailActivity, it.message, Toast.LENGTH_LONG).show()
            }
            binding.progressBar.visibility = View.GONE
        }
    }
}
