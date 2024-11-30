package de.eloc.eloc_control_panel.activities.themable

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.ActivityCommandLineBinding
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.showModalOptionAlert
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.activities.hideKeyboard
import de.eloc.eloc_control_panel.activities.showInstructions

class CommandLineActivity : ThemableActivity() {
    private lateinit var binding: ActivityCommandLineBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommandLineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { goBack() }
        binding.instructionsButton.setOnClickListener { showInstructions() }
        binding.sendButton.setOnClickListener { sendCommand() }
        binding.deleteButton.setOnClickListener { clearLog() }
        DeviceDriver.setCommandLineListener(::logResponse)
        try {
            val monospaceFont =
                Typeface.createFromAsset(assets, "Courier_Prime/CourierPrime-Regular.ttf")
            binding.outputBox.typeface = monospaceFont
        } catch (_: Exception) {
        }
        showContent()
    }

    override fun onDestroy() {
        super.onDestroy()
        DeviceDriver.clearCommandLineListener()
    }

    private fun clearLog() {
        showModalOptionAlert(
            getString(R.string.confirm),
            getString(R.string.confirm_clear_log),
            getString(R.string.yes),
            positiveCallback = {
                binding.outputBox.setText("")
            }
        )
    }

    private fun sendCommand() {
        val command = binding.commandEditText.text.toString().trim().ifEmpty {
            showModalAlert(
                getString(R.string.required),
                getString(R.string.command_required)
            )
            return
        }

        showProgress()
        appendLog("Command: $command\n\n")
        binding.commandEditText.setText("")
        DeviceDriver.sendCustomCommand(command)
    }

    private fun logResponse(json: String) {
        runOnUiThread {
            showContent()
            val text = json.trim()
            if (text.isNotEmpty()) {
                appendLog("Response: \n$text\n\n\n\n")
            }
        }
    }

    private fun showProgress() {
        hideKeyboard()
        binding.progressIndicator.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
    }

    private fun showContent() {
        binding.progressIndicator.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
    }

    private fun appendLog(text: String) {
        val newText = buildString {
            append(binding.outputBox.text)
            append(text)
        }
        binding.outputBox.setText(newText)
        binding.outputBox.requestLayout()
    }
}