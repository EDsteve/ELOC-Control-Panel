package de.eloc.eloc_control_panel.activities.themable.editors

import android.os.Bundle
import androidx.core.view.children
import com.google.android.material.radiobutton.MaterialRadioButton
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.showInstructions
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.databinding.ActivityOptionEditorBinding
import de.eloc.eloc_control_panel.driver.DeviceDriver

class OptionEditorActivity : BaseEditorActivity() {
    private lateinit var binding: ActivityOptionEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOptionEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyData()
    }

    override fun applyData() {
        binding.instructionsButton.setOnClickListener { showInstructions() }
        binding.settingNameTextView.text = getString(R.string.text_editor_setting_name, settingName)
        binding.currentValueEditText.setText(currentValue)
        binding.saveButton.setOnClickListener { save() }
        binding.toolbar.setNavigationOnClickListener { goBack() }

        var id = 1
        options.forEach {
            val child = MaterialRadioButton(this)
            child.tag = it.key
            child.text = it.value
            child.id = ++id
            child.isChecked = (currentValue == it.value)
            binding.optionsRadioGroup.addView(child)
        }
    }

    override fun setViews() {
        progressIndicator = binding.progressIndicator
        contentLayout = binding.contentLayout
    }

    override fun save() {
        val selection = binding.optionsRadioGroup.checkedRadioButtonId
        val choice = binding.optionsRadioGroup.children.firstOrNull {
            it.id == selection
        }
        val newValue = choice?.tag?.toString()
        if (newValue == null) {
            showModalAlert(getString(R.string.required), getString(R.string.selection_required))
            return
        }
        if (DeviceDriver.setProperty(property, newValue)) {
            showProgress()
        } else {
            showModalAlert(getString(R.string.error), getString(R.string.invalid_setting))
        }
    }
}