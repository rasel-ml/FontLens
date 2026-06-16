package com.fontlens.ui.settings

import android.app.AlertDialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.fontlens.R
import com.fontlens.data.AppSettings
import com.fontlens.data.ColorTheme
import com.fontlens.data.FontRepository
import com.fontlens.data.SamplePriority
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
        renderColorThemeSpinner(s)
        renderFollowSystemSwitch(s)
        renderDarkModeSwitch(s)
        renderPriority(s)
        renderGlyphSwitch(s)
        renderRecursiveSwitch(s)
        renderLangList(s)
        renderLangSpinner(s)
    }

    // ── Color theme spinner ───────────────────────────────────────────────

    private fun renderColorThemeSpinner(s: AppSettings) {
        data class ThemeOption(val theme: ColorTheme, val label: String, val color: Int)

        val options = listOf(
            ThemeOption(ColorTheme.GREEN,  "Green",  ThemeManager.bulletColor(ColorTheme.GREEN)),
            ThemeOption(ColorTheme.BLUE,   "Blue",   ThemeManager.bulletColor(ColorTheme.BLUE)),
            ThemeOption(ColorTheme.RED,    "Red",    ThemeManager.bulletColor(ColorTheme.RED)),
            ThemeOption(ColorTheme.YELLOW, "Yellow", ThemeManager.bulletColor(ColorTheme.YELLOW))
        )

        val adapter = object : ArrayAdapter<ThemeOption>(
            requireContext(), android.R.layout.simple_spinner_item, options
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                buildRow(position, convertView, parent)

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
                buildRow(position, convertView, parent)

            private fun buildRow(position: Int, convertView: View?, parent: ViewGroup): View {
                val opt = options[position]
                val row = convertView
                    ?: LayoutInflater.from(context)
                        .inflate(android.R.layout.simple_spinner_item, parent, false)
                val tv = row.findViewById<TextView>(android.R.id.text1)
                tv.text = opt.label
                tv.textSize = 14f
                val density = context.resources.displayMetrics.density
                val size = (20 * density).toInt()
                val circle = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(opt.color)
                    setSize(size, size)
                }
                tv.setCompoundDrawablesWithIntrinsicBounds(circle, null, null, null)
                tv.compoundDrawablePadding = (6 * density).toInt()
                return row
            }
        }

        binding.spinnerColorTheme.adapter = adapter
        val idx = options.indexOfFirst { it.theme == s.colorTheme }.coerceAtLeast(0)
        binding.spinnerColorTheme.setSelection(idx)

        var userInteracted = false
        binding.spinnerColorTheme.post { userInteracted = true }

        binding.spinnerColorTheme.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (!userInteracted) return
                    val chosen = options[pos].theme
                    if (chosen == FontRepository.settings.colorTheme) return
                    FontRepository.settings = FontRepository.settings.copy(colorTheme = chosen)
                    FontRepository.saveSettings(requireContext())
                    restartActivity()
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }
    }

    // ── Follow system switch ──────────────────────────────────────────────

    private fun renderFollowSystemSwitch(s: AppSettings) {
        binding.switchFollowSystem.isChecked = s.followSystem
        updateDarkModeRowVisibility(s.followSystem)
        binding.switchFollowSystem.setOnCheckedChangeListener { _, checked ->
            FontRepository.settings = FontRepository.settings.copy(followSystem = checked)
            FontRepository.saveSettings(requireContext())
            updateDarkModeRowVisibility(checked)
            restartActivity()
        }
    }

    private fun updateDarkModeRowVisibility(followSystem: Boolean) {
        binding.rowDarkMode.visibility = if (followSystem) View.GONE else View.VISIBLE
    }

    // ── Dark mode switch ──────────────────────────────────────────────────

    private fun renderDarkModeSwitch(s: AppSettings) {
        binding.switchDarkMode.isChecked = s.darkMode
        binding.switchDarkMode.setOnCheckedChangeListener { _, checked ->
            FontRepository.settings = FontRepository.settings.copy(darkMode = checked)
            FontRepository.saveSettings(requireContext())
            restartActivity()
        }
    }

    private fun restartActivity() {
        requireActivity().recreate()
    }

    // ── Priority ──────────────────────────────────────────────────────────

    private fun renderPriority(s: AppSettings) {
        val rb = when (s.samplePriority) {
            SamplePriority.METADATA_FIRST -> binding.rbMetaFirst
            SamplePriority.USER_FIRST     -> binding.rbUserFirst
            SamplePriority.ALWAYS_USER    -> binding.rbAlwaysUser
            SamplePriority.ALWAYS_META    -> binding.rbAlwaysMeta
        }
        rb.isChecked = true
        binding.rgPriority.setOnCheckedChangeListener { _, checkedId ->
            val priority = when (checkedId) {
                R.id.rb_meta_first  -> SamplePriority.METADATA_FIRST
                R.id.rb_user_first  -> SamplePriority.USER_FIRST
                R.id.rb_always_user -> SamplePriority.ALWAYS_USER
                R.id.rb_always_meta -> SamplePriority.ALWAYS_META
                else                -> SamplePriority.METADATA_FIRST
            }
            FontRepository.settings = FontRepository.settings.copy(samplePriority = priority)
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

    // ── Recursive switch ──────────────────────────────────────────────────

    private fun renderRecursiveSwitch(s: AppSettings) {
        binding.switchRecursive.isChecked = s.folderRecursive
        binding.switchRecursive.setOnCheckedChangeListener { _, checked ->
            FontRepository.settings = FontRepository.settings.copy(folderRecursive = checked)
            FontRepository.saveSettings(requireContext())
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
                val updated = FontRepository.settings.langSamples.toMutableMap()
                updated.remove(lang)
                FontRepository.settings = FontRepository.settings.copy(langSamples = updated)
                FontRepository.saveSettings(requireContext())
                renderLangList(FontRepository.settings)
                renderLangSpinner(FontRepository.settings)
            }
            binding.langListContainer.addView(lb.root)
        }
        binding.btnAddLang.setOnClickListener { showAddLangDialog() }
    }

    private fun renderLangSpinner(s: AppSettings) {
        val langs = s.langSamples.keys.toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, langs)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLang.adapter = adapter
        val idx = langs.indexOf(s.defaultLang).coerceAtLeast(0)
        binding.spinnerLang.setSelection(idx)
        binding.spinnerLang.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                FontRepository.settings = FontRepository.settings.copy(defaultLang = langs[pos])
                FontRepository.saveSettings(requireContext())
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun showAddLangDialog() {
        val dialogView = LayoutInflater.from(
            ContextThemeWrapper(requireContext(), ThemeManager.currentThemeResId(requireContext()))
        ).inflate(R.layout.dialog_add_lang, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_lang_name)
        val etText = dialogView.findViewById<TextInputEditText>(R.id.et_lang_sample)
        AlertDialog.Builder(ContextThemeWrapper(requireContext(), ThemeManager.currentThemeResId(requireContext())))
            .setTitle(getString(R.string.add_language))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = etName.text?.toString()?.trim() ?: ""
                val text = etText.text?.toString()?.trim() ?: ""
                if (name.isNotEmpty()) {
                    val updated = FontRepository.settings.langSamples.toMutableMap()
                    updated[name] = text
                    FontRepository.settings = FontRepository.settings.copy(langSamples = updated)
                    FontRepository.saveSettings(requireContext())
                    renderLangList(FontRepository.settings)
                    renderLangSpinner(FontRepository.settings)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
