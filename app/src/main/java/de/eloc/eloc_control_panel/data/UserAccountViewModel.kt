package de.eloc.eloc_control_panel.data

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.eloc.eloc_control_panel.activities.LoginActivity
import de.eloc.eloc_control_panel.interfaces.BooleanCallback
import de.eloc.eloc_control_panel.interfaces.GoogleSignInCallback
import de.eloc.eloc_control_panel.interfaces.ProfileCheckCallback
import de.eloc.eloc_control_panel.interfaces.StringCallback
import de.eloc.eloc_control_panel.interfaces.VoidCallback

class UserAccountViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        var rangerName: String = ""
    }

    private val repository = UserAccountRepository.instance

    private val userProfileLiveData = MutableLiveData<UserProfile?>()

    val profile: LiveData<UserProfile?> = userProfileLiveData

    val isSignedIn get() = repository.isSignedIn

    val emailAddress get() = repository.emailAddress

    val isEmailVerified get() = repository.isUserEmailVerified

    fun getProfileAsync(
        offlineProfile: Boolean,
        viewModel: UserAccountViewModel,
        uiCallback: VoidCallback?
    ) {
        repository.getProfile(offlineProfile, viewModel, userProfileLiveData::setValue, uiCallback)
    }

    suspend fun signOut() = repository.signOut()

    fun sendEmailVerificationLink(callback: BooleanCallback) =
        repository.sendEmailVerificationLink(callback)

    fun deleteRemoteFiles(callback: BooleanCallback) = repository.deleteRemoteFiles(callback)

    fun deleteProfile(callback: BooleanCallback) = repository.deleteProfile(callback)

    fun deleteAuthAccount(callback: BooleanCallback) = repository.deleteAuthAccount(callback)

    fun verifyPassword(password: String, callback: StringCallback) =
        repository.verifyPassword(password, callback)

    fun uploadProfilePicture(bitmap: Bitmap, callback: StringCallback) =
        repository.uploadProfilePicture(bitmap, callback)

    fun updateProfilePicture(url: String, viewModel: UserAccountViewModel, callback: VoidCallback) =
        repository.updateProfilePicture(url, viewModel, callback)

    fun changeEmailAddress(emailAddress: String, password: String, callback: StringCallback) =
        repository.changeEmailAddress(emailAddress, password, callback)

    fun changePassword(newPassword: String, oldPassword: String, callback: StringCallback) =
        repository.changePassword(newPassword, oldPassword, callback)

    suspend fun signInWithGoogle(
        activity: LoginActivity,
        filter: Boolean,
        callback: GoogleSignInCallback?
    ) =
        repository.signInWithGoogle(activity, filter, callback)

    fun register(email: String, password: String, callback: StringCallback) =
        repository.register(email, password, callback)

    fun updateProfile(
        data: HashMap<String, Any>,
        viewModel: UserAccountViewModel,
        callback: BooleanCallback
    ) =
        repository.updateProfile(data, viewModel, callback)

    fun hasProfile(callback: ProfileCheckCallback) =
        repository.hasProfile(callback)

    fun userIdExists(userId: String, callback: BooleanCallback) =
        repository.userIdExists(userId, callback)

    fun signIn(emailAddress: String, password: String, callback: StringCallback) =
        repository.signIn(emailAddress, password, callback)

    fun sendPasswordResetLink(emailAddress: String, callback: BooleanCallback) =
        repository.sendPasswordResetLink(emailAddress, callback)

    fun saveRangerName(name: String) {
        userProfileLiveData.value?.userId = name
        rangerName = name
    }

    fun saveProfilePictureUrl(url: String) {
        userProfileLiveData.value?.profilePictureUrl = url
    }
}