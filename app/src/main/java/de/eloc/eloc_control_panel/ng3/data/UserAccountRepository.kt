package de.eloc.eloc_control_panel.ng3.data

import android.content.Intent
import android.graphics.Bitmap
import de.eloc.eloc_control_panel.data.firebase.FirestoreHelper
import de.eloc.eloc_control_panel.data.firebase.StorageHelper
import de.eloc.eloc_control_panel.ng3.interfaces.BooleanCallback
import de.eloc.eloc_control_panel.ng3.interfaces.ProfileCallback
import de.eloc.eloc_control_panel.ng3.interfaces.StringCallback
import de.eloc.eloc_control_panel.ng3.interfaces.VoidCallback
import de.eloc.eloc_control_panel.ng3.data.firebase.AuthHelper
import kotlin.collections.HashMap

class UserAccountRepository {

    companion object {

        val instance get() = UserAccountRepository()

    }

    private val firestoreHelper: FirestoreHelper
    private val authHelper: AuthHelper
    private val storageHelper: StorageHelper

    val emailAddress get() = authHelper.emailAddress

    val googleSignInIntent get() = authHelper.googleSignInIntent

    val isSignedIn get() = authHelper.isSignedIn

    val isUserEmailVerified get() = authHelper.isEmailAddressVerified

    init {
        firestoreHelper = FirestoreHelper.getInstance();
        authHelper = AuthHelper.instance
        storageHelper = StorageHelper.getInstance();
    }

    fun signOut() = authHelper.signOut()

    fun signInWithGoogle(data: Intent?, callback: BooleanCallback) {
        if (data != null) {
            authHelper.signInWithGoogle(data, callback)
        }
    }

    fun register(email: String, password: String, callback: StringCallback) =
        authHelper.register(email, password, callback)

    fun updateProfile(data: HashMap<String, Any>, callback: BooleanCallback) =
        firestoreHelper.updateProfile(authHelper.userId, data, callback)

    fun hasProfile(callback: BooleanCallback) =
        firestoreHelper.hasProfile(authHelper.userId, callback)

    fun userIdExists(userId: String, callback: BooleanCallback) =
        firestoreHelper.userIdExists(userId, callback)

    fun signIn(emailAddress: String, password: String, callback: StringCallback) =
        authHelper.signIn(emailAddress, password, callback)

    fun sendPasswordResetLink(emailAddress: String, callback: BooleanCallback) =
        authHelper.sendResetLink(emailAddress, callback)

    fun sendEmailVerificationLink(callback: BooleanCallback) {
        authHelper.sendVerificationLink(callback);
    }

    fun getProfile(callback: ProfileCallback) =
        firestoreHelper.getProfile(authHelper.userId, emailAddress, callback);

    fun uploadProfilePicture(bitmap: Bitmap, callback: StringCallback) =
        storageHelper.uploadProfilePicture(authHelper.userId, bitmap, callback)

    fun updateProfilePicture(url: String, callback: VoidCallback) =
        firestoreHelper.updateProfilePicture(url, authHelper.userId, callback)

    fun changeEmailAddress(emailAddress: String, password: String, callback: StringCallback) =
        authHelper.changeEmailAddress(emailAddress, password, callback)

    fun changePassword(newPassword: String, oldPassword: String, callback: StringCallback) =
        authHelper.changePassword(newPassword, oldPassword, callback)

    fun deleteRemoteFiles(callback: BooleanCallback) =
        storageHelper.deleteAccount(authHelper.userId, callback)

    fun deleteProfile(callback: BooleanCallback) =
        firestoreHelper.deleteProfile(authHelper.userId, callback)

    fun deleteAuthAccount(callback: BooleanCallback) = authHelper.deleteAccount(callback)

    fun verifyPassword(password: String, callback: StringCallback) =
        authHelper.reauthenticate(password, callback)
}
//140