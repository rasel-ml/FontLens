package com.fontlens.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fontlens.R
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentFontListBinding

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFontListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FontListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFontListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = getString(R.string.favorites)
        binding.fabAdd.visibility = View.GONE
        binding.searchLayout.visibility = View.GONE

        adapter = FontListAdapter(
            onFontClick = { font ->
                val action = FavoritesFragmentDirections.actionFavToPreview(font.id)
                findNavController().navigate(action)
            },
            onFavoriteClick = { font ->
                FontRepository.toggleFavorite(font.id, requireContext())
                refresh()
            },
            isFavorite = { FontRepository.isFavorite(it) },
            getSample = { FontRepository.getSampleText(it) }
        )
        binding.rvFonts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFonts.adapter = adapter
        refresh()
    }

    private fun refresh() {
        val favs = FontRepository.getFavorites()
        binding.tvCount.text = favs.size.toString()
        binding.tvEmpty.text = getString(R.string.no_favorites)
        binding.layoutEmpty.visibility = if (favs.isEmpty()) View.VISIBLE else View.GONE
        binding.rvFonts.visibility = if (favs.isEmpty()) View.GONE else View.VISIBLE
        adapter.submitList(favs)
    }

    override fun onResume() { super.onResume(); refresh() }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
