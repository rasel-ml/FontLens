package com.fontlens.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.fontlens.databinding.DialogLoadingBinding

class LoadingDialog : DialogFragment() {

    private var _binding: DialogLoadingBinding? = null
    private val binding get() = _binding!!

    fun updateProgress(loaded: Int, total: Int) {
        if (_binding == null) return
        if (total > 0) {
            binding.tvProgress.visibility = View.VISIBLE
            binding.tvProgress.text = context.getString(R.string.progress_format, loaded, total)
            binding.progressBar.isIndeterminate = false
            binding.progressBar.max = total
            binding.progressBar.progress = loaded
        } else {
            binding.tvProgress.visibility = View.GONE
            binding.progressBar.isIndeterminate = true
        }
    }

    fun updateMessage(msg: String) {
        _binding?.tvMessage?.text = msg
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.progressBar.isIndeterminate = true
        binding.tvProgress.visibility = View.GONE
        binding.tvMessage.text = context.getString(R.string.loading_fonts)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isCancelable = false
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        const val TAG = "LoadingDialog"
    }
}
