package com.gistapp.ui.gistlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gistapp.data.model.Gist
import com.gistapp.databinding.ItemGistBinding

/**
 * RecyclerView adapter untuk daftar Gist.
 */
class GistAdapter(
    private val onGistClick: (Gist) -> Unit
) : RecyclerView.Adapter<GistAdapter.GistViewHolder>() {

    private val gists = mutableListOf<Gist>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GistViewHolder {
        val binding = ItemGistBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GistViewHolder(binding, onGistClick)
    }

    override fun onBindViewHolder(holder: GistViewHolder, position: Int) {
        holder.bind(gists[position])
    }

    override fun getItemCount(): Int = gists.size

    fun addGists(newGists: List<Gist>) {
        val startPos = gists.size
        gists.addAll(newGists)
        notifyItemRangeInserted(startPos, newGists.size)
    }

    fun clear() {
        val count = gists.size
        gists.clear()
        notifyItemRangeRemoved(0, count)
    }

    class GistViewHolder(
        private val binding: ItemGistBinding,
        private val onGistClick: (Gist) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(gist: Gist) {
            // Ambil file pertama dari map
            val firstFile = gist.files.values.firstOrNull()

            binding.tvGistTitle.text = firstFile?.filename ?: "(tanpa nama)"
            binding.tvDescription.text = gist.description?.take(120) ?: "Tidak ada deskripsi"
            binding.tvLanguage.text = firstFile?.language ?: "Unknown"
            binding.tvVisibility.text = if (gist.isPublic) "🌐 Public" else "🔒 Secret"
            binding.tvFileCount.text = "${gist.files.size} file(s)"
            binding.tvOwner.text = gist.owner?.login ?: "anonymous"

            // Time
            binding.tvUpdatedAt.text = gist.updatedAt?.let { formatDate(it) } ?: ""

            binding.root.setOnClickListener {
                onGistClick(gist)
            }
        }

        private fun formatDate(isoDate: String): String {
            // Simple formatting: ambil 10 karakter pertama (yyyy-MM-dd) lalu ganti
            return try {
                isoDate.substring(0, 10).replace("T", " ")
            } catch (e: Exception) {
                isoDate
            }
        }
    }
}
