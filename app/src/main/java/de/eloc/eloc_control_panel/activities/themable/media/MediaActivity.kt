package de.eloc.eloc_control_panel.activities.themable.media

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.annotation.CallSuper
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.themable.ThemableActivity
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.activities.showModalOptionAlert
import de.eloc.eloc_control_panel.activities.openSystemAppSettings

private const val CAMERA_PERMISSION = Manifest.permission.CAMERA

abstract class MediaActivity : ThemableActivity() {
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>

    val hasCameraPermission: Boolean
        get() {
            val status = ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION)
            return (status == PackageManager.PERMISSION_GRANTED)
        }

    abstract fun setImage(src: Uri)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                println(result)
            }
    }

    @CallSuper
    open fun takePhoto() {
        checkCameraPermission()
    }

    abstract fun pickImage()

    private fun checkCameraPermission() {
        if (!hasCameraPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(CAMERA_PERMISSION)) {
                    val title = getString(R.string.permission_required)
                    val appName = getString(R.string.app_name)
                    val message = getString(R.string.camera_permission_rationale, appName)
                    val positiveButtonText = getString(R.string.yes)
                    val negativeButtonText = getString(android.R.string.cancel)
                    showModalOptionAlert(
                        title,
                        message,
                        positiveButtonText,
                        negativeButtonText,
                        {
                            askCameraPermission()
                            Preferences.setCameraRequested()
                        },
                        {
                            Preferences.setCameraRequested()
                        },
                    )
                } else {
                    askCameraPermission()
                }
            } else {
                askCameraPermission()
            }
        }
    }

    private fun askCameraPermission() {
        if (Preferences.cameraRequested) {
            val title = getString(R.string.permission_required)
            val message = getString(R.string.manual_camera)
            val positiveButtonText = getString(R.string.open_settings)
            val negativeButtonText = getString(android.R.string.cancel)
            showModalOptionAlert(
                title,
                message,
                positiveButtonText,
                negativeButtonText,
                { openSystemAppSettings() },
                {},
            )
        } else {
            cameraPermissionLauncher.launch(CAMERA_PERMISSION)
        }
    }

    fun downScaleAvatar(bitmap: Bitmap): Bitmap {
        val maxDimension = 800f
        val bigDimension = kotlin.math.max(bitmap.width, bitmap.height).toFloat()
        return if (bigDimension > maxDimension) {
            val scaleFactor = maxDimension / bigDimension
            val matrix = Matrix()
            matrix.setScale(scaleFactor, scaleFactor)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
        } else {
            bitmap
        }
    }
}
