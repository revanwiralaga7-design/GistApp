package com.gistapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gistapp.R
import com.gistapp.data.model.GitHubRepo
import com.gistapp.data.remote.GitHubApiService
import com.gistapp.data.remote.GitHubUserResponse
import com.gistapp.data.remote.RetrofitClient
import com.gistapp.data.repository.GistRepository
import com.gistapp.databinding.FragmentProfileBinding
import com.gistapp.databinding.ItemRepoBinding
import com.gistapp.ui.auth.AuthActivity
import com.gistapp.util.TokenManager
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: GistRepository
    private lateinit var tokenManager: TokenManager
    private var userLogin: String? = null

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
        loadProfile()
    }

    private fun loadProfile() {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            // Load user info
            val userResult = repository.verifyToken()
            val reposResult = repository.getMyRepos()

            binding.progressBar.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false

            userResult.onSuccess { user -> displayUser(user) }
                .onFailure { Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show() }

            reposResult.onSuccess { repos -> displayRepos(repos) }
                .onFailure { binding.tvRepoCount.text = "Gagal load repos" }
        }
    }

    private fun displayUser(user: GitHubUserResponse) {
        userLogin = user.login
        binding.tvUsername.text = "@${user.login ?: "?"}"
        binding.tvName.text = user.name ?: user.login ?: "-"
        binding.tvBio.text = user.bio ?: "Tidak ada bio"
        if (user.bio.isNullOrEmpty()) binding.tvBio.visibility = View.GONE
        else binding.tvBio.visibility = View.VISIBLE

        binding.tvLocation.text = user.location ?: "-"
        binding.tvCompany.text = user.company ?: "-"
        binding.tvBlog.text = user.blog ?: "-"

        binding.tvGistCount.text = "${user.publicGists ?: 0}"
        binding.tvRepoCountValue.text = "${user.publicRepos ?: 0}"
        binding.tvFollowers.text = "${user.followers ?: 0}"
        binding.tvFollowing.text = "${user.following ?: 0}"

        // Buka profil di browser
        binding.cardUserInfo.setOnClickListener {
            user.login?.let { login ->
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/$login"))
                startActivity(intent)
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

        binding.tvRepoCount.text = "${repos.size} repository"

        val maxShow = minOf(repos.size, 20) // max 20 biar gak berat
        for (i in 0 until maxShow) {
            val repo = repos[i]
            val itemBinding = ItemRepoBinding.inflate(
                LayoutInflater.from(requireContext()), container, false
            )

            itemBinding.tvRepoName.text = repo.name ?: "-"
            itemBinding.tvRepoDesc.text = repo.description ?: "Tidak ada deskripsi"
            itemBinding.tvRepoLang.text = repo.language ?: "-"
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
