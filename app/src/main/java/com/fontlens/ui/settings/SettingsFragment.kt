package com.fontlens.ui.settings

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.fontlens.R
import com.fontlens.data.AppSettings
import com.fontlens.data.ColorTheme
import com.fontlens.data.FontRepository
import com.fontlens.data.ThemeMode
import com.fontlens.databinding.FragmentSettingsBinding
import com.fontlens.databinding.ItemLangSettingBinding
import com.fontlens.utils.ThemeManager
import com.google.android.material.textfield.TextInputEditText

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
        renderShowFullNameSwitch(s)
        renderGlyphSwitch(s)
        renderPreferMetaSwitch(s)
        renderLangSpinner(s)
        renderLangList(s)
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

        // Two chips per row
        val rows = options.chunked(2)
        rows.forEach { rowOptions ->
            val row = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            rowOptions.forEach { opt ->
                val isSelected = opt.theme == s.colorTheme
                val chipView = LayoutInflater.from(requireContext())
                    .inflate(android.R.layout.simple_list_item_1, row, false) as TextView

                // Chip background
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

                // Color circle drawable
                val circleSize = (22f * density).toInt()
                val circle = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(opt.color)
                    setSize(circleSize, circleSize)
                }

                // Checkmark for selected
                val checkSize = (18f * density).toInt()
                val checkDrawable = if (isSelected) {
                    GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(opt.color)
                        setSize(checkSize, checkSize)
                    }
                } else null

                chipView.text = opt.label
                chipView.textSize = 13f
                chipView.setTextColor(if (isSelected) opt.color else p.textPrimary)
                chipView.setCompoundDrawablesWithIntrinsicBounds(circle, null, checkDrawable, null)
                chipView.compoundDrawablePadding = (8f * density).toInt()

                val padH = (14f * density).toInt()
                val padV = (12f * density).toInt()
                chipView.setPadding(padH, padV, padH, padV)

                val lp = android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                lp.marginEnd = if (rowOptions.last() != opt) (6f * density).toInt() else 0
                lp.bottomMargin = (6f * density).toInt()
                chipView.layoutParams = lp

                chipView.setOnClickListener {
                    if (opt.theme == FontRepository.settings.colorTheme) return@setOnClickListener
                    FontRepository.settings = FontRepository.settings.copy(colorTheme = opt.theme)
                    FontRepository.saveSettings(requireContext())
                    restartActivity()
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

        val idx = when (s.themeMode) {
            ThemeMode.SYSTEM -> 0
            ThemeMode.DAY    -> 1
            ThemeMode.NIGHT  -> 2
        }
        binding.spinnerThemeMode.setSelection(idx)

        var ready = false
        binding.spinnerThemeMode.post { ready = true }

        binding.spinnerThemeMode.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (!ready) return
                    val mode = when (pos) { 0 -> ThemeMode.SYSTEM; 1 -> ThemeMode.DAY; else -> ThemeMode.NIGHT }
                    if (mode == FontRepository.settings.themeMode) return
                    // Bug fix: preserve language settings — only change themeMode
                    FontRepository.settings = FontRepository.settings.copy(themeMode = mode)
                    FontRepository.saveSettings(requireContext())
                    restartActivity()
                }
                override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
            }
    }

    // ── Show full font name ───────────────────────────────────────────────

    private fun renderShowFullNameSwitch(s: AppSettings) {
        binding.switchShowFullName.isChecked = s.showFullFontName
        binding.switchShowFullName.setOnCheckedChangeListener { _, checked ->
            FontRepository.settings = FontRepository.settings.copy(showFullFontName = checked)
            FontRepository.saveSettings(requireContext())
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

    // ── Prefer meta sample switch ─────────────────────────────────────────

    private fun renderPreferMetaSwitch(s: AppSettings) {
        binding.switchPreferMeta.isChecked = s.preferMetaSample
        binding.switchPreferMeta.setOnCheckedChangeListener { _, checked ->
            FontRepository.settings = FontRepository.settings.copy(preferMetaSample = checked)
            FontRepository.saveSettings(requireContext())
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

    // ── Language spinner ──────────────────────────────────────────────────

    private fun renderLangSpinner(s: AppSettings) {
        val langs = s.langSamples.keys.toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, langs)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLang.adapter = adapter
        val idx = langs.indexOf(s.defaultLang).coerceAtLeast(0)
        binding.spinnerLang.setSelection(idx)

        var ready = false
        binding.spinnerLang.post { ready = true }

        binding.spinnerLang.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (!ready) return
                FontRepository.settings = FontRepository.settings.copy(defaultLang = langs[pos])
                FontRepository.saveSettings(requireContext())
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    // ── Language list ─────────────────────────────────────────────────────

    private fun renderLangList(s: AppSettings) {
        binding.langListContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        s.langSamples.forEach { (lang, text) ->
            val lb = ItemLangSettingBinding.inflate(inflater, binding.langListContainer, false)
            lb.tvLangName.text = lang
            lb.etLangText.setText(text)
            lb.etLangText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val updated = FontRepository.settings.langSamples.toMutableMap()
                    updated[lang] = lb.etLangText.text?.toString() ?: ""
                    FontRepository.settings = FontRepository.settings.copy(langSamples = updated)
                    FontRepository.saveSettings(requireContext())
                }
            }
            lb.btnRemove.setOnClickListener {
                val current = FontRepository.settings.langSamples
                if (current.size <= 1) {
                    Toast.makeText(requireContext(), "At least one sample text must remain.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val updated = current.toMutableMap()
                updated.remove(lang)
                // If deleted lang was the default, reset to first remaining
                val newDefault = if (FontRepository.settings.defaultLang == lang)
                    updated.keys.first() else FontRepository.settings.defaultLang
                FontRepository.settings = FontRepository.settings.copy(
                    langSamples = updated, defaultLang = newDefault)
                FontRepository.saveSettings(requireContext())
                renderLangList(FontRepository.settings)
                renderLangSpinner(FontRepository.settings)
            }
            binding.langListContainer.addView(lb.root)
        }
        binding.btnAddLang.setOnClickListener { showAddLangDialog() }
    }

    // ── Add language dialog ───────────────────────────────────────────────

    private fun showAddLangDialog() {
        val themed = ContextThemeWrapper(requireContext(), ThemeManager.currentThemeResId(requireContext()))
        val dialogView = LayoutInflater.from(themed).inflate(R.layout.dialog_add_lang, null)

        val etName   = dialogView.findViewById<TextInputEditText>(R.id.et_lang_name)
        val etSample = dialogView.findViewById<TextInputEditText>(R.id.et_lang_sample)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_dialog_cancel)
        val btnSave   = dialogView.findViewById<TextView>(R.id.btn_dialog_save)

        val dialog = Dialog(themed, R.style.Theme_FontLens_Dialog)
        dialog.setContentView(dialogView)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (requireContext().resources.displayMetrics.widthPixels * 0.92f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val name = etName.text?.toString()?.trim() ?: ""
            val text = etSample.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) {
                etName.error = "Language name required"
                return@setOnClickListener
            }
            val updated = FontRepository.settings.langSamples.toMutableMap()
            updated[name] = text
            FontRepository.settings = FontRepository.settings.copy(langSamples = updated)
            FontRepository.saveSettings(requireContext())
            renderLangList(FontRepository.settings)
            renderLangSpinner(FontRepository.settings)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun restartActivity() { requireActivity().recreate() }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
