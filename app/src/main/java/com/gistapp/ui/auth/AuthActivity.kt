package com.gistapp.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.gistapp.R
import com.gistapp.data.remote.RetrofitClient
import com.gistapp.data.repository.GistRepository
import com.gistapp.databinding.ActivityAuthBinding
import com.gistapp.ui.MainActivity
import com.gistapp.util.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var tokenManager: TokenManager

    companion object {
        // 🔧 GANTI dengan client_id dari OAuth App GitHub kamu:
        // Buat di: https://github.com/settings/developers → New OAuth App
        // Callback URL: gistapp://oauth
        private const val OAUTH_CLIENT_ID = "Ov23li..." // TODO: ganti dengan punyamu
        private const val OAUTH_REDIRECT_URI = "gistapp://oauth"
        private const val OAUTH_SCOPES = "gist,user,repo"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        // Handle OAuth callback dari browser
        handleOAuthCallback(intent)

        if (tokenManager.hasToken()) {
            navigateToMain()
            return
        }

        // OAuth button
        binding.cardOAuth.setOnClickListener { startOAuthFlow() }

        // Token manual
        binding.btnLogin.setOnClickListener {
            val token = binding.etToken.text.toString().trim()
            if (TextUtils.isEmpty(token)) {
                binding.tilToken.error = "Token tidak boleh kosong"
                return@setOnClickListener
            }
            binding.tilToken.error = null
            verifyAndLogin(token)
        }

        binding.tvSkip.setOnClickListener { navigateToMain() }
        binding.tvHowToGetToken.setOnClickListener {
            binding.tvTokenHint.visibility =
                if (binding.tvTokenHint.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
    }

    // ===== OAuth Flow =====

    private fun startOAuthFlow() {
        if (OAUTH_CLIENT_ID.startsWith("Ov23li...") || OAUTH_CLIENT_ID.length < 10) {
            Toast.makeText(this, "⚠ Client ID belum dikonfigurasi. Buka kode AuthActivity untuk setup OAuth.", Toast.LENGTH_LONG).show()
            // Fallback: buka halaman generate token
            val url = "https://github.com/settings/tokens/new?scopes=gist,user,repo&description=GistApp"
            openInBrowser(url)
            return
        }

        val authUrl = "https://github.com/login/oauth/authorize" +
                "?client_id=$OAUTH_CLIENT_ID" +
                "&redirect_uri=$OAUTH_REDIRECT_URI" +
                "&scope=$OAUTH_SCOPES"

        try {
            val builder = CustomTabsIntent.Builder()
            builder.setToolbarColor(resources.getColor(R.color.surface, theme))
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(this, Uri.parse(authUrl))
        } catch (e: Exception) {
            // Fallback ke browser biasa
            openInBrowser(authUrl)
        }
    }

    private fun openInBrowser(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun handleOAuthCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "gistapp" || uri.host != "oauth") return

        val code = uri.getQueryParameter("code")
        if (code.isNullOrEmpty()) {
            val error = uri.getQueryParameter("error_description")
                ?: uri.getQueryParameter("error") ?: "OAuth gagal"
            Toast.makeText(this, "OAuth gagal: $error", Toast.LENGTH_LONG).show()
            return
        }

        // Tukar code dengan access token
        exchangeCodeForToken(code)
    }

    private fun exchangeCodeForToken(code: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.cardOAuth.isEnabled = false
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val token = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()

                    val body = mapOf(
                        "client_id" to OAUTH_CLIENT_ID,
                        "client_secret" to "", // GitHub: optional untuk public OAuth apps
                        "code" to code,
                        "redirect_uri" to OAUTH_REDIRECT_URI
                    ).entries.joinToString("&") { "${it.key}=${it.value}" }

                    val request = Request.Builder()
                        .url("https://github.com/login/oauth/access_token")
                        .header("Accept", "application/json")
                        .header("User-Agent", "GistApp-Android")
                        .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                        .build()

                    val response = client.newCall(request).execute()
                    val json = JSONObject(response.body?.string() ?: "{}")

                    if (json.has("error")) {
                        throw Exception(json.optString("error_description", "OAuth exchange failed"))
                    }
                    json.optString("access_token", "")
                }

                if (token.isNotEmpty()) {
                    tokenManager.saveToken(token)
                    verifyAndLogin(token)
                } else {
                    throw Exception("Token kosong dari GitHub")
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.cardOAuth.isEnabled = true
                binding.btnLogin.isEnabled = true
                Toast.makeText(this@AuthActivity, "OAuth gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ===== Token Manual =====

    private fun verifyAndLogin(token: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        binding.cardOAuth.isEnabled = false

        tokenManager.saveToken(token)
        val apiService = RetrofitClient.getApiService(tokenManager)
        val repository = GistRepository(apiService)

        lifecycleScope.launch {
            val result = repository.verifyToken()
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            binding.cardOAuth.isEnabled = true

            result.onSuccess { user ->
                Toast.makeText(this@AuthActivity, "Login berhasil! Selamat datang, ${user.login}", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }.onFailure { error ->
                tokenManager.clearToken()
                Toast.makeText(this@AuthActivity, "Login gagal: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
