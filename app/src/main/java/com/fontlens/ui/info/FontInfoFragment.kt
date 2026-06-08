package com.fontlens.ui.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.fontlens.R
import com.fontlens.data.FontRepository
import com.fontlens.databinding.FragmentFontInfoBinding
import com.fontlens.databinding.ItemInfoRowBinding

class FontInfoFragment : Fragment() {

    private var _binding: FragmentFontInfoBinding? = null
    private val binding get() = _binding!!
    private val args: FontInfoFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFontInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val font = FontRepository.getById(args.fontId) ?: run { findNavController().popBackStack(); return }
        val m = font.effectiveMeta

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        fun bindBool(rowBinding: ItemInfoRowBinding, label: String, value: Boolean) {
            rowBinding.tvInfoKey.text = label
            rowBinding.tvInfoValue.text = if (value) getString(R.string.yes) else getString(R.string.no_str)
            rowBinding.tvInfoValue.setTextColor(
                ContextCompat.getColor(requireContext(), if (value) R.color.success else R.color.error_red)
            )
        }

        fun bindText(rowBinding: ItemInfoRowBinding, label: String, value: String) {
            rowBinding.tvInfoKey.text   = label
            rowBinding.tvInfoValue.text = value
        }

        bindBool(binding.rowBold,      getString(R.string.bold_support),      m.boldSupport)
        bindBool(binding.rowItalic,    getString(R.string.italic_support),    m.italicSupport)
        bindBool(binding.rowCondensed, getString(R.string.condensed_support), m.condensedSupport)
        bindBool(binding.rowExtended,  getString(R.string.extended_support),  m.extendedSupport)
        bindBool(binding.rowMono,      getString(R.string.monospaced),        m.isFixedPitch)

        bindText(binding.rowGlyphs,     getString(R.string.num_glyphs),     "%,d".format(m.numGlyphs))
        bindText(binding.rowWeight,     getString(R.string.weight_class),   "${m.weight} (${m.weightName})")
        bindText(binding.rowUpm,        getString(R.string.units_per_em),   m.unitsPerEm.toString())
        bindText(binding.rowItalicAngle,getString(R.string.italic_angle),   "%.2f°".format(m.italicAngle))
        bindText(binding.rowChars,      getString(R.string.supported_chars),"%,d".format(m.supportedChars.size))
        bindText(binding.rowTables,     getString(R.string.font_tables),    m.tables.joinToString(", "))
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
