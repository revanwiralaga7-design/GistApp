package com.gistapp.ui.gistlist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.gistapp.R
import com.gistapp.data.remote.RetrofitClient
import com.gistapp.data.repository.GistRepository
import com.gistapp.databinding.FragmentGistListBinding
import com.gistapp.ui.gistdetail.GistDetailActivity
import com.gistapp.util.TokenManager

/**
 * Fragment untuk menampilkan daftar gists (My Gists / Public Gists).
 */
class GistListFragment : Fragment() {

    private var _binding: FragmentGistListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: GistAdapter
    private lateinit var repository: GistRepository
    private lateinit var tokenManager: TokenManager
    private var isPublic: Boolean = false
    private var currentPage: Int = 1
    private var isLoading: Boolean = false

    companion object {
        private const val ARG_IS_PUBLIC = "is_public"

        fun newInstance(isPublic: Boolean): GistListFragment {
            return GistListFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_PUBLIC, isPublic)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isPublic = arguments?.getBoolean(ARG_IS_PUBLIC, false) ?: false
        tokenManager = TokenManager(requireContext())
        val apiService = RetrofitClient.getApiService(tokenManager)
        repository = GistRepository(apiService)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGistListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = GistAdapter { gist ->
            val intent = Intent(requireContext(), GistDetailActivity::class.java).apply {
                putExtra("gist_id", gist.id)
            }
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            currentPage = 1
            adapter.clear()
            loadGists()
        }

        // Load more saat scroll ke bawah (pagination)
        binding.recyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visibleItemCount + firstVisibleItem) >= totalItemCount && firstVisibleItem >= 0) {
                    currentPage++
                    loadGists()
                }
            }
        })

        // Cek login untuk tab My Gists
        if (!isPublic && !tokenManager.hasToken()) {
            binding.tvEmpty.text = "Silakan login untuk melihat gists Anda"
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            loadGists()
        }
    }

    private fun loadGists() {
        if (isLoading) return
        isLoading = true
        binding.progressBar.visibility = if (currentPage == 1) View.VISIBLE else View.GONE

        val call = if (isPublic) {
            repository.getPublicGists(currentPage)
        } else {
            repository.getMyGists(currentPage)
        }

        kotlinx.coroutines.MainScope().launch {
            val result = call
            binding.progressBar.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
            isLoading = false

            result.onSuccess { gists ->
                adapter.addGists(gists)
                if (adapter.itemCount == 0) {
                    binding.tvEmpty.text = if (isPublic) "Tidak ada public gists" else "Belum ada gists"
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }.onFailure { error ->
                if (currentPage == 1) {
                    binding.tvEmpty.text = "Error: ${error.message}"
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
                    currentPage--
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
