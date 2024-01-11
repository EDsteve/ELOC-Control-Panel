package de.eloc.eloc_control_panel.activities.editors

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.data.CommandType
import de.eloc.eloc_control_panel.data.SampleRate
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.driver.General
import de.eloc.eloc_control_panel.driver.Microphone
import de.eloc.eloc_control_panel.interfaces.GetCommandCompletedCallback

internal const val NOT_SET = "not_set"
abstract class BaseEditorActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_SETTING_NAME = "setting_name"
        const val EXTRA_CURRENT_VALUE = "current_value"
        const val EXTRA_PROPERTY = "property"
        const val EXTRA_OPTIONS = "options"
        const val EXTRA_IS_NUMERIC = "is_numeric"
        const val EXTRA_PREFIX = "prefix"
        const val EXTRA_MINIMUM = "minimum"
    }

    protected var property = ""
    protected var settingName = ""
    protected var currentValue = ""
    protected var prefix = ""
    protected var isNumeric = false
    protected var minimumValue: Double? = null
    protected var options = mutableMapOf<String, String>()
    private var configUpdated = false
    private var statusUpdated = false
    protected lateinit var progressLayout: View
    protected lateinit var contentLayout: View
    protected lateinit var progressTextView: TextView
    private val onGetDeviceInfoCompleted = GetCommandCompletedCallback {
        if (!configUpdated && (it == CommandType.GetConfig)) {
            configUpdated = true
        }
        if (!statusUpdated && (it == CommandType.GetStatus)) {
            statusUpdated = true
        }
        if (statusUpdated && configUpdated) {
            runOnUiThread {
                goBack()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setData()
    }

    override fun onResume() {
        super.onResume()
        setViews()
        showContent()
    }

    override fun onDestroy() {
        super.onDestroy()
        DeviceDriver.removeOnGetCommandCompletedListener(onGetDeviceInfoCompleted)
        DeviceDriver.removeOnSetCommandCompletedListener(::onSaveCompleted)
    }

    protected abstract fun setViews()

    protected abstract fun applyData()

    private fun setData() {
        val extras = intent.extras

        isNumeric = extras?.getBoolean(EXTRA_IS_NUMERIC, false) ?: false

        if (extras?.containsKey(EXTRA_MINIMUM) == true) {
            minimumValue = extras.getDouble(EXTRA_MINIMUM)
        }

        prefix = extras?.getString(EXTRA_PREFIX, "")?.trim() ?: ""

        settingName = extras?.getString(EXTRA_SETTING_NAME)?.trim() ?: ""
        if (settingName.isEmpty()) {
            goBack()
            return
        }

        property = extras?.getString(EXTRA_PROPERTY)?.trim() ?: ""
        if (property.isEmpty()) {
            goBack()
            return
        }

        currentValue = extras?.getString(EXTRA_CURRENT_VALUE)?.trim() ?: ""
        if (currentValue.isEmpty()) {
            goBack()
            return
        }
        currentValue = prefix + currentValue
        if ((currentValue.lowercase() == NOT_SET) && (property == General.FILE_HEADER)) {
            currentValue = ""
        }

        if (property == Microphone.SAMPLE_RATE) {
            val code = currentValue.toDoubleOrNull() ?: 0.0
            currentValue = SampleRate.parse(code).toString()
        }

        val rawOptions = extras?.getStringArray(EXTRA_OPTIONS)
        val splitter = "|"
        rawOptions?.forEach {
            if (it.contains(splitter)) {
                val parts = it.split("|")
                options[parts[0]] = parts[1]
            }
        }

        DeviceDriver.addOnSetCommandCompletedListener(::onSaveCompleted)
        DeviceDriver.addOnGetCommandCompletedListener(onGetDeviceInfoCompleted)
    }

    protected fun showProgress() {
        progressTextView.text = getString(R.string.applying_changes)
        progressLayout.visibility = View.VISIBLE
        contentLayout.visibility = View.INVISIBLE
    }

    private fun showContent() {
        progressLayout.visibility = View.INVISIBLE
        contentLayout.visibility = View.VISIBLE
    }

    private fun onSaveCompleted(success: Boolean, type: CommandType) {
        runOnUiThread {
            if (success) {
                if (type.isSetCommand) {
                    progressTextView.text = getString(R.string.updating_values)
                    DeviceDriver.getDeviceInfo()
                }
            } else {
                showContent()
                showModalAlert(
                    getString(R.string.error),
                    getString(R.string.failed_to_save)
                )
            }
        }
    }

    protected abstract fun save()
}