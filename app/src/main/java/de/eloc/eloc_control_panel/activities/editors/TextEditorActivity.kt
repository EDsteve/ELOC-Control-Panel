package de.eloc.eloc_control_panel.activities.editors

import android.os.Bundle
import android.text.InputType
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.hideKeyboard
import de.eloc.eloc_control_panel.activities.showInstructions
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.databinding.ActivityTextEditorBinding
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.driver.General
import de.eloc.eloc_control_panel.interfaces.TextInputWatcher

class TextEditorActivity : BaseEditorActivity() {

    private lateinit var binding: ActivityTextEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyData()
    }

    override fun applyData() {
        binding.instructionsButton.setOnClickListener { showInstructions() }
        binding.settingNameTextView.text = getString(R.string.text_editor_setting_name, settingName)
        binding.currentValueEditText.setText(currentValue)
        binding.saveButton.setOnClickListener { save() }
        binding.toolbar.setNavigationOnClickListener { goBack() }
        binding.newValueTextInputLayout.prefixText = prefix
        binding.newValueEditText.addTextChangedListener(TextInputWatcher(binding.newValueTextInputLayout))
        if (isNumeric) {
            binding.newValueEditText.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }

    override fun setViews() {
        progressIndicator = binding.progressIndicator
        contentLayout = binding.contentLayout
    }

    override fun save() {
        hideKeyboard()
        var newValue = binding.newValueEditText.text.toString().trim()
        if (newValue.isEmpty()) {
            if (property == General.FILE_HEADER) {
                newValue = NOT_SET
            } else {
                binding.newValueTextInputLayout.error = getString(R.string.required)
                return
            }
        }
        newValue = prefix + newValue

        val numericValue = newValue.toDoubleOrNull()
        if (isNumeric) {
            if (numericValue == null) {
                binding.newValueTextInputLayout.error = getString(R.string.enter_a_valid_number)
                return
            } else if ((minimumValue != null) && (numericValue < minimumValue!!)) {
                binding.newValueTextInputLayout.error =
                    getString(R.string.minimum_value_error, minimumValue!!.toInt())
                return
            }
        }

        val accepted = DeviceDriver.setProperty(
            property,
            if (isNumeric) {
                numericValue.toString()
            } else {
                newValue
            },
        )
        if (accepted) {
            showProgress()
        } else {
            showModalAlert(getString(R.string.error), getString(R.string.invalid_setting))
        }
    }
}