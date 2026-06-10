package com.fontlens.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.fontlens.R
import com.fontlens.data.AppSettings
import com.fontlens.data.AppTheme
import com.fontlens.data.FontRepository
import com.fontlens.data.SamplePriority
import com.fontlens.databinding.FragmentSettingsBinding
import com.fontlens.databinding.ItemLangSettingBinding
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
        renderTheme(s)
        renderPriority(s)
        renderGlyphSwitch(s)
        renderRecursiveSwitch(s)
        renderLangList(s)
        renderLangSpinner(s)
    }

    private fun renderTheme(s: AppSettings) {
        val rb = when (s.theme) {
            AppTheme.SYSTEM -> binding.rbThemeSystem
            AppTheme.DAY    -> binding.rbThemeDay
            AppTheme.NIGHT  -> binding.rbThemeNight
        }
        rb.isChecked = true
        binding.rgTheme.setOnCheckedChangeListener { _, id ->
            val theme = when (id) {
                R.id.rb_theme_system -> AppTheme.SYSTEM
                R.id.rb_theme_day    -> AppTheme.DAY
                R.id.rb_theme_night  -> AppTheme.NIGHT
                else                 -> AppTheme.SYSTEM
            }
            FontRepository.settings = FontRepository.settings.copy(theme = theme)
            FontRepository.saveSettings(requireContext())
            AppCompatDelegate.setDefaultNightMode(when (theme) {
                AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                AppTheme.DAY    -> AppCompatDelegate.MODE_NIGHT_NO
                AppTheme.NIGHT  -> AppCompatDelegate.MODE_NIGHT_YES
            })
        }
    }

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

    private fun renderGlyphSwitch(s: AppSettings) {
        binding.switchGlyphAll.isChecked = s.glyphShowAll
        binding.switchGlyphAll.setOnCheckedChangeListener { _, checked ->
            FontRepository.settings = FontRepository.settings.copy(glyphShowAll = checked)
            FontRepository.saveSettings(requireContext())
        }
    }

    private fun renderRecursiveSwitch(s: AppSettings) {
        binding.switchRecursive.isChecked = s.folderRecursive
        binding.switchRecursive.setOnCheckedChangeListener { _, checked ->
            FontRepository.settings = FontRepository.settings.copy(folderRecursive = checked)
            FontRepository.saveSettings(requireContext())
        }
    }

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
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_lang, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_lang_name)
        val etText = dialogView.findViewById<TextInputEditText>(R.id.et_lang_sample)
        AlertDialog.Builder(requireContext())
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
