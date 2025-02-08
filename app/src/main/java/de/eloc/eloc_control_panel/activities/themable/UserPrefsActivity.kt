package de.eloc.eloc_control_panel.activities.themable

import android.os.Bundle
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.activities.themable.editors.preferences.OptionEditorActivity
import de.eloc.eloc_control_panel.activities.themable.editors.preferences.RangeEditorActivity
import de.eloc.eloc_control_panel.data.MainMenuPosition
import de.eloc.eloc_control_panel.databinding.ActivityUserPrefsBinding
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.data.PreferredFontSize
import de.eloc.eloc_control_panel.data.StatusUploadInterval

class UserPrefsActivity : ThemableActivity() {

    private lateinit var binding: ActivityUserPrefsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserPrefsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
    }

    override fun onResume() {
        super.onResume()
        loadPrefs()
    }

    private fun setListeners() {
        binding.toolbar.setNavigationOnClickListener { goBack() }

        binding.fontSizeItem.setOnClickListener {
            showModalAlert(getString(R.string.important), getString(R.string.font_size_notice)) {
                val options = PreferredFontSize.entries.map { "${it.code}|$it" }
                OptionEditorActivity.open(
                    this,
                    Preferences.PREF_USER_FONT_SIZE,
                    getString(R.string.font_size),
                    Preferences.preferredFontSize.toString(),
                    options
                )
            }
        }

        binding.mainMenuPositionItem.setOnClickListener {
            val options = MainMenuPosition.entries.map { "${it.code}|$it" }
            OptionEditorActivity.open(
                this,
                Preferences.PREF_MAIN_MENU_POSITION,
                getString(R.string.main_menu_position),
                Preferences.mainMenuPosition.toString(),
                options
            )
        }

        binding.uploadIntervalItem.setOnClickListener {
            val options = StatusUploadInterval.entries.map { "${it.seconds}|$it" }
            OptionEditorActivity.open(
                this,
                Preferences.PREF_STATUS_UPLOAD_INTERVAL,
                getString(R.string.retry_upload_interval),
                Preferences.statusUploadInterval.toString(),
                options
            )
        }

        binding.gpsTimeoutItem.setOnClickListener {
            RangeEditorActivity.openRangeEditor(
                this,
                Preferences.PREF_GPS_LOCATION_TIMEOUT,
                getString(R.string.gps_location_timeout),
                "${Preferences.gpsLocationTimeoutSeconds} sec",
                Preferences.gpsLocationTimeoutSeconds.toFloat(),
                Preferences.MIN_GPS_TIMEOUT_SECONDS.toFloat(),
                Preferences.MAX_GPS_TIMEOUT_SECONDS.toFloat()
            )
        }

        binding.logBtTrafficItem.setSwitchClickedListener {
            Preferences.logBtTraffic = (!Preferences.logBtTraffic)
            loadPrefs()
        }
    }

    private fun loadPrefs() {
        binding.fontSizeItem.valueText = Preferences.preferredFontSize.toString()
        binding.mainMenuPositionItem.valueText = Preferences.mainMenuPosition.toString()
        binding.uploadIntervalItem.valueText = Preferences.statusUploadInterval.toString()
        binding.gpsTimeoutItem.valueText = "${Preferences.gpsLocationTimeoutSeconds} secs"
        binding.logBtTrafficItem.setSwitch(Preferences.logBtTraffic)
    }
}
