package de.eloc.eloc_control_panel.ng3.data

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.eloc.eloc_control_panel.ng2.interfaces.BooleanCallback
import de.eloc.eloc_control_panel.ng2.interfaces.StringCallback
import de.eloc.eloc_control_panel.ng2.interfaces.VoidCallback

class UserAccountViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserAccountRepository.instance

    private val userProfileLiveData = MutableLiveData<UserProfile?>()

    val profile: LiveData<UserProfile?> = userProfileLiveData

    val isSignedIn get() = repository.isSignedIn

    val emailAddress get() = repository.emailAddress

    val googleSignInIntent get() = repository.googleSignInIntent

    val isEmailVerified get() = repository.isUserEmailVerified

    fun getProfileAsync() {
        repository.getProfile(userProfileLiveData::setValue)
    }

    fun signOut() = repository.signOut()

    fun sendEmailVerificationLink(callback: BooleanCallback) =
        repository.sendEmailVerificationLink(callback)

    fun deleteRemoteFiles(callback: BooleanCallback) = repository.deleteRemoteFiles(callback)

    fun deleteProfile(callback: BooleanCallback) = repository.deleteProfile(callback)

    fun deleteAuthAccount(callback: BooleanCallback) = repository.deleteAuthAccount(callback)

    fun verifyPassword(password: String, callback: StringCallback) =
        repository.verifyPassword(password, callback)

    fun uploadProfilePicture(bitmap: Bitmap, callback: StringCallback) =
        repository.uploadProfilePicture(bitmap, callback)

    fun updateProfilePicture(url: String, callback: VoidCallback) =
        repository.updateProfilePicture(url, callback)

    fun changeEmailAddress(emailAddress: String, password: String, callback: StringCallback) =
        repository.changeEmailAddress(emailAddress, password, callback)

    fun changePassword(newPassword: String, oldPassword: String, callback: StringCallback) =
        repository.changePassword(newPassword, oldPassword, callback)

    fun signInWithGoogle(data: Intent?, callback: BooleanCallback) =
        repository.signInWithGoogle(data, callback)

    fun register(email: String, password: String, callback: StringCallback) =
        repository.register(email, password, callback)

    fun updateProfile(data: HashMap<String, Any>, callback: BooleanCallback) =
        repository.updateProfile(data, callback)

    fun hasProfile(callback: BooleanCallback) =
        repository.hasProfile(callback)

    fun userIdExists(userId: String, callback: BooleanCallback) =
        repository.userIdExists(userId, callback)

    fun signIn(emailAddress: String, password: String, callback: StringCallback) =
        repository.signIn(emailAddress, password, callback)

    fun sendPasswordResetLink(emailAddress: String, callback: BooleanCallback) =
        repository.sendPasswordResetLink(emailAddress, callback)
}