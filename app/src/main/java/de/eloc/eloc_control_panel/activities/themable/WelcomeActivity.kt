package de.eloc.eloc_control_panel.activities.themable

import android.os.Bundle
import de.eloc.eloc_control_panel.activities.openActivity
import de.eloc.eloc_control_panel.databinding.ActivityWelcomeBinding

class WelcomeActivity : ThemableActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            openActivity(LoginActivity::class.java, finishTask = true)
        }
        binding.registerButton.setOnClickListener {
            openActivity(RegisterActivity::class.java, finishTask = true)
        }
    }
}