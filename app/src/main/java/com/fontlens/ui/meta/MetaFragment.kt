package com.fontlens.ui.meta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fontlens.R
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentMetadataBinding

class MetaFragment : Fragment() {

    private var _binding: FragmentMetadataBinding? = null
    private val binding get() = _binding!!
    private val args: MetaFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMetadataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val font = FontRepository.getById(args.fontId)
            ?: run { findNavController().popBackStack(); return }
        val m = font.effectiveMeta

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.btnEdit.setOnClickListener {
            findNavController().navigate(
                R.id.action_meta_to_edit,
                Bundle().apply { putString("fontId", font.id) }
            )
        }

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
            binding.rvMeta.layoutManager =
                androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            binding.rvMeta.adapter = MetaAdapter(fields)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
