package de.eloc.eloc_control_panel.activities.themable.editors.preferences

import android.os.Bundle
import android.view.View
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.themable.ThemableActivity
import de.eloc.eloc_control_panel.widgets.ProgressIndicator

abstract class BaseEditorActivity : ThemableActivity() {
    companion object {
        const val EXTRA_RANGE_CURRENT = "range_current"
        const val EXTRA_RANGE_MINIMUM = "range_minimum"
        const val EXTRA_RANGE_MAXIMUM = "range_maximum"
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
    protected var rangeCurrentValue: Float? = null
    protected var rangeMinimumValue: Float? = null
    protected var rangeMaximumValue: Float? = null
    protected var options = mutableMapOf<String, String>()
    protected lateinit var contentLayout: View
    protected lateinit var progressIndicator: ProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setData()
    }

    override fun onResume() {
        super.onResume()
        setViews()
        showContent()
    }

    protected abstract fun setViews()

    protected abstract fun applyData()



    private fun setData() {
        val extras = intent.extras

        isNumeric = extras?.getBoolean(EXTRA_IS_NUMERIC, false) ?: false

        if (extras?.containsKey(EXTRA_MINIMUM) == true) {
            minimumValue = extras.getDouble(EXTRA_MINIMUM)
        }

        if (extras?.containsKey(EXTRA_RANGE_CURRENT) == true) {
            rangeCurrentValue = extras.getFloat(EXTRA_RANGE_CURRENT)
        }

        if (extras?.containsKey(EXTRA_RANGE_MINIMUM) == true) {
            rangeMinimumValue = extras.getFloat(EXTRA_RANGE_MINIMUM)
        }
        if (extras?.containsKey(EXTRA_RANGE_MAXIMUM) == true) {
            rangeMaximumValue = extras.getFloat(EXTRA_RANGE_MAXIMUM)
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

        val rawOptions = extras?.getStringArray(EXTRA_OPTIONS)
        val splitter = "|"
        rawOptions?.forEach {
            if (it.contains(splitter)) {
                val parts = it.split("|")
                options[parts[0]] = parts[1]
            }
        }

    }

    private fun showContent() {
        progressIndicator.visibility = View.INVISIBLE
        contentLayout.visibility = View.VISIBLE
    }

    protected abstract fun save()
}