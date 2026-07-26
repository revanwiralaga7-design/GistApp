package com.gistapp.ui.auth

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gistapp.R
import com.gistapp.util.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OAuthActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tokenManager: TokenManager

    companion object {
        const val OAUTH_CLIENT_ID = "Ov23li..." // TODO: ganti dengan Client ID kamu
        const val OAUTH_SCOPE = "gist,user,repo"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oauth)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        tokenManager = TokenManager(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE

                if (url != null && url.contains("code=")) {
                    val code = extractCode(url)
                    if (code != null) {
                        webView.visibility = View.GONE
                        exchangeCodeForToken(code)
                    } else {
                        Toast.makeText(this@OAuthActivity, "Gagal membaca kode OAuth", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            }
        }

        val authUrl = "https://github.com/login/oauth/authorize" +
                "?client_id=$OAUTH_CLIENT_ID" +
                "&scope=$OAUTH_SCOPE"
        webView.loadUrl(authUrl)
    }

    private fun extractCode(url: String): String? {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.getQueryParameter("code")
        } catch (_: Exception) { null }
    }

    private fun exchangeCodeForToken(code: String) {
        tvStatus.text = "Menukarkan kode dengan token..."
        tvStatus.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val accessToken = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()

                    val params = mapOf(
                        "client_id" to OAUTH_CLIENT_ID,
                        "code" to code,
                        "scope" to OAUTH_SCOPE
                    ).entries.joinToString("&") { "${it.key}=${it.value}" }

                    val request = Request.Builder()
                        .url("https://github.com/login/oauth/access_token?$params")
                        .header("Accept", "application/json")
                        .header("User-Agent", "GistApp-Android")
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    val json = JSONObject(response.body?.string() ?: "{}")

                    if (json.has("error")) {
                        throw Exception(json.optString("error_description",
                            json.optString("error", "OAuth gagal")))
                    }

                    val token = json.optString("access_token", "")
                    if (token.isEmpty()) throw Exception("Token kosong — cek client_id")
                    token
                }

                tokenManager.saveToken(accessToken)
                Toast.makeText(this@OAuthActivity, "Login berhasil!", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                tvStatus.text = "Gagal: ${e.message}"
                webView.visibility = View.VISIBLE
                Toast.makeText(this@OAuthActivity, "OAuth gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
