package de.eloc.eloc_control_panel.ng2.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.ng2.models.PreferencesHelper
import de.eloc.eloc_control_panel.ng2.models.PreferredFontSize

abstract class ThemableActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fontSize = PreferencesHelper.instance.getPreferredFontSize()
        when (PreferredFontSize.fromInt(fontSize)) {
            PreferredFontSize.small -> setTheme(R.style.AppTheme)
            PreferredFontSize.medium -> setTheme(R.style.AppThemeMedium)
            PreferredFontSize.large -> setTheme(R.style.AppThemeLarge)
            null -> {}
        }
    }
}