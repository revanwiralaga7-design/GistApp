package com.gistapp.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gistapp.R
import com.gistapp.ui.MainActivity
import com.gistapp.util.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

class OAuthActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tokenManager: TokenManager

    companion object {
        const val OAUTH_CLIENT_ID = "Ov23li..." // GANTI dengan Client ID kamu
        const val OAUTH_SCOPE = "gist,user,repo"
        const val REDIRECT_URI = "gistapp://oauth"
        const val PKCE_PREF_KEY = "pkce_verifier"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oauth)

        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        tokenManager = TokenManager(this)

        val data = intent.data
        if (data != null && data.toString().startsWith(REDIRECT_URI)) {
            val code = data.getQueryParameter("code")
            val error = data.getQueryParameter("error")
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, "OAuth dibatalkan: $error", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            if (!code.isNullOrEmpty()) {
                tvStatus.text = "Menukarkan kode dengan token..."
                tvStatus.visibility = android.view.View.VISIBLE
                progressBar.visibility = android.view.View.VISIBLE
                exchangeCodeForToken(code)
            } else {
                Toast.makeText(this, "Kode OAuth tidak ditemukan", Toast.LENGTH_LONG).show()
                finish()
            }
        } else {
            val codeVerifier = generateCodeVerifier()
            val prefs = getSharedPreferences("oauth_pkce", MODE_PRIVATE)
            prefs.edit().putString(PKCE_PREF_KEY, codeVerifier).apply()

            val codeChallenge = generateCodeChallenge(codeVerifier)
            val authUrl = "https://github.com/login/oauth/authorize" +
                    "?client_id=$OAUTH_CLIENT_ID" +
                    "&redirect_uri=$REDIRECT_URI" +
                    "&scope=$OAUTH_SCOPE" +
                    "&code_challenge=$codeChallenge" +
                    "&code_challenge_method=S256"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            startActivity(browserIntent)
            finish()
        }
    }

    private fun generateCodeVerifier(): String {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "")
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun exchangeCodeForToken(code: String) {
        lifecycleScope.launch {
            try {
                val accessToken = withContext(Dispatchers.IO) {
                    val prefs = getSharedPreferences("oauth_pkce", MODE_PRIVATE)
                    val codeVerifier = prefs.getString(PKCE_PREF_KEY, "") ?: ""
                    if (codeVerifier.isEmpty()) throw Exception("PKCE verifier tidak ditemukan")

                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()

                    val params = mutableListOf<String>()
                    params.add("client_id=$OAUTH_CLIENT_ID")
                    params.add("code=$code")
                    params.add("redirect_uri=$REDIRECT_URI")
                    params.add("code_verifier=$codeVerifier")
                    val queryString = params.joinToString("&")

                    val request = Request.Builder()
                        .url("https://github.com/login/oauth/access_token?$queryString")
                        .header("Accept", "application/json")
                        .header("User-Agent", "GistApp-Android")
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: "{}"
                    val json = JSONObject(bodyStr)

                    if (json.has("error")) {
                        throw Exception(json.optString("error_description",
                            json.optString("error", "OAuth gagal")))
                    }

                    val token = json.optString("access_token", "")
                    if (token.isEmpty()) throw Exception("Token kosong — cek client_id")
                    token
                }

                // Bersihkan verifier setelah berhasil
                getSharedPreferences("oauth_pkce", MODE_PRIVATE).edit().clear().apply()

                tokenManager.saveToken(accessToken)
                Toast.makeText(this@OAuthActivity, "Login berhasil!", Toast.LENGTH_SHORT).show()
                val mainIntent = Intent(this@OAuthActivity, MainActivity::class.java)
                mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(mainIntent)
                finish()
            } catch (e: Exception) {
                tvStatus.text = "Gagal: ${e.message}"
                progressBar.visibility = android.view.View.GONE
                Toast.makeText(this@OAuthActivity, "OAuth gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
