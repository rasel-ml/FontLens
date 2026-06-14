package com.fontlens.ui.meta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentMetaEditBinding
import com.fontlens.databinding.ItemEditFieldBinding

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.fontlens.utils.FontMetadataEditor


class MetaEditFragment : Fragment() {

    private var _binding: FragmentMetaEditBinding? = null
    private val binding get() = _binding!!
    private val args: MetaEditFragmentArgs by navArgs()

    private val fieldKeys = listOf(
        "family"       to "Family",
        "subfamily"    to "Subfamily",
        "fullName"     to "Full Name",
        "version"      to "Version",
        "postscript"   to "PostScript",
        "manufacturer" to "Manufacturer",
        "designer"     to "Designer",
        "description"  to "Description",
        "trademark"    to "Trademark",
        "license"      to "License",
        "licenseURL"   to "License URL",
        "vendorURL"    to "Vendor URL",
        "designerURL"  to "Designer URL",
        "sampleText"   to "Sample Text"
    )

    private val fieldBindings = mutableListOf<Pair<String, ItemEditFieldBinding>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMetaEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val font = FontRepository.getById(args.fontId)
            ?: run { findNavController().popBackStack(); return }

        val originalMeta = font.meta  // raw parsed values

        // Map of original values from parsed font
        val originalValues = mapOf(
            "family"       to originalMeta.family,
            "subfamily"    to originalMeta.subfamily,
            "fullName"     to originalMeta.fullName,
            "version"      to originalMeta.version,
            "postscript"   to originalMeta.postscript,
            "manufacturer" to originalMeta.manufacturer,
            "designer"     to originalMeta.designer,
            "description"  to originalMeta.description,
            "trademark"    to originalMeta.trademark,
            "license"      to originalMeta.license,
            "licenseURL"   to originalMeta.licenseURL,
            "vendorURL"    to originalMeta.vendorURL,
            "designerURL"  to originalMeta.designerURL,
            "sampleText"   to originalMeta.sampleText
        )

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val inflater = LayoutInflater.from(requireContext())
        fieldKeys.forEach { (key, label) ->
            val fb = ItemEditFieldBinding.inflate(inflater, binding.formContainer, false)
            fb.tvFieldLabel.text = label
            val currentValue = originalValues[key] ?: ""
            fb.etFieldValue.setText(currentValue)
            fb.etFieldValue.hint = currentValue.ifEmpty { "—" }
            binding.formContainer.addView(fb.root)
            fieldBindings.add(key to fb)
        }
        binding.btnSave.setOnClickListener {
            val updates = mutableMapOf<String, String>()
            fieldBindings.forEach { (key, fb) ->
                val text = fb.etFieldValue.text?.toString() ?: ""
                val original = originalValues[key] ?: ""
                if (text != original) {
                    updates[key] = text
                }
            }
        
            if (updates.isEmpty()) {
                findNavController().popBackStack()
                return@setOnClickListener
            }
        
            binding.btnSave.isEnabled = false
        
            viewLifecycleOwner.lifecycleScope.launch {
                val updatedMeta = withContext(Dispatchers.IO) {
                    FontMetadataEditor.writeMetadata(requireContext(), font.uri, updates)
                }
        
                if (updatedMeta != null) {
                    FontRepository.updateFontMeta(font.id, updatedMeta, requireContext())
                    findNavController().popBackStack()
                } else {
                    binding.btnSave.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
