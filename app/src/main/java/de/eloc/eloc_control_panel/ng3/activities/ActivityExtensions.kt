package de.eloc.eloc_control_panel.ng3.activities

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.LayoutAlertOkBinding
import de.eloc.eloc_control_panel.databinding.LayoutAlertOptionBinding
import de.eloc.eloc_control_panel.databinding.LayoutAppChipBinding
import de.eloc.eloc_control_panel.ng2.interfaces.VoidCallback

fun AppCompatActivity.getPickImageRequest(): PickVisualMediaRequest =
    PickVisualMediaRequest.Builder()
        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
        .build()

fun AppCompatActivity.openSystemAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    startActivity(intent)
}

fun AppCompatActivity.openLocationSettings() {
    val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    startActivity(settingsIntent)
}

fun AppCompatActivity.setChipColors(chipBinding: LayoutAppChipBinding) {
    val colorTextOnPrimary = ContextCompat.getColor(this, R.color.colorTextOnPrimary)
    val colorTextOnPrimaryTransclucent =
        ContextCompat.getColor(this, R.color.colorTextOnPrimaryTranslucent)
    if (chipBinding.chip.isChecked) {
        chipBinding.chip.setTextColor(colorTextOnPrimary)
        chipBinding.chip.setChipBackgroundColorResource(R.color.colorPrimary)
    } else {
        chipBinding.chip.setTextColor(colorTextOnPrimaryTransclucent)
        chipBinding.chip.setChipBackgroundColorResource(R.color.colorPrimaryTranslucent)
    }
}

fun AppCompatActivity.open(target: Class<*>, finishTask: Boolean = false) {
    val intent = Intent(this, target)
    startActivity(intent)
    if (finishTask) {
        finish()
    }
}

fun AppCompatActivity.hideKeyboard() {
    try {
        val binder = currentFocus?.windowToken
        if (binder != null) {
            val manager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            manager?.hideSoftInputFromWindow(binder, 0)
        }
    } catch (_: Exception) {
        // It an error occurs trying to hide the keyboard,
        // just ignore the error without crashing app.
    }
}

fun AppCompatActivity.overrideGoBack(callback: VoidCallback?) {
    onBackPressedDispatcher.addCallback(
        this,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                callback?.handler()
            }
        },
    )
}

fun ComponentActivity.goBack() {
    onBackPressedDispatcher.onBackPressed()
}

fun AppCompatActivity.showModalAlert(title: String, message: String) =
    showAlert(title, message)

fun AppCompatActivity.showModalAlert(
    title: String,
    message: String,
    callback: VoidCallback? = null
) = showAlert(title, message, callback)

private fun AppCompatActivity.showAlert(
    title: String,
    message1: String = "",
    callback: VoidCallback? = null
) {
    val dialogMessage = message1.trim().ifEmpty {
        return
    }
    val binding = LayoutAlertOkBinding.inflate(layoutInflater)
    val dialog = Dialog(this)
    dialog.setContentView(binding.root)
    binding.titleTextView.text = title
    binding.messageTextView.text = dialogMessage
    binding.okButton.setOnClickListener {
        dialog.dismiss()
        callback?.handler()
    }
    dialog.setCancelable(false)
    dialog.show()
}

fun AppCompatActivity.showModalOptionAlert(
    title: String,
    message: String,
    positiveCallback: VoidCallback?
) {
    showModalOptionAlert(
        title = title,
        message = message,
        positiveCallback = positiveCallback
    )
}

fun AppCompatActivity.showModalOptionAlert(
    title: String,
    message: String = "",
    positiveButtonLabel: String = "",
    negativeButtonLabel: String = "",
    positiveCallback: VoidCallback?,
    negativeCallback: VoidCallback?,
) {
    val dialogMessage = message.trim()
    if (dialogMessage.isEmpty()) {
        return
    }
    val binding = LayoutAlertOptionBinding.inflate(layoutInflater)
    val dialog = Dialog(this)
    dialog.setContentView(binding.root)
    val positiveLabel = positiveButtonLabel.trim().ifEmpty {
        getString(android.R.string.ok)
    }
    val negativeLabel = negativeButtonLabel.trim().ifEmpty {
        getString(android.R.string.cancel)
    }
    binding.titleTextView.text = title
    binding.messageTextView.text = dialogMessage
    binding.positiveButton.text = positiveLabel
    binding.negativeButton.text = negativeLabel
    binding.negativeButton.setOnClickListener {
        dialog.dismiss()
        negativeCallback?.handler()
    }
    binding.positiveButton.setOnClickListener {
        dialog.dismiss()
        positiveCallback?.handler()
    }
    dialog.setCancelable(false)
    dialog.show()
}