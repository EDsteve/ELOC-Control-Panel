package de.eloc.eloc_control_panel.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.DialogFragment
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.LayoutUnknownLocationBinding
import de.eloc.eloc_control_panel.interfaces.VoidCallback

class UnknowLocationDialog(
    private val height: Int,
    private val items: List<String>,
    private val showListener: VoidCallback,
    private val dismissListener: VoidCallback
) :
    DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AppCompatDialog(requireContext(), android.R.style.Theme_Material_NoActionBar_Fullscreen)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = LayoutUnknownLocationBinding.inflate(inflater, container, false)
        binding.backButton.setOnClickListener { close() }
        val sortedItems = if (items.isEmpty()) {
            listOf(getString(R.string.none))
        } else {
            items.sorted()
        }
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortedItems)
        binding.listView.adapter = adapter
        return binding.root
    }

    private fun close() {
        dismissListener.handler()
        dismiss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                close()
            }
        }

        dialog?.setOnDismissListener { dismissListener.handler() }
        dialog?.setOnShowListener { showListener.handler() }
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
}