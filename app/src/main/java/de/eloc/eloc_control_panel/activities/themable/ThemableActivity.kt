package de.eloc.eloc_control_panel.activities.themable

import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import de.eloc.eloc_control_panel.data.util.Preferences

abstract class ThemableActivity : AppCompatActivity() {

    private var active = false
    private var appliedInsets = false

    val isActive get(): Boolean = active

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(Preferences.preferredFontThemeID)


    }

    override fun onResume() {
        super.onResume()
        active = true

        if (!appliedInsets) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Apply top padding
                val rootView = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
                rootView.setOnApplyWindowInsetsListener { v, insets ->
                    val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                    v.setPadding(0, statusBarInsets.top, 0, 0)
                    appliedInsets = true
                    insets
                }
                window.decorView.requestApplyInsets()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        active = false
    }
}