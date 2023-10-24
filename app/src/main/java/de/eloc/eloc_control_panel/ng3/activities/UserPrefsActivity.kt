package de.eloc.eloc_control_panel.ng3.activities

import android.content.Intent
import android.os.Bundle
import android.widget.CompoundButton
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityUserPrefsBinding
import de.eloc.eloc_control_panel.ng3.data.PreferencesHelper
import de.eloc.eloc_control_panel.ng3.data.PreferredFontSize

class UserPrefsActivity : ThemableActivity() {

    companion object {
        const val EXTRA_FONT_SIZE_CHANGED = "extra_font_size_changed"
        const val TAG_LEFT_MENU_CHIP = "left_menu_chip"
        const val TAG_RIGHT_MENU_CHIP = "right_menu_chip"
    }

    private lateinit var binding: ActivityUserPrefsBinding
    private var preferredFontSize = PreferredFontSize.Small
    private var oldPreferredFontSize = PreferredFontSize.Small
    private val helper = PreferencesHelper.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserPrefsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setChips()
        setListeners()
    }

    override fun onResume() {
        super.onResume()
        loadPrefs()
    }

    private fun onBackButtonPressed() {
        if (oldPreferredFontSize != preferredFontSize) {
            val data = Intent()
            data.putExtra(EXTRA_FONT_SIZE_CHANGED, true)
            setResult(RESULT_OK, data)
        } else {
            setResult(RESULT_CANCELED)
        }
        goBack()
    }

    private fun setChips() {
        binding.leftChipLayout.chip.tag = TAG_LEFT_MENU_CHIP
        binding.leftChipLayout.chip.setText(R.string.top_left)
        binding.leftChipLayout.chip.isChecked = false
        binding.leftChipLayout.chip.setOnCheckedChangeListener(this::menuPositionChanged)
        setChipColors(binding.leftChipLayout)

        binding.rightChipLayout.chip.tag = TAG_RIGHT_MENU_CHIP
        binding.rightChipLayout.chip.setText(R.string.top_right)
        binding.rightChipLayout.chip.isChecked = false
        binding.rightChipLayout.chip.setOnCheckedChangeListener(this::menuPositionChanged)
        setChipColors(binding.rightChipLayout)

        binding.smallFontChipLayout.chip.setText(R.string.small)
        binding.smallFontChipLayout.chip.isChecked = false
        setChipColors(binding.smallFontChipLayout)

        binding.mediumFontChipLayout.chip.setText(R.string.medium)
        binding.mediumFontChipLayout.chip.isChecked = false
        setChipColors(binding.mediumFontChipLayout)

        binding.largeFontChipLayout.chip.setText(R.string.large)
        binding.largeFontChipLayout.chip.isChecked = false
        setChipColors(binding.largeFontChipLayout)
    }

    private fun setListeners() {
        binding.elocAppBar.setOnBackButtonClickedListener { onBackButtonPressed() }

        binding.btDevicesSwitch.setOnCheckedChangeListener { _, checked ->
            helper.setShowAllBluetoothDevices(checked)
        }

        binding.smallFontChipLayout.chip.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                setPreferredFont(PreferredFontSize.Small)
            }
        }
        binding.mediumFontChipLayout.chip.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                setPreferredFont(PreferredFontSize.Medium)
            }
        }
        binding.largeFontChipLayout.chip.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                setPreferredFont(PreferredFontSize.Large)
            }
        }
    }

    private fun menuPositionChanged(chip: CompoundButton, checked: Boolean) {
        val tag = chip.tag.toString()
        val menuOnLeftSide = if (TAG_LEFT_MENU_CHIP == tag) {
            checked
        } else {
            !checked
        }
        if (checked) {
            helper.setMainMenuPosition(menuOnLeftSide)
        }
        setChipColors(binding.rightChipLayout)
        setChipColors(binding.leftChipLayout)
    }

    private fun loadPrefs() {
        binding.btDevicesSwitch.isChecked = helper.showingAllBluetoothDevices()
        if (helper.isMainMenuOnLeft()) {
            binding.leftChipLayout.chip.isChecked = true
        } else {
            binding.rightChipLayout.chip.isChecked = true
        }
        val fontSize = helper.getPreferredFontSizeValue()
        oldPreferredFontSize = PreferredFontSize.parse(fontSize)
        preferredFontSize = PreferredFontSize.parse(fontSize)
        when (preferredFontSize) {
            PreferredFontSize.Small -> binding.smallFontChipLayout.chip.isChecked = true
            PreferredFontSize.Medium -> binding.mediumFontChipLayout.chip.isChecked = true
            PreferredFontSize.Large -> binding.largeFontChipLayout.chip.isChecked = true
        }
    }

    private fun setPreferredFont(newFontSize: PreferredFontSize) {
        preferredFontSize = newFontSize
        helper.setPreferredFontSize(preferredFontSize.code)
        setChipColors(binding.smallFontChipLayout)
        setChipColors(binding.mediumFontChipLayout)
        setChipColors(binding.largeFontChipLayout)
    }
}
//162