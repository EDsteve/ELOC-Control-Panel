package de.eloc.eloc_control_panel.activities.themable

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.eloc.eloc_control_panel.data.util.Preferences

abstract class ThemableActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(Preferences.preferredFontThemeID)
    }
}