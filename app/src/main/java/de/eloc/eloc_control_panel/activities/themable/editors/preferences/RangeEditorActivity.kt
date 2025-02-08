package de.eloc.eloc_control_panel.activities.themable.editors.preferences

import android.content.Context
import android.content.Intent
import android.os.Bundle
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.showInstructions
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.databinding.ActivityEditorRangeBinding

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
            intent.putExtra(
                de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings.BaseEditorActivity.EXTRA_SETTING_NAME,
                settingName
            )
            intent.putExtra(
                de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings.BaseEditorActivity.EXTRA_CURRENT_VALUE,
                currentString
            )
            intent.putExtra(
                de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings.BaseEditorActivity.EXTRA_RANGE_CURRENT,
                currentFloat
            )
            intent.putExtra(
                de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings.BaseEditorActivity.EXTRA_RANGE_MINIMUM,
                minimum
            )
            intent.putExtra(
                de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings.BaseEditorActivity.EXTRA_RANGE_MAXIMUM,
                maximum
            )
            intent.putExtra(
                de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings.BaseEditorActivity.EXTRA_PROPERTY,
                property
            )
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
            when (property) {
                Preferences.PREF_GPS_LOCATION_TIMEOUT -> {
                    val label = "${newValue.toInt()} secs"
                    binding.newValueTextView.text = label
                }
            }
        }
        binding.settingNameTextView.text = getString(R.string.text_editor_setting_name, settingName)
        binding.currentValueEditText.setText(currentValue)
        when (property) {
            Preferences.PREF_GPS_LOCATION_TIMEOUT -> {
                binding.slider.setLabelFormatter { value -> "${value.toInt()} secs" }
                binding.slider.value =
                    currentValue.toFloatOrNull() ?: Preferences.MIN_GPS_TIMEOUT_SECONDS.toFloat()
                binding.newValueTextView.text = currentValue
            }
        }

        binding.saveButton.setOnClickListener { save() }
        binding.toolbar.setNavigationOnClickListener { goBack() }
        if ((rangeCurrentValue != null) && (rangeMinimumValue != null) && (rangeMaximumValue != null)) {
            binding.slider.stepSize = 1.0f
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
        when (property) {
            Preferences.PREF_GPS_LOCATION_TIMEOUT -> {
                Preferences.gpsLocationTimeoutSeconds = binding.slider.value.toInt()
            }
        }
        goBack()
    }
}