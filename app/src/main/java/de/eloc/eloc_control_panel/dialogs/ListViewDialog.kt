package de.eloc.eloc_control_panel.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ListAdapter
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.eloc.eloc_control_panel.databinding.LayoutListViewDialogBinding

class ListViewDialog(
    private val title: String,
    private val height: Int,
    private val adapter: ListAdapter,
    private val showListener: () -> Unit,
    private val dismissListener: () -> Unit
) :
    DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AppCompatDialog(requireContext(), android.R.style.Theme_Material_NoActionBar_Fullscreen)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = LayoutListViewDialogBinding.inflate(inflater, container, false)
        binding.titleTextView.text = title
        binding.backButton.setOnClickListener { close() }
        binding.listView.adapter = adapter
        return binding.root
    }

    private fun close() {
        dismissListener()
        dismiss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                close()
            }
        }

        dialog?.setOnDismissListener { dismissListener() }
        dialog?.setOnShowListener { showListener() }
        (dialog as ComponentDialog).onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            callback
        )
        dialog?.setCancelable(false)
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            height
        )
    }

    fun show(manager: FragmentManager) {
        show(manager, "listviewdialog")
    }
}