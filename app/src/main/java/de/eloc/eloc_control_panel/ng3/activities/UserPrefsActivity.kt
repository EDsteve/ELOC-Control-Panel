package de.eloc.eloc_control_panel.ng3.activities

import android.content.Intent
import android.os.Bundle
import android.widget.CompoundButton
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityUserPrefsBinding
import de.eloc.eloc_control_panel.ng3.data.helpers.PreferencesHelper
import de.eloc.eloc_control_panel.ng3.data.PreferredFontSize
import de.eloc.eloc_control_panel.ng3.data.StatusUploadInterval

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
        setChipColors(binding.leftChipLayout)

        binding.rightChipLayout.chip.tag = TAG_RIGHT_MENU_CHIP
        binding.rightChipLayout.chip.setText(R.string.top_right)
        binding.rightChipLayout.chip.isChecked = false
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

        binding.uploadInterval15minsChipLayout.chip.setText(R.string.mins_15)
        binding.uploadInterval15minsChipLayout.chip.isChecked = false
        setChipColors(binding.uploadInterval15minsChipLayout)

        binding.uploadInterval30minsChipLayout.chip.setText(R.string.mins_30)
        binding.uploadInterval30minsChipLayout.chip.isChecked = false
        setChipColors(binding.uploadInterval30minsChipLayout)

        binding.uploadInterval60minsChipLayout.chip.setText(R.string.mins_60)
        binding.uploadInterval60minsChipLayout.chip.isChecked = false
        setChipColors(binding.uploadInterval60minsChipLayout)

        binding.uploadInterval120minsChipLayout.chip.setText(R.string.mins_120)
        binding.uploadInterval120minsChipLayout.chip.isChecked = false
        setChipColors(binding.uploadInterval120minsChipLayout)
    }

    private fun setListeners() {
        binding.toolbar.setNavigationOnClickListener { onBackButtonPressed() }

        binding.btDevicesSwitch.setOnCheckedChangeListener { _, checked ->
            helper.setShowAllBluetoothDevices(checked)
        }

        binding.rightChipLayout.chip.setOnCheckedChangeListener(this::menuPositionChanged)
        binding.leftChipLayout.chip.setOnCheckedChangeListener(this::menuPositionChanged)

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

        binding.uploadInterval15minsChipLayout.chip.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                setPreferredUploadInterval(StatusUploadInterval.Mins15)
            }
        }

        binding.uploadInterval30minsChipLayout.chip.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                setPreferredUploadInterval(StatusUploadInterval.Mins30)
            }
        }

        binding.uploadInterval60minsChipLayout.chip.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                setPreferredUploadInterval(StatusUploadInterval.Mins60)
            }
        }

        binding.uploadInterval120minsChipLayout.chip.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                setPreferredUploadInterval(StatusUploadInterval.Mins120)
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
        val fontSize = helper.getPreferredFontSize()
        oldPreferredFontSize = fontSize
        preferredFontSize = fontSize
        when (preferredFontSize) {
            PreferredFontSize.Small -> binding.smallFontChipLayout.chip.isChecked = true
            PreferredFontSize.Medium -> binding.mediumFontChipLayout.chip.isChecked = true
            PreferredFontSize.Large -> binding.largeFontChipLayout.chip.isChecked = true
        }

        when (helper.getStatusUploadInterval()) {
            StatusUploadInterval.Mins15 -> binding.uploadInterval15minsChipLayout.chip.isChecked =
                true

            StatusUploadInterval.Mins30 -> binding.uploadInterval30minsChipLayout.chip.isChecked =
                true

            StatusUploadInterval.Mins60 -> binding.uploadInterval60minsChipLayout.chip.isChecked =
                true

            StatusUploadInterval.Mins120 -> binding.uploadInterval120minsChipLayout.chip.isChecked =
                true
        }
    }

    private fun setPreferredFont(newFontSize: PreferredFontSize) {
        preferredFontSize = newFontSize
        helper.setPreferredFontSize(preferredFontSize)
        setChipColors(binding.smallFontChipLayout)
        setChipColors(binding.mediumFontChipLayout)
        setChipColors(binding.largeFontChipLayout)
    }

    private fun setPreferredUploadInterval(newInterval: StatusUploadInterval) {
        helper.setStatusUploadInterval(newInterval)
        setChipColors(binding.uploadInterval15minsChipLayout)
        setChipColors(binding.uploadInterval30minsChipLayout)
        setChipColors(binding.uploadInterval60minsChipLayout)
        setChipColors(binding.uploadInterval120minsChipLayout)
    }
}
