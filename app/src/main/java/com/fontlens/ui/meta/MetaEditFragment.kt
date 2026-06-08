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
        val savedOverrides = FontRepository.getMetaOverrides(font.id) // previously saved edits

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
            // Show saved override if exists, otherwise show original parsed value
            val currentValue = savedOverrides[key] ?: originalValues[key] ?: ""
            fb.etFieldValue.setText(currentValue)
            fb.etFieldValue.hint = originalValues[key]?.ifEmpty { "—" } ?: "—"
            binding.formContainer.addView(fb.root)
            fieldBindings.add(key to fb)
        }

        binding.btnSave.setOnClickListener {
            val overrides = mutableMapOf<String, String>()
            fieldBindings.forEach { (key, fb) ->
                val text = fb.etFieldValue.text?.toString() ?: ""
                // Only save if different from original (no point overriding with same value)
                if (text.isNotBlank() && text != (originalValues[key] ?: "")) {
                    overrides[key] = text
                }
            }
            FontRepository.saveMetaOverrides(font.id, overrides, requireContext())
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
