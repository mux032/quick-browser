package com.quick.browser.presentation.ui.saved

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.quick.browser.R
import com.quick.browser.databinding.DialogAddTagBinding
import com.quick.browser.presentation.ui.saved.viewmodel.TagViewModel

class AddTagDialogFragment : DialogFragment() {

    private val tagViewModel: TagViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val binding = DialogAddTagBinding.inflate(requireActivity().layoutInflater)

            builder.setView(binding.root)
                .setTitle(R.string.create_new_tag)
                .setPositiveButton(R.string.create) { _, _ ->
                    val tagName = binding.editTextTagName.text.toString().trim()
                    if (tagName.isNotEmpty()) {
                        tagViewModel.createTag(tagName)
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}