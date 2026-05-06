package de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import com.google.android.material.chip.Chip
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.TimeSliderHelper
import de.eloc.eloc_control_panel.activities.TimeUnit
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.prettifyTime
import de.eloc.eloc_control_panel.activities.showInstructions
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.data.Command
import de.eloc.eloc_control_panel.data.MicrophoneVolumePower
import de.eloc.eloc_control_panel.databinding.ActivityEditorRangeBinding
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.driver.Inference
import de.eloc.eloc_control_panel.driver.Microphone

class RangeEditorActivity : BaseEditorActivity() {
    private lateinit var binding: ActivityEditorRangeBinding

    private var suppressSync = false
    private var currentUnit: TimeUnit = TimeUnit.SECONDS
    private val chipsBySeconds = mutableMapOf<Int, Chip>()
    private var unitOptions: List<TimeUnit> = emptyList()

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
        binding.settingNameTextView.text = getString(R.string.text_editor_setting_name, settingName)
        binding.currentValueEditText.setText(currentValue)
        binding.newValueTextView.text =
            if (TimeSliderHelper.isTimeProperty(property) && rangeCurrentValue != null) {
                prettifyTime(rangeCurrentValue!!.toInt())
            } else {
                currentValue
            }
        binding.saveButton.setOnClickListener { save() }
        binding.toolbar.setNavigationOnClickListener { goBack() }
        if ((rangeCurrentValue != null) && (rangeMinimumValue != null) && (rangeMaximumValue != null)) {
            binding.slider.stepSize = 1.0f
            binding.slider.setLabelFormatter { value ->
                when {
                    property == Microphone.VOLUME_POWER -> MicrophoneVolumePower(value).percentage
                    TimeSliderHelper.isTimeProperty(property) -> prettifyTime(value.toInt())
                    property == Inference.THRESHOLD -> value.toInt().toString()
                    else -> value.toString()
                }
            }
            binding.slider.valueFrom = rangeMinimumValue!!
            binding.slider.valueTo = rangeMaximumValue!!
            binding.slider.value = rangeCurrentValue!!

            binding.slider.addOnChangeListener { _, newValue, _ ->
                setNewValue(newValue, false)
                if (!suppressSync && TimeSliderHelper.isTimeProperty(property)) {
                    suppressSync = true
                    syncNumericFromSeconds(newValue.toInt())
                    syncChipFromSeconds(newValue.toInt())
                    suppressSync = false
                }
            }
            binding.incrementButton.setOnClickListener {
                val step = stepSize()
                val newValue = binding.slider.value + step
                setNewValue(newValue, true)
            }
            binding.decrementButton.setOnClickListener {
                val step = stepSize()
                val newValue = binding.slider.value - step
                setNewValue(newValue, true)
            }

            if (TimeSliderHelper.isTimeProperty(property)) {
                binding.timeControls.visibility = View.VISIBLE
                setupTimeControls()
            }
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
            value = binding.slider.value.toInt().toString(),
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

    private fun stepSize(): Float =
        if (TimeSliderHelper.isTimeProperty(property)) {
            currentUnit.seconds.toFloat()
        } else {
            binding.slider.stepSize
        }

    private fun setupTimeControls() {
        val min = rangeMinimumValue!!.toInt()
        val max = rangeMaximumValue!!.toInt()
        val current = rangeCurrentValue!!.toInt()

        // Chips
        val cg = binding.chipGroupQuickPicks
        cg.removeAllViews()
        chipsBySeconds.clear()
        TimeSliderHelper.picksFor(min, max).forEach { qp ->
            val chip = Chip(this).apply {
                text = qp.label
                isCheckable = true
                isChecked = (qp.seconds == current)
                setOnClickListener {
                    suppressSync = true
                    binding.slider.value = qp.seconds.toFloat()
                    syncNumericFromSeconds(qp.seconds)
                    syncChipFromSeconds(qp.seconds)
                    suppressSync = false
                    binding.newValueTextView.text = prettifyTime(qp.seconds)
                }
            }
            cg.addView(chip)
            chipsBySeconds[qp.seconds] = chip
        }

        // Unit dropdown
        unitOptions = TimeSliderHelper.unitsFor(min, max)
        val unitLabels = unitOptions.map { getString(it.labelRes) }
        val dropdown = binding.unitDropdown
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, unitLabels)
        dropdown.setAdapter(adapter)
        currentUnit = TimeSliderHelper.bestUnitFor(current).takeIf { it in unitOptions }
            ?: unitOptions.first()
        dropdown.setText(getString(currentUnit.labelRes), false)
        dropdown.setOnItemClickListener { _, _, pos, _ ->
            currentUnit = unitOptions[pos]
            suppressSync = true
            syncNumericFromSeconds(binding.slider.value.toInt())
            suppressSync = false
        }

        // Numeric input
        suppressSync = true
        syncNumericFromSeconds(current)
        suppressSync = false
        binding.numericValueEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressSync) return
                val raw = s?.toString()?.toIntOrNull() ?: return
                val seconds = (raw * currentUnit.seconds).coerceIn(min, max)
                suppressSync = true
                binding.slider.value = seconds.toFloat()
                syncChipFromSeconds(seconds)
                binding.newValueTextView.text = prettifyTime(seconds)
                suppressSync = false
            }
        })
    }

    private fun syncNumericFromSeconds(seconds: Int) {
        binding.numericValueEditText.setText(
            TimeSliderHelper.toUnitValue(seconds, currentUnit).toString()
        )
    }

    private fun syncChipFromSeconds(seconds: Int) {
        chipsBySeconds.forEach { (s, chip) -> chip.isChecked = (s == seconds) }
    }

    private fun setNewValue(newValue: Float, fromButton: Boolean) {
        val min = binding.slider.valueFrom
        val max = binding.slider.valueTo
        val sanitizedValue = if (newValue <= min) {
            min
        } else if (newValue >= max) {
            max
        } else {
            newValue
        }
        if (fromButton) {
            binding.slider.value = sanitizedValue
        } else {
            binding.newValueTextView.text = when {
                property == Microphone.VOLUME_POWER -> MicrophoneVolumePower(sanitizedValue).percentage
                TimeSliderHelper.isTimeProperty(property) -> prettifyTime(sanitizedValue.toInt())
                else -> sanitizedValue.toInt().toString()
            }
        }
    }
}
