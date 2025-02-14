package de.eloc.eloc_control_panel.activities.themable.media

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.StorageHelper
import de.eloc.eloc_control_panel.databinding.ActivityProfileSetupBinding
import de.eloc.eloc_control_panel.interfaces.TextInputWatcher
import de.eloc.eloc_control_panel.old.DataHelper
import de.eloc.eloc_control_panel.activities.getPickImageRequest
import de.eloc.eloc_control_panel.activities.hideKeyboard
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.activities.openActivity
import de.eloc.eloc_control_panel.activities.themable.LoadProfileActivity

class ProfileSetupActivity : MediaActivity() {
    private lateinit var binding: ActivityProfileSetupBinding
    private lateinit var imagePicker: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private var avatar: Bitmap? = null
    private var photoUri: Uri? = null
    private val data = HashMap<String, Any>()
    private val authHelper = AuthHelper.instance
    private val storageHelper = StorageHelper.instance
    private val firestoreHelper = FirestoreHelper.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setLaunchers()
        setListeners()
        setTextWatchers()
        updateUI(false)
    }

    override fun takePhoto() {
        super.takePhoto()
        if (hasCameraPermission) {
            photoUri = DataHelper.getTempFileUri()
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraLauncher.launch(intent)
        }
    }

    override fun pickImage() {
        imagePicker.launch(getPickImageRequest())
    }

    override fun setImage(src: Uri) {
        avatar = null
        try {
            var bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val imageSource = ImageDecoder.createSource(contentResolver, src)
                ImageDecoder.decodeBitmap(imageSource)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, src)
            }
            bitmap = downScaleAvatar(bitmap)
            avatar = bitmap
        } catch (ignore: Exception) {
        }
        binding.avatarImageView.setImageBitmap(avatar)
    }

    private fun setTextWatchers() =
        binding.userIdTextInput.addTextChangedListener(TextInputWatcher(binding.userIdLayout))

    private fun setListeners() {
        binding.galleryButton.setOnClickListener { pickImage() }
        binding.cameraButton.setOnClickListener { takePhoto() }
        binding.root.setOnClickListener { hideKeyboard() }
        binding.doneButton.setOnClickListener { saveProfile() }
    }

    private fun setLaunchers() {
        imagePicker = registerForActivityResult(PickVisualMedia()) { result ->
            if (result != null) {
                setImage(result)
            }
        }

        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                if (photoUri != null) {
                    setImage(photoUri!!)
                    photoUri = null
                }
            }
        }
    }

    private fun updateUI(lock: Boolean) {
        if (lock) {
            binding.cameraButton.isEnabled = false
            binding.galleryButton.isEnabled = false
            binding.userIdLayout.isEnabled = false
            binding.progressIndicator.visibility = View.VISIBLE
            binding.doneButton.visibility = View.INVISIBLE
            binding.galleryButton.alpha = 0.25f
            binding.cameraButton.alpha = 0.25f
        } else {
            binding.cameraButton.isEnabled = true
            binding.galleryButton.isEnabled = true
            binding.userIdLayout.isEnabled = true
            binding.progressIndicator.visibility = View.GONE
            binding.doneButton.visibility = View.VISIBLE
            binding.galleryButton.alpha = 1f
            binding.cameraButton.alpha = 1f
        }
    }

    private fun saveProfile() {
        binding.userIdLayout.error = null
        val userId = binding.userIdTextInput.editableText?.toString()?.trim() ?: ""
        if (userId.isEmpty()) {
            binding.userIdLayout.error = getString(R.string.set_your_desired_user_id)
            return
        } else if (userId.length < 6) {
            binding.userIdLayout.error = getString(R.string.user_id_is_too_short)
            return
        } else if (!userIdHasValidChars(userId)) {
            binding.userIdLayout.error = getString(R.string.user_id_has_invalid_characters)
            return
        }

        updateUI(true)
        data[FirestoreHelper.FIELD_USER_ID] = userId
        data[FirestoreHelper.FIELD_PROFILE_PICTURE] = ""
        firestoreHelper.userIdExists(userId, ::onUserIdChecked)
    }

    private fun userIdHasValidChars(userId: String): Boolean {
        val max = userId.length - 1
        for (i in 0..max) {
            val c = userId[i]
            if ((!Character.isLetterOrDigit(c)) && (c != '_')) {
                return false
            }
        }
        return true
    }

    private fun onUserIdChecked(exists: Boolean) {
        if (exists) {
            binding.userIdLayout.error = getString(R.string.user_id_is_already_taken)
            updateUI(false)
        } else {
            if (avatar != null) {
                storageHelper.uploadProfilePicture(
                    authHelper.userId,
                    avatar!!,
                    this::onProfilePictureUploaded
                )
            } else {
                uploadProfile(data)
            }
        }
    }

    private fun onProfilePictureUploaded(url: String) {
        val address = url.trim()
        if (address.isEmpty()) {
            updateUI(false)
            showModalAlert(
                getString(R.string.oops),
                getString(R.string.profile_picture_upload_error)
            )
            return
        }
        data[FirestoreHelper.FIELD_PROFILE_PICTURE] = address
        uploadProfile(data)
    }

    private fun uploadProfile(data: HashMap<String, Any>) =
        firestoreHelper.updateProfile(authHelper.userId, data, this::onProfileUploadCompleted)

    private fun onProfileUploadCompleted(success: Boolean) {
        updateUI(false)
        if (success) {
            openActivity(LoadProfileActivity::class.java, true)
        } else {
            showModalAlert(
                getString(R.string.oops),
                getString(R.string.something_went_wrong)
            )
        }
    }
}