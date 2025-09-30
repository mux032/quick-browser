package com.quick.browser.presentation.ui.saved

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.quick.browser.R
import com.quick.browser.databinding.ItemTagBinding
import com.quick.browser.domain.model.Tag
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import android.widget.PopupMenu
import android.widget.Toast

/**
 * Adapter for displaying tags in a RecyclerView
 */
class TagsAdapter(
    private val onItemClick: (Tag) -> Unit,
    private val onDeleteTag: (Tag) -> Unit,
    private val onRenameTag: (Tag, String) -> Unit
) : ListAdapter<Tag, TagsAdapter.TagViewHolder>(TagDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = ItemTagBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TagViewHolder(private val binding: ItemTagBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tag: Tag) {
            binding.textTagName.text = tag.name

            itemView.setOnClickListener {
                onItemClick(tag)
            }

            binding.buttonTagMenu.setOnClickListener {
                showTagMenu(tag)
            }
        }

        private fun showTagMenu(tag: Tag) {
            val popupMenu = PopupMenu(binding.buttonTagMenu.context, binding.buttonTagMenu)
            popupMenu.menuInflater.inflate(R.menu.menu_tag_options, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_rename_tag -> {
                        showRenameTagDialog(tag)
                        true
                    }
                    R.id.menu_delete_tag -> {
                        showDeleteTagConfirmation(tag)
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }

        private fun showRenameTagDialog(tag: Tag) {
            val context = binding.root.context
            val inputEditText = TextInputEditText(context)

            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.rename_tag))
                .setView(inputEditText)
                .setPositiveButton(context.getString(R.string.rename)) { _, _ ->
                    val newName = inputEditText.text?.toString()?.trim()
                    if (!newName.isNullOrBlank()) {
                        onRenameTag(tag, newName)
                    } else {
                        Toast.makeText(context, context.getString(R.string.tag_name_cannot_be_empty), Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(context.getString(R.string.cancel)) { _, _ -> }
                .show()

            // Pre-fill the input with current tag name
            inputEditText.setText(tag.name)
            inputEditText.requestFocus()
        }

        private fun showDeleteTagConfirmation(tag: Tag) {
            val context = binding.root.context
            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.delete_tag))
                .setMessage(context.getString(R.string.delete_tag_confirmation, tag.name))
                .setPositiveButton(context.getString(R.string.delete)) { _, _ ->
                    onDeleteTag(tag)
                }
                .setNegativeButton(context.getString(R.string.cancel)) { _, _ -> }
                .show()
        }
    }

    class TagDiffCallback : DiffUtil.ItemCallback<Tag>() {
        override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean {
            return oldItem == newItem
        }
    }
}