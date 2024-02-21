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
import androidx.lifecycle.ViewModelProvider
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.old.DataHelper
import de.eloc.eloc_control_panel.databinding.ActivityProfileBinding
import de.eloc.eloc_control_panel.data.helpers.HttpHelper
import de.eloc.eloc_control_panel.data.UserAccountViewModel
import de.eloc.eloc_control_panel.data.UserProfile
import java.io.IOException

class ProfileActivity : MediaActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: UserAccountViewModel
    private var imagePicker: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var cameraLauncher: ActivityResultLauncher<Intent>? = null
    private var photoUri: Uri? = null
    private var originalProfilePicture = ""

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
        setViewModel()
        setItemTitles()
        setLaunchers()
        setListeners()
        updateUI(false)
        viewModel.getProfileAsync(false, null)
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
            viewModel.uploadProfilePicture(bitmap, this::profilePictureUploadCompleted)
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

    private fun setViewModel() {
        viewModel = ViewModelProvider(this)[UserAccountViewModel::class.java]
        viewModel.profile.observe(this, this::setItemValues)
    }

    private fun setItemTitles() {
        binding.userIdItem.titleTextView.setText(R.string.user_id)
        binding.userIdItem.chevron.visibility = View.GONE
        binding.userIdItem.button.background = null
    }

    private fun setItemValues(profile: UserProfile?) {
        updateUI(false)
        originalProfilePicture = profile?.profilePictureUrl ?: ""
        binding.avatarImageView.setImageUrl(
            profile?.profilePictureUrl,
            HttpHelper.instance.imageLoader
        )
        binding.userIdItem.valueTextView.text = profile?.userId
    }

    private fun updateField(value: String) {
        val completedCallback = fun() {
            updateUI(false)
            viewModel.getProfileAsync(false, null)
        }
        updateUI(true)
        if (currentField == ProfileField.ProfilePicture) {
            viewModel.updateProfilePicture(value, completedCallback)
        }
        currentField = null
    }

    private fun updateUI(showProgress: Boolean) {
        if (showProgress) {
            binding.fieldsLayout.visibility = View.GONE
            binding.progressLayout.visibility = View.VISIBLE
        } else {
            binding.fieldsLayout.visibility = View.VISIBLE
            binding.progressLayout.visibility = View.GONE
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