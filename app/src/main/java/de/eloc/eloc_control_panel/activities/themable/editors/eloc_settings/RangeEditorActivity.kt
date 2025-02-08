package de.eloc.eloc_control_panel.activities.themable.editors

import android.os.Bundle
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.showInstructions
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.data.Command
import de.eloc.eloc_control_panel.data.MicrophoneVolumePower
import de.eloc.eloc_control_panel.databinding.ActivityEditorRangeBinding
import de.eloc.eloc_control_panel.driver.DeviceDriver

class RangeEditorActivity : BaseEditorActivity() {
    private lateinit var binding: ActivityEditorRangeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorRangeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyData()
    }

    override fun applyData() {
        binding.instructionsButton.setOnClickListener { showInstructions() }
        binding.slider.addOnChangeListener { _, newValue, _ ->
            binding.newValueTextView.text = MicrophoneVolumePower(newValue).percentage
        }
        binding.settingNameTextView.text = getString(R.string.text_editor_setting_name, settingName)
        binding.currentValueEditText.setText(currentValue)
        binding.newValueTextView.text = currentValue
        binding.saveButton.setOnClickListener { save() }
        binding.toolbar.setNavigationOnClickListener { goBack() }
        if ((rangeCurrentValue != null) && (rangeMinimumValue != null) && (rangeMaximumValue != null)) {
            binding.slider.stepSize = 1.0f
            binding.slider.setLabelFormatter { value ->
                MicrophoneVolumePower(value).percentage
            }
            binding.slider.valueFrom = rangeMinimumValue!!
            binding.slider.valueTo = rangeMaximumValue!!
            binding.slider.value = rangeCurrentValue!!
        } else {
            goBack()
        }
    }

    override fun setViews() {
        progressIndicator = binding.progressIndicator
        contentLayout = binding.contentLayout
    }

    override fun save() {
        Command.createSetConfigPropertyCommand(
            property,
            binding.slider.value.toString(),
            { command ->
                showProgress()
                DeviceDriver.processCommandQueue(command)
            },
            {
                showModalAlert(getString(R.string.error), getString(R.string.invalid_setting))
            })
    }
}