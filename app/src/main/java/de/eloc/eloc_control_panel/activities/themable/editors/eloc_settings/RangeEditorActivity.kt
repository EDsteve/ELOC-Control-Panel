package de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings

import android.content.Context
import android.content.Intent
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

    companion object {
        fun openRangeEditor(
            context: Context,
            property: String,
            settingName: String,
            currentString: String,
            currentFloat: Float,
            minimum: Float,
            maximum: Float
        ) {
            val intent = Intent(context, RangeEditorActivity::class.java)
            intent.putExtra(EXTRA_SETTING_NAME, settingName)
            intent.putExtra(EXTRA_CURRENT_VALUE, currentString)
            intent.putExtra(EXTRA_RANGE_CURRENT, currentFloat)
            intent.putExtra(EXTRA_RANGE_MINIMUM, minimum)
            intent.putExtra(EXTRA_RANGE_MAXIMUM, maximum)
            intent.putExtra(EXTRA_PROPERTY, property)
            context.startActivity(intent)
        }
    }

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
            property = property,
            value = binding.slider.value.toString(),
            commandCreatedCallback = { command ->
                commandId = command.id
                showProgress()
                DeviceDriver.processCommandQueue(command)
            },
            errorCallback = {
                showModalAlert(getString(R.string.error), getString(R.string.invalid_setting))
            },
        ) {
            runOnUiThread {
                val succeeded = DeviceDriver.commandSucceeded(it)
                if (succeeded) {
                    onSaveCompleted()
                } else {
                    showContent()
                    showModalAlert(
                        getString(R.string.error),
                        getString(R.string.failed_to_save)
                    )
                }
            }
        }
    }
}