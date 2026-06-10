package com.fontlens.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentMetadataBinding
import com.fontlens.ui.meta.MetaAdapter

class StandaloneMetaFragment : Fragment() {

    private var _binding: FragmentMetadataBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMetadataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fontId = arguments?.getString("fontId") ?: return
        val font   = FontRepository.getById(fontId) ?: return
        val m      = font.effectiveMeta

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        // Hide edit button — meta editing not available in standalone mode
        binding.btnEdit.visibility = View.GONE

        val fields = listOf(
            "Family"       to m.family,
            "Subfamily"    to m.subfamily,
            "Full Name"    to m.fullName,
            "Version"      to m.version,
            "PostScript"   to m.postscript,
            "Manufacturer" to m.manufacturer,
            "Designer"     to m.designer,
            "Description"  to m.description,
            "Trademark"    to m.trademark,
            "License"      to m.license,
            "License URL"  to m.licenseURL,
            "Vendor URL"   to m.vendorURL,
            "Designer URL" to m.designerURL,
            "Sample Text"  to m.sampleText
        ).filter { it.second.isNotBlank() }

        if (fields.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvMeta.visibility  = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvMeta.visibility  = View.VISIBLE
            binding.rvMeta.layoutManager = LinearLayoutManager(requireContext())
            binding.rvMeta.adapter = MetaAdapter(fields)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
