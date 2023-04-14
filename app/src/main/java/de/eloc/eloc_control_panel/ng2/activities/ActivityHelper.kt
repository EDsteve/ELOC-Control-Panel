package de.eloc.eloc_control_panel.ng2.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.ng2.App

object ActivityHelper {
    fun showStatusUpdates( activity: AppCompatActivity) {
        openUrl(App.instance.getString(R.string.status_updates_url), activity)
    }

    fun showInstructions( activity: AppCompatActivity) {
        openUrl(App.instance.getString(R.string.instructions_url), activity)
    }

    private fun openUrl(address: String, activity: AppCompatActivity) {
        // Note: startActivity() is called outside an Activity
        // be sure you are using the contect from an Activity.
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(address)
            activity.startActivity(intent)
        } catch (_: Exception) {
        }
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

    fun getPrettifiedDuration(duration: Double): String {
        val hoursPerDay = 24
        val hours = (duration % hoursPerDay).toInt()
        val days = ((duration - hours) / hoursPerDay).toInt()
        val minutes = ((duration - (days * hoursPerDay) - hours) * 60).toInt()
        var prettyDays = ""
        if (days == 1) {
            prettyDays = "1 day"
        } else if (days > 1) {
            prettyDays = "$days days"
        }
        var prettyHours = ""
        if (hours == 1) {
            prettyHours = "1 hr"
        } else if (hours > 1) {
            prettyHours = "$hours hrs"
        }
        var prettyMinutes = ""
        if (minutes == 1) {
            prettyMinutes = "1 min"
        } else if (minutes > 1) {
            prettyMinutes = "$minutes mins"
        }
        var result = "$prettyDays $prettyHours $prettyMinutes".trim()
        if (result.isEmpty()) {
            result = "< 1 mins"
        }
        return result
    }
}