package de.eloc.eloc_control_panel.data.helpers.firebase

import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.openActivity
import de.eloc.eloc_control_panel.activities.themable.ThemableActivity
import de.eloc.eloc_control_panel.activities.themable.WelcomeActivity
import de.eloc.eloc_control_panel.data.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthHelper {

    companion object {
        val instance: AuthHelper = AuthHelper()
    }

    private val user: FirebaseUser? get() = FirebaseAuth.getInstance().currentUser

    val isSignedIn get() = user != null

    val emailAddress get() = user?.email ?: ""

    val isEmailAddressVerified get() = user?.isEmailVerified ?: false

    val userId get() = user?.uid ?: ""

    private val defaultErrorMessage get() = App.instance.getString(R.string.something_went_wrong)

    fun register(email: String, password: String, callback: (String) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user?.sendEmailVerification()?.addOnCompleteListener { callback("") }
            } else {
                val message =
                    task.exception?.message ?: App.instance.getString(R.string.something_went_wrong)
                callback(message)
            }
        }
    }

    fun signIn(email: String, password: String, callback: (String) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            val error = if (!task.isSuccessful) {
                val context = App.instance
                val exception = task.exception
                if ((exception is FirebaseAuthInvalidCredentialsException) || (exception is FirebaseAuthInvalidUserException)) {
                    context.getString(R.string.invalid_email_address_or_password)
                } else {
                    exception?.message ?: context.getString(R.string.something_went_wrong)
                }
            } else {
                ""
            }
            callback(error)
        }
    }

    fun signOut(activity: ThemableActivity) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                Preferences.clearProfileData()
                FirebaseAuth.getInstance().signOut()
                activity.openActivity(WelcomeActivity::class.java, true)
            }
        }
    }

    fun sendResetLink(emailAddress: String, callback: (Boolean) -> Unit) {
        val address = emailAddress.trim()
        if (address.isEmpty()) {
            callback(false)
        } else {
            FirebaseAuth.getInstance().sendPasswordResetEmail(address)
                .addOnCompleteListener {
                    callback(it.isSuccessful)
                }
        }
    }

    fun sendVerificationLink(callback: (Boolean) -> Unit) {
        if (user != null) {
            user?.sendEmailVerification()?.addOnCompleteListener {
                callback(it.isSuccessful)
            }
        } else {
            callback(false)
        }
    }

    fun reauthenticate(password: String, callback: (String) -> Unit) {
        if (user == null) {
            callback(defaultErrorMessage)
        } else {
            val credential = EmailAuthProvider.getCredential(emailAddress, password)
            user!!.reauthenticate(credential).addOnCompleteListener {
                val error = if (!it.isSuccessful) {
                    App.instance.getString(R.string.invalid_password)
                } else {
                    ""
                }
                callback(error)
            }
        }
    }

    fun changeEmailAddress(
        newEmailAddress: String,
        password: String,
        callback: (String) -> Unit
    ) {
        reauthenticate(password) { error ->
            val err = error.trim()
            if (err.isEmpty()) {
                changeEmailAfterReauth(newEmailAddress, callback)
            } else {
                callback(error)
            }
        }
    }

    private fun changeEmailAfterReauth(newEmailAddress: String, callback: (String) -> Unit) {
        if (user == null) {
            callback(defaultErrorMessage)
        } else {
            user!!.verifyBeforeUpdateEmail(newEmailAddress).addOnCompleteListener { task ->
                val error = if (task.isSuccessful) {
                    user?.sendEmailVerification()
                    ""
                } else {
                    var error = task.exception?.localizedMessage ?: defaultErrorMessage
                    if (error.lowercase().contains("invalid")) {
                        error = App.instance.getString(R.string.invalid_email_address)
                    }
                    error
                }
                callback(error)
            }
        }
    }

    fun changePassword(newPassword: String, oldPassword: String, callback: (String) -> Unit) {
        reauthenticate(oldPassword) { error ->
            val err = error.trim()
            if (err.isEmpty()) {
                changePasswordAfterReauth(newPassword, callback)
            } else {
                callback(error)
            }
        }
    }

    private fun changePasswordAfterReauth(newPassword: String, callback: (String) -> Unit) {
        if (user == null) {
            callback(defaultErrorMessage)
        } else {
            user?.updatePassword(newPassword)?.addOnCompleteListener { task ->
                val error = if (!task.isSuccessful) {
                    task.exception?.localizedMessage ?: defaultErrorMessage
                } else {
                    ""
                }
                callback(error)
            }
        }
    }

    fun deleteAccount(callback: (Boolean) -> Unit) {
        if (user == null) {
            callback(false)
        } else {
            user?.delete()?.addOnCompleteListener {
                callback(it.isSuccessful)
            }
        }
    }
}