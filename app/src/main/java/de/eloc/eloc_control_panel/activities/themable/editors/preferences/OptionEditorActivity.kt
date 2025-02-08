package de.eloc.eloc_control_panel.activities.themable.editors.preferences

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.children
import com.google.android.material.radiobutton.MaterialRadioButton
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.restartApp
import de.eloc.eloc_control_panel.activities.showInstructions
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.data.MainMenuPosition
import de.eloc.eloc_control_panel.data.PreferredFontSize
import de.eloc.eloc_control_panel.data.StatusUploadInterval
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.databinding.ActivityEditorOptionsBinding

class OptionEditorActivity : BaseEditorActivity() {
    companion object {
        fun open(
            context: Context,
            property: String,
            settingName: String,
            currentValue: String,
            options: List<String>
        ) {
            val intent = Intent(context, OptionEditorActivity::class.java)
            intent.putExtra(EXTRA_SETTING_NAME, settingName)
            intent.putExtra(EXTRA_CURRENT_VALUE, currentValue)
            intent.putExtra(EXTRA_PROPERTY, property)
            intent.putExtra(EXTRA_OPTIONS, options.toTypedArray())
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityEditorOptionsBinding

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
        val code = newValue.toIntOrNull()
        if (code != null) {
            when (property) {
                Preferences.PREF_USER_FONT_SIZE -> {
                    val fontSize = PreferredFontSize.parse(code)
                    Preferences.preferredFontSize = fontSize
                    if (currentValue != fontSize.toString()) {
                        restartApp()
                        return
                    }
                }

                Preferences.PREF_MAIN_MENU_POSITION -> {
                    val menuPosition = MainMenuPosition.parse(code)
                    Preferences.mainMenuPosition = menuPosition
                }

                Preferences.PREF_STATUS_UPLOAD_INTERVAL -> {
                    val interval = StatusUploadInterval.parse(code)
                    Preferences.statusUploadInterval = interval
                }
            }
        }
        goBack()
    }
}