package com.gistapp.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import java.util.concurrent.TimeUnit

class OAuthActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tokenManager: TokenManager

    companion object {
        // GANTI dengan Client ID aplikasi OAuth GitHub kamu
        const val OAUTH_CLIENT_ID = "Ov23li..."
        const val OAUTH_CLIENT_SECRET = "" // GANTI dengan Client Secret kamu
        const val OAUTH_SCOPE = "gist,user,repo"
        const val REDIRECT_URI = "gistapp://oauth"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oauth)

        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        tokenManager = TokenManager(this)

        // Cek apakah ini redirect balik dari browser
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
            // Buka browser untuk login OAuth
            val authUrl = "https://github.com/login/oauth/authorize" +
                    "?client_id=$OAUTH_CLIENT_ID" +
                    "&redirect_uri=$REDIRECT_URI" +
                    "&scope=$OAUTH_SCOPE"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            startActivity(browserIntent)
            finish()
        }
    }

    private fun exchangeCodeForToken(code: String) {
        lifecycleScope.launch {
            try {
                val accessToken = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()

                    val params = mutableListOf<String>()
                    params.add("client_id=$OAUTH_CLIENT_ID")
                    params.add("client_secret=$OAUTH_CLIENT_SECRET")
                    params.add("code=$code")
                    params.add("redirect_uri=$REDIRECT_URI")
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
                    if (token.isEmpty()) throw Exception("Token kosong — cek client_id / client_secret")
                    token
                }

                tokenManager.saveToken(accessToken)
                Toast.makeText(this@OAuthActivity, "Login berhasil!", Toast.LENGTH_SHORT).show()
                val mainIntent = Intent(this, MainActivity::class.java)
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
