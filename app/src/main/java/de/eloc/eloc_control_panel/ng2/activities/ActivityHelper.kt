package de.eloc.eloc_control_panel.ng2.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import de.eloc.eloc_control_panel.ng3.App
import de.eloc.eloc_control_panel.ng2.interfaces.VoidCallback
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.LayoutAppChipBinding
import de.eloc.eloc_control_panel.ng3.activities.ThemableActivity

object ActivityHelper {

    fun showStatusUpdates(activity: ThemableActivity) {
        openUrl(App.instance.getString(R.string.status_updates_url), activity)
    }

    fun showInstructions(activity: ThemableActivity) {
        openUrl(App.instance.getString(R.string.instructions_url), activity)
    }

    private fun openUrl(address: String, activity: ThemableActivity) {
        // Note: startActivity() is called outside an Activity
        // be sure you are using the contect from an Activity.
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(address)
            activity.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun showAlert(activity: ThemableActivity, message: String, cancelable: Boolean = true, callback: VoidCallback?) {
        MaterialAlertDialogBuilder(activity)
                .setCancelable(cancelable)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    callback?.handler()
                    dialog.dismiss()
                }
                .show()
    }

    // todo: replace with showDialog(), which shows a Material3 UI alert
    fun showAlert(activity: ThemableActivity, message: String) {
        MaterialAlertDialogBuilder(activity)
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

    fun setChipColors(context: Context, chipBinding: LayoutAppChipBinding) {
        val colorTextOnPrimary = ContextCompat.getColor(context, R.color.colorTextOnPrimary)
        val colorTextOnPrimaryTransclucent = ContextCompat.getColor(context, R.color.colorTextOnPrimaryTranslucent)
        if (chipBinding.chip.isChecked) {
            chipBinding.chip.setTextColor(colorTextOnPrimary)
            chipBinding.chip.setChipBackgroundColorResource(R.color.colorPrimary)
        } else {
            chipBinding.chip.setTextColor(colorTextOnPrimaryTransclucent)
            chipBinding.chip.setChipBackgroundColorResource(R.color.colorPrimaryTranslucent)
        }
    }
}