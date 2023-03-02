package de.eloc.eloc_control_panel.ng2.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.ng2.App

object ActivityHelper {
    fun showStatusUpdates() {
        openUrl(App.instance.getString(R.string.status_updates_url))
    }

    fun showInstructions() {
        openUrl(App.instance.getString(R.string.instructions_url))
    }

    private fun openUrl(address: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(address)
        App.instance.startActivity(intent)
    }

    fun showAlert(activity: AppCompatActivity, message: String) {
        AlertDialog.Builder(activity)
            .setCancelable(true)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun showSnack(coordinator: CoordinatorLayout, message: String) =
        Snackbar
            .make(coordinator, message, Snackbar.LENGTH_LONG)
            .show()

    fun hideKeyboard(activity: AppCompatActivity) {
        try {
            val binder = activity.currentFocus?.windowToken
            if (binder != null) {
                val manager =
                    activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                manager?.hideSoftInputFromWindow(binder, 0)
            }
        } catch (_: Exception) {
            // It an error occurs trying to hide the keyboard,
            // just ignore the error without crashing app.
        }
    }
}