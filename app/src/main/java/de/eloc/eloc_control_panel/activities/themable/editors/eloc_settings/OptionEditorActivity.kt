package de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.children
import com.google.android.material.radiobutton.MaterialRadioButton
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.hideKeyboard
import de.eloc.eloc_control_panel.activities.showInstructions
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.data.Command
import de.eloc.eloc_control_panel.databinding.ActivityEditorOptionsBinding
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.interfaces.TextInputWatcher

class OptionEditorActivity : BaseEditorActivity() {
    companion object {
        // Marks the extra radio button that reveals the free-text field; the "|" splitter used
        // for option keys makes this impossible to collide with a real option key.
        private const val CUSTOM_TAG = "custom|value"

        fun open(
            context: Context,
            property: String,
            settingName: String,
            currentValue: String,
            options: List<String>,
            allowCustom: Boolean = false,
        ) {
            val intent = Intent(context, OptionEditorActivity::class.java)
            intent.putExtra(EXTRA_SETTING_NAME, settingName)
            intent.putExtra(EXTRA_CURRENT_VALUE, currentValue)
            intent.putExtra(EXTRA_PROPERTY, property)
            intent.putExtra(EXTRA_OPTIONS, options.toTypedArray())
            intent.putExtra(EXTRA_ALLOW_CUSTOM, allowCustom)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityEditorOptionsBinding
    private var customOptionId = View.NO_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorOptionsBinding.inflate(layoutInflater)
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
        var matchesOption = false
        options.forEach {
            val child = MaterialRadioButton(this)
            child.tag = it.key
            child.text = it.value
            child.id = ++id
            child.isChecked = (currentValue == it.value)
            if (child.isChecked) {
                matchesOption = true
            }
            binding.optionsRadioGroup.addView(child)
        }

        if (allowCustom) {
            addCustomOption(++id, preselect = !matchesOption)
        }
    }

    // Lets the user set a value the option list does not contain (e.g. a LoRa region added by
    // newer firmware). A value already on the device that is not in the list preselects this.
    private fun addCustomOption(id: Int, preselect: Boolean) {
        val child = MaterialRadioButton(this)
        child.tag = CUSTOM_TAG
        child.text = getString(R.string.custom)
        child.id = id
        child.isChecked = preselect
        binding.optionsRadioGroup.addView(child)
        customOptionId = id

        binding.customValueEditText.addTextChangedListener(
            TextInputWatcher(binding.customValueInputLayout)
        )
        if (preselect) {
            binding.customValueEditText.setText(currentValue)
        }
        setCustomFieldVisibility(preselect)
        binding.optionsRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            setCustomFieldVisibility(checkedId == customOptionId)
        }
    }

    private fun setCustomFieldVisibility(visible: Boolean) {
        binding.customValueInputLayout.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) {
            hideKeyboard()
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
        var newValue = choice?.tag?.toString()
        if (newValue == null) {
            showModalAlert(getString(R.string.required), getString(R.string.selection_required))
            return
        }
        if (newValue == CUSTOM_TAG) {
            hideKeyboard()
            newValue = binding.customValueEditText.text.toString().trim()
            if (newValue.isEmpty()) {
                binding.customValueInputLayout.error = getString(R.string.required)
                return
            }
        }
        Command.createSetConfigPropertyCommand(
            property = property,
            value = newValue,
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