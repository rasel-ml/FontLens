package com.fontlens.ui.settings

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fontlens.R
import com.fontlens.data.AppSettings
import com.fontlens.data.ColorTheme
import com.fontlens.data.FontRepository
import com.fontlens.data.ThemeMode
import com.fontlens.databinding.FragmentSettingsBinding
import com.fontlens.utils.ThemeManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderAll()
    }

    private fun renderAll() {
        val s = FontRepository.settings
        renderColorChips(s)
        renderThemeModeSpinner(s)
        renderGlyphSwitch(s)
        renderBuiltinSampleSwitch(s)
        renderEditSamplesRow()
        renderRecursiveSwitch(s)
    }

    // ── Color chips ───────────────────────────────────────────────────────

    private fun renderColorChips(s: AppSettings) {
        val container = binding.colorChipContainer
        container.removeAllViews()
        val p = ThemeManager.activePalette

        data class ChipOption(val theme: ColorTheme, val label: String, val color: Int)
        val options = listOf(
            ChipOption(ColorTheme.GREEN,  "Green",  ThemeManager.bulletColor(ColorTheme.GREEN)),
            ChipOption(ColorTheme.BLUE,   "Blue",   ThemeManager.bulletColor(ColorTheme.BLUE)),
            ChipOption(ColorTheme.RED,    "Red",    ThemeManager.bulletColor(ColorTheme.RED)),
            ChipOption(ColorTheme.YELLOW, "Yellow", ThemeManager.bulletColor(ColorTheme.YELLOW))
        )
        val density = requireContext().resources.displayMetrics.density

        options.chunked(2).forEach { rowOptions ->
            val row = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            rowOptions.forEach { opt ->
                val isSelected = opt.theme == s.colorTheme
                val chipView = LayoutInflater.from(requireContext())
                    .inflate(android.R.layout.simple_list_item_1, row, false) as TextView

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 10f * density
                    if (isSelected) {
                        setColor(android.graphics.Color.argb(30,
                            android.graphics.Color.red(opt.color),
                            android.graphics.Color.green(opt.color),
                            android.graphics.Color.blue(opt.color)))
                        setStroke((2f * density).toInt(), opt.color)
                    } else {
                        setColor(p.bgElevated)
                        setStroke((1f * density).toInt(), p.border)
                    }
                }
                chipView.background = bg

                val circleSize = (22f * density).toInt()
                val circle = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(opt.color); setSize(circleSize, circleSize)
                }
                val checkSize = (18f * density).toInt()
                val checkDrawable = if (isSelected) GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(opt.color); setSize(checkSize, checkSize)
                } else null

                chipView.text = opt.label
                chipView.textSize = 13f
                chipView.setTextColor(if (isSelected) opt.color else p.textPrimary)
                chipView.setCompoundDrawablesWithIntrinsicBounds(circle, null, checkDrawable, null)
                chipView.compoundDrawablePadding = (8f * density).toInt()
                val padH = (14f * density).toInt(); val padV = (12f * density).toInt()
                chipView.setPadding(padH, padV, padH, padV)

                val lp = android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                lp.marginEnd = if (rowOptions.last() != opt) (6f * density).toInt() else 0
                lp.bottomMargin = (6f * density).toInt()
                chipView.layoutParams = lp
                chipView.setOnClickListener {
                    if (opt.theme == FontRepository.settings.colorTheme) return@setOnClickListener
                    FontRepository.settings = FontRepository.settings.copy(colorTheme = opt.theme)
                    FontRepository.saveSettings(requireContext())
                    requireActivity().recreate()
                }
                row.addView(chipView)
            }
            container.addView(row)
        }
    }

    // ── Theme mode spinner ────────────────────────────────────────────────

    private fun renderThemeModeSpinner(s: AppSettings) {
        val modes = listOf("System Default", "Day Mode", "Night Mode")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerThemeMode.adapter = adapter
        binding.spinnerThemeMode.setSelection(when (s.themeMode) {
            ThemeMode.SYSTEM -> 0; ThemeMode.DAY -> 1; ThemeMode.NIGHT -> 2
        })
        var ready = false
        binding.spinnerThemeMode.post { ready = true }
        binding.spinnerThemeMode.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (!ready) return
                    val mode = when (pos) { 0 -> ThemeMode.SYSTEM; 1 -> ThemeMode.DAY; else -> ThemeMode.NIGHT }
                    if (mode == FontRepository.settings.themeMode) return
                    FontRepository.settings = FontRepository.settings.copy(themeMode = mode)
                    FontRepository.saveSettings(requireContext())
                    requireActivity().recreate()
                }
                override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
            }
    }

    // ── Glyph switch ──────────────────────────────────────────────────────

    private fun renderGlyphSwitch(s: AppSettings) {
        binding.switchGlyphAll.isChecked = s.glyphShowAll
        binding.switchGlyphAll.setOnCheckedChangeListener { _, checked ->
            FontRepository.settings = FontRepository.settings.copy(glyphShowAll = checked)
            FontRepository.saveSettings(requireContext())
        }
    }

    // ── Built-in sample text toggle ───────────────────────────────────────

    private fun renderBuiltinSampleSwitch(s: AppSettings) {
        binding.switchPreferMeta.isChecked = s.preferMetaSample
        updateBuiltinSampleDesc(s.preferMetaSample)
        binding.switchPreferMeta.setOnCheckedChangeListener { _, checked ->
            FontRepository.settings = FontRepository.settings.copy(preferMetaSample = checked)
            FontRepository.saveSettings(requireContext())
            updateBuiltinSampleDesc(checked)
        }
    }

    private fun updateBuiltinSampleDesc(on: Boolean) {
        binding.tvBuiltinSampleDesc.text = if (on)
            "ON: font's embedded sample text is shown when available"
        else
            "OFF: custom sample texts from your priority list are used"
    }

    // ── Edit Sample Texts navigation row ─────────────────────────────────

    private fun renderEditSamplesRow() {
        binding.rowEditSamples.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_sampleManager)
        }
    }

    // ── Recursive switch ──────────────────────────────────────────────────

    private fun renderRecursiveSwitch(s: AppSettings) {
        binding.switchRecursive.isChecked = s.folderRecursive
        binding.switchRecursive.setOnCheckedChangeListener { _, checked ->
            FontRepository.settings = FontRepository.settings.copy(folderRecursive = checked)
            FontRepository.saveSettings(requireContext())
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
