package com.gistapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gistapp.R
import com.gistapp.data.remote.RetrofitClient
import com.gistapp.data.repository.GistRepository
import com.gistapp.databinding.ActivityAuthBinding
import com.gistapp.ui.MainActivity
import com.gistapp.util.TokenManager
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        if (tokenManager.hasToken()) {
            navigateToMain()
            return
        }

        // OAuth button → buka WebView login
        binding.cardOAuth.setOnClickListener {
            startActivity(Intent(this, OAuthActivity::class.java))
        }

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
                Toast.makeText(this@AuthActivity,
                    "Login berhasil! Selamat datang, ${user.login}", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }.onFailure { error ->
                tokenManager.clearToken()
                Toast.makeText(this@AuthActivity,
                    "Login gagal: ${error.message}", Toast.LENGTH_LONG).show()
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
