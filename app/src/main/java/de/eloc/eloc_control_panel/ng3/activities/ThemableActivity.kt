package de.eloc.eloc_control_panel.ng3.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.eloc.eloc_control_panel.ng2.models.PreferencesHelper

abstract class ThemableActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(PreferencesHelper.instance.getPreferredFontThemeID())
    }
}