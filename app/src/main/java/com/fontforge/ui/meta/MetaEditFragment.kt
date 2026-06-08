package com.fontforge.ui.meta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fontforge.data.FontRepository
import com.fontforge.databinding.FragmentMetaEditBinding
import com.fontforge.databinding.ItemEditFieldBinding

class MetaEditFragment : Fragment() {

    private var _binding: FragmentMetaEditBinding? = null
    private val binding get() = _binding!!
    private val args: MetaEditFragmentArgs by navArgs()

    private val fieldKeys = listOf(
        "family" to "Family",
        "subfamily" to "Subfamily",
        "fullName" to "Full Name",
        "version" to "Version",
        "postscript" to "PostScript",
        "manufacturer" to "Manufacturer",
        "designer" to "Designer",
        "description" to "Description",
        "trademark" to "Trademark",
        "license" to "License",
        "licenseURL" to "License URL",
        "vendorURL" to "Vendor URL",
        "designerURL" to "Designer URL",
        "sampleText" to "Sample Text"
    )

    private val fieldBindings = mutableListOf<Pair<String, ItemEditFieldBinding>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMetaEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val font = FontRepository.getById(args.fontId) ?: run { findNavController().popBackStack(); return }
        val m = font.effectiveMeta

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val currentValues = mapOf(
            "family" to m.family, "subfamily" to m.subfamily, "fullName" to m.fullName,
            "version" to m.version, "postscript" to m.postscript,
            "manufacturer" to m.manufacturer, "designer" to m.designer,
            "description" to m.description, "trademark" to m.trademark,
            "license" to m.license, "licenseURL" to m.licenseURL,
            "vendorURL" to m.vendorURL, "designerURL" to m.designerURL,
            "sampleText" to m.sampleText
        )

        val inflater = LayoutInflater.from(requireContext())
        fieldKeys.forEach { (key, label) ->
            val fb = ItemEditFieldBinding.inflate(inflater, binding.formContainer, false)
            fb.tvFieldLabel.text = label
            fb.etFieldValue.setText(font.metaOverrides[key] ?: currentValues[key] ?: "")
            fb.etFieldValue.hint = currentValues[key] ?: ""
            binding.formContainer.addView(fb.root)
            fieldBindings.add(key to fb)
        }

        binding.btnSave.setOnClickListener {
            val overrides = mutableMapOf<String, String>()
            fieldBindings.forEach { (key, fb) ->
                val text = fb.etFieldValue.text?.toString() ?: ""
                if (text.isNotBlank()) overrides[key] = text
            }
            FontRepository.saveMetaOverrides(font.id, overrides, requireContext())
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
