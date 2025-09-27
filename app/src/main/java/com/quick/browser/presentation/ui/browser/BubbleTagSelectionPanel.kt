package com.quick.browser.presentation.ui.browser

import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import com.google.android.material.button.MaterialButton
import com.quick.browser.R
import com.quick.browser.domain.model.Tag

class BubbleTagSelectionPanel(
    private val context: Context,
    private val bubbleAnimator: BubbleAnimator
) {

    private var isVisible = false
    private var tagSelectionPanel: View? = null
    private var tagsContainer: LinearLayout? = null
    private var btnCreateTag: MaterialButton? = null
    private var defaultTagButton: View? = null

    interface TagSelectionListener {
        fun onTagSelected(tagId: Long)
        fun onCreateTag(tagName: String)
    }

    private var listener: TagSelectionListener? = null

    fun setListener(listener: TagSelectionListener?) {
        this.listener = listener
    }

    fun initialize(panel: View) {
        this.tagSelectionPanel = panel
        tagsContainer = panel.findViewById(R.id.tag_selection_container)
        btnCreateTag = panel.findViewById(R.id.btn_add_new_tag)
        defaultTagButton = panel.findViewById(R.id.tag_default_selection)

        btnCreateTag?.setOnClickListener {
            showCreateTagDialog()
        }

        defaultTagButton?.setOnClickListener {
            listener?.onTagSelected(0)
            hide()
        }
        
        panel.setOnTouchListener { _, _ -> true }
    }

    fun show(tags: List<Tag>, triggerButton: View? = null) {
        if (isVisible) return

        populateTags(tags)

        isVisible = true
        tagSelectionPanel?.let {
            bubbleAnimator.animateSettingsPanelShow(it, triggerButton)
        }
    }

    fun hide() {
        if (!isVisible) return

        isVisible = false
        tagSelectionPanel?.let {
            bubbleAnimator.animateSettingsPanelHide(it)
        }
    }

    fun isVisible(): Boolean {
        return isVisible
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        if (!isVisible || event.action != MotionEvent.ACTION_DOWN) {
            return false
        }

        val touchX = event.rawX.toInt()
        val touchY = event.rawY.toInt()

        val panelRect = Rect()
        tagSelectionPanel?.getGlobalVisibleRect(panelRect)

        if (!panelRect.contains(touchX, touchY)) {
            hide()
            return true
        }

        return false
    }

    private fun populateTags(tags: List<Tag>) {
        tagsContainer?.removeAllViews()

        val inflater = LayoutInflater.from(android.view.ContextThemeWrapper(context, R.style.Theme_QBrowser))
        for (tag in tags) {
            val tagView = inflater.inflate(R.layout.item_tag, tagsContainer, false) as LinearLayout
            val tagName = tagView.findViewById<TextView>(R.id.text_tag_name)
            tagName.text = tag.name
            tagView.setOnClickListener {
                listener?.onTagSelected(tag.id)
                hide()
            }
            tagsContainer?.addView(tagView)
        }
    }

    private fun showCreateTagDialog() {
        val input = EditText(context)
        input.hint = "Enter tag name"

        val dialog = AlertDialog.Builder(android.view.ContextThemeWrapper(context, R.style.Theme_QBrowser))
            .setTitle("Create New Tag")
            .setView(input)
            .setPositiveButton("Create") { dialog, _ ->
                val tagName = input.text.toString().trim()
                if (tagName.isNotEmpty()) {
                    listener?.onCreateTag(tagName)
                    hide()
                } else {
                    Toast.makeText(context, "Tag name cannot be empty", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .create()

        dialog.window?.setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }
}
