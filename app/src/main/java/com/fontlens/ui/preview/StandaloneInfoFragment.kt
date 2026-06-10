package com.fontlens.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.fontlens.R
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentFontInfoBinding
import com.fontlens.databinding.ItemInfoRowBinding

class StandaloneInfoFragment : Fragment() {

    private var _binding: FragmentFontInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFontInfoBinding.inflate(inflater, container, false)
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

        fun bindBool(row: ItemInfoRowBinding, label: String, value: Boolean) {
            row.tvInfoKey.text   = label
            row.tvInfoValue.text = if (value) getString(R.string.yes) else getString(R.string.no_str)
            row.tvInfoValue.setTextColor(ContextCompat.getColor(requireContext(),
                if (value) R.color.success else R.color.error_red))
        }
        fun bindText(row: ItemInfoRowBinding, label: String, value: String) {
            row.tvInfoKey.text   = label
            row.tvInfoValue.text = value
        }

        bindBool(binding.rowBold,      getString(R.string.bold_support),      m.boldSupport)
        bindBool(binding.rowItalic,    getString(R.string.italic_support),    m.italicSupport)
        bindBool(binding.rowCondensed, getString(R.string.condensed_support), m.condensedSupport)
        bindBool(binding.rowExtended,  getString(R.string.extended_support),  m.extendedSupport)
        bindBool(binding.rowMono,      getString(R.string.monospaced),        m.isFixedPitch)
        bindText(binding.rowGlyphs,      getString(R.string.num_glyphs),     "%,d".format(m.numGlyphs))
        bindText(binding.rowWeight,      getString(R.string.weight_class),   "${m.weight} (${m.weightName})")
        bindText(binding.rowUpm,         getString(R.string.units_per_em),   m.unitsPerEm.toString())
        bindText(binding.rowItalicAngle, getString(R.string.italic_angle),   "%.2f°".format(m.italicAngle))
        bindText(binding.rowChars,       getString(R.string.supported_chars),"%,d".format(m.supportedChars.size))
        bindText(binding.rowTables,      getString(R.string.font_tables),    m.tables.joinToString(", "))
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
