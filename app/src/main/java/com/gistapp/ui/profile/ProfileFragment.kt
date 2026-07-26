package com.gistapp.ui.profile

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gistapp.data.model.GitHubRepo
import com.gistapp.data.remote.GitHubUserResponse
import com.gistapp.data.remote.RetrofitClient
import com.gistapp.data.repository.GistRepository
import com.gistapp.databinding.FragmentProfileBinding
import com.gistapp.databinding.ItemRepoBinding
import com.gistapp.ui.auth.AuthActivity
import com.gistapp.util.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: GistRepository
    private lateinit var tokenManager: TokenManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())
        val apiService = RetrofitClient.getApiService(tokenManager)
        repository = GistRepository(apiService)

        if (!tokenManager.hasToken()) {
            binding.layoutLoggedOut.visibility = View.VISIBLE
            binding.layoutProfile.visibility = View.GONE
            binding.btnGoLogin.setOnClickListener {
                startActivity(Intent(requireContext(), AuthActivity::class.java))
                requireActivity().finish()
            }
            return
        }

        binding.layoutLoggedOut.visibility = View.GONE
        binding.layoutProfile.visibility = View.VISIBLE

        binding.swipeRefresh.setOnRefreshListener { loadProfile() }

        // Hanya izinkan swipe refresh saat scroll di posisi paling atas
        binding.layoutProfile.setOnScrollChangeListener(
            androidx.core.widget.NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                binding.swipeRefresh.isEnabled = scrollY == 0
            }
        )

        loadProfile()
    }

    private fun loadProfile() {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            // Jalankan 3 request paralel
            val userResult = repository.verifyToken()
            val reposResult = repository.getMyRepos()
            val gistsResult = repository.getMyGists(page = 1)

            binding.progressBar.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false

            userResult.onSuccess { user -> displayUser(user) }
                .onFailure {
                    // Jangan redirect ke login, hanya tampilkan error
                    Toast.makeText(requireContext(), "Gagal refresh: ${it.message}", Toast.LENGTH_SHORT).show()
                }

            reposResult.onSuccess { repos -> displayRepos(repos) }
                .onFailure { binding.tvRepoCount.text = "Gagal load repos" }

            // Hitung gists real (public + secret)
            gistsResult.onSuccess { gists ->
                binding.tvGistCount.text = "${gists.size}"
            }.onFailure {
                // fallback ke publicGists dari user API
                binding.tvGistCount.text = "${userResult.getOrNull()?.publicGists ?: 0}"
            }
        }
    }

    private fun displayUser(user: GitHubUserResponse) {
        binding.tvUsername.text = "@${user.login ?: "?"}"
        binding.tvName.text = user.name ?: user.login ?: "Unknown"
        binding.tvBio.text = user.bio

        if (user.bio.isNullOrBlank()) binding.tvBio.visibility = View.GONE
        else binding.tvBio.visibility = View.VISIBLE

        // Detail rows: hide if empty
        setDetailRow(binding.rowCompany, binding.tvCompany, user.company)
        setDetailRow(binding.rowLocation, binding.tvLocation, user.location)
        setDetailRow(binding.rowBlog, binding.tvBlog, user.blog)

        binding.tvGistCount.text = "..."
        binding.tvRepoCountValue.text = "${user.publicRepos ?: 0}"
        binding.tvFollowers.text = "${user.followers ?: 0}"
        binding.tvFollowing.text = "${user.following ?: 0}"

        // Load avatar
        user.avatarUrl?.let { loadAvatar(it, binding.ivAvatar) }

        binding.cardUserInfo.setOnClickListener {
            user.login?.let { login ->
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/$login")))
            }
        }
    }

    private fun setDetailRow(row: View, tv: android.widget.TextView, value: String?) {
        if (value.isNullOrBlank()) {
            row.visibility = View.GONE
        } else {
            row.visibility = View.VISIBLE
            tv.text = value
        }
    }

    private fun loadAvatar(url: String, imageView: ImageView) {
        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build()
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "GistApp-Android")
                        .build()
                    val response = client.newCall(request).execute()
                    response.body?.byteStream()?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (_: Exception) {
                // Keep placeholder
            }
        }
    }

    private fun displayRepos(repos: List<GitHubRepo>) {
        val container = binding.reposContainer
        container.removeAllViews()

        if (repos.isEmpty()) {
            binding.tvRepoCount.text = "Tidak ada repository"
            return
        }

        binding.tvRepoCountValue.text = "${repos.size}"
        binding.tvRepoCount.text = "${repos.size} repository"

        val maxShow = minOf(repos.size, 20)
        for (i in 0 until maxShow) {
            val repo = repos[i]
            val itemBinding = ItemRepoBinding.inflate(
                LayoutInflater.from(requireContext()), container, false
            )

            itemBinding.tvRepoName.text = repo.name ?: "-"
            itemBinding.tvRepoDesc.text = repo.description ?: "Tidak ada deskripsi"
            itemBinding.tvRepoLang.text = repo.language ?: "-"
            if (repo.language.isNullOrBlank()) {
                itemBinding.tvRepoLang.visibility = View.GONE
            }
            itemBinding.tvRepoStars.text = "⭐ ${repo.stars ?: 0}"
            itemBinding.tvRepoForks.text = "🍴 ${repo.forks ?: 0}"
            itemBinding.tvRepoVisibility.text = if (repo.isPrivate) "🔒" else "🌐"

            itemBinding.root.setOnClickListener {
                repo.htmlUrl?.let { url ->
                    startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                }
            }

            container.addView(itemBinding.root)
        }

        if (repos.size > maxShow) {
            binding.tvRepoCount.text = "${repos.size} repository (menampilkan $maxShow)"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
