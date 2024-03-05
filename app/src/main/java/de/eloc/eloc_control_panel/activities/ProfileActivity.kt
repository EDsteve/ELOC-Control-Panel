package de.eloc.eloc_control_panel.activities

import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.decodeBitmap
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.AppState
import de.eloc.eloc_control_panel.old.DataHelper
import de.eloc.eloc_control_panel.databinding.ActivityProfileBinding
import de.eloc.eloc_control_panel.data.helpers.HttpHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.StorageHelper
import java.io.IOException

class ProfileActivity : MediaActivity() {
    private lateinit var binding: ActivityProfileBinding
    private var imagePicker: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var cameraLauncher: ActivityResultLauncher<Intent>? = null
    private var photoUri: Uri? = null
    private var originalProfilePicture = ""
    private val authHelper = AuthHelper.instance

    private enum class ProfileField {
        ProfilePicture,
    }

    private var currentField: ProfileField? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        binding.avatarImageView.setDefaultImageResId(R.drawable.person)
        binding.avatarImageView.setErrorImageResId(R.drawable.person)
        setContentView(binding.root)
        setItemTitles()
        setLaunchers()
        setListeners()
        updateUI(false)
    }

    override fun onResume() {
        super.onResume()
        setItemValues()
    }

    override fun takePhoto() {
        super.takePhoto()
        photoUri = DataHelper.getTempFileUri()
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        cameraLauncher?.launch(intent)
    }

    override fun pickImage() {
        imagePicker?.launch(getPickImageRequest())
    }

    override fun setImage(src: Uri) {
        try {
            var bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.createSource(contentResolver, src).decodeBitmap { _, _ -> }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, src)
            }
            bitmap = downScaleAvatar(bitmap)
            updateUI(true)
            binding.avatarImageView.setImageBitmap(bitmap)
            StorageHelper.instance.uploadProfilePicture(
                authHelper.userId,
                bitmap,
                ::profilePictureUploadCompleted
            )
        } catch (_: IOException) {
        }
    }

    private fun profilePictureUploadCompleted(url: String) {
        if (url.isEmpty()) {
            showModalAlert(
                getString(R.string.oops),
                getString(R.string.upload_profile_picture_error)
            ) {
                binding.avatarImageView.setImageUrl(
                    originalProfilePicture,
                    HttpHelper.instance.imageLoader
                )
            }
        } else {
            currentField = ProfileField.ProfilePicture
            updateField(url)
        }
    }

    private fun setItemTitles() {
        binding.userIdItem.titleTextView.setText(R.string.user_id)
        binding.userIdItem.chevron.visibility = View.GONE
        binding.userIdItem.button.background = null
    }

    private fun setItemValues() {
        updateUI(false)
        originalProfilePicture = AppState.profilePictureUrl
        binding.avatarImageView.setImageUrl(
            AppState.profilePictureUrl,
            HttpHelper.instance.imageLoader
        )
        binding.userIdItem.valueTextView.text = AppState.rangerName
    }

    private fun updateField(value: String) {
        updateUI(true)
        if (currentField == ProfileField.ProfilePicture) {
            FirestoreHelper.instance.updateProfilePicture(value, authHelper.userId) {
                runOnUiThread { updateUI(false) }
            }
        }
        currentField = null
    }

    private fun updateUI(showProgress: Boolean) {
        if (showProgress) {
            binding.fieldsLayout.visibility = View.GONE
            binding.progressIndicator.visibility = View.VISIBLE
        } else {
            binding.fieldsLayout.visibility = View.VISIBLE
            binding.progressIndicator.visibility = View.GONE
        }
    }

    private fun setListeners() {
        binding.cameraButton.setOnClickListener { takePhoto() }
        binding.galleryButton.setOnClickListener { pickImage() }
        binding.toolbar.setNavigationOnClickListener { goBack() }
    }

    private fun setLaunchers() {
        imagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
            if (it != null) {
                setImage(it)
            }
        }

        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if ((it != null) && (it.resultCode == RESULT_OK)) {
                    if (photoUri != null) {
                        setImage(photoUri!!)
                        photoUri = null
                    }
                }
            }
    }
}