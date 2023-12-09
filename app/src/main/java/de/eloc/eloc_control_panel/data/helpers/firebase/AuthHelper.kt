package de.eloc.eloc_control_panel.data.helpers.firebase

import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.BuildConfig
import de.eloc.eloc_control_panel.data.helpers.PreferencesHelper
import de.eloc.eloc_control_panel.interfaces.BooleanCallback
import de.eloc.eloc_control_panel.interfaces.StringCallback
import de.eloc.eloc_control_panel.interfaces.VoidCallback

class AuthHelper {

    private var credentialManager = CredentialManager.create(App.instance.applicationContext)
    var googleSignInCanceled = false
        private set
    private var googleSignInListeners = mutableSetOf<VoidCallback>()
    var googleSignInRunning = false
        private set(value) {
            field = value
            for (listener in googleSignInListeners) {
                listener.handler()
            }
        }

    companion object {
        val instance: AuthHelper = AuthHelper()
    }

    private val user: FirebaseUser? get() = FirebaseAuth.getInstance().currentUser

    val isSignedIn get() = user != null

    val emailAddress get() = user?.email ?: ""

    val isEmailAddressVerified get() = user?.isEmailVerified ?: false

    val userId get() = user?.uid ?: ""

    private val defaultErrorMessage get() = App.instance.getString(R.string.something_went_wrong)

    fun clearGoogleSignInCanceled() {
        googleSignInCanceled = false
    }

    fun register(email: String, password: String, callback: StringCallback) {
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user?.sendEmailVerification()?.addOnCompleteListener { callback.handler("") }
            } else {
                val message =
                    task.exception?.message ?: App.instance.getString(R.string.something_went_wrong)
                callback.handler(message)
            }
        }
    }

    fun signIn(email: String, password: String, callback: StringCallback) {
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
            callback.handler(error)
        }
    }

    fun registerGoogleSignInListener(listener: VoidCallback) {
        googleSignInListeners.add(listener)
    }

    suspend fun signInWithGoogle(errorCallback: StringCallback?) {
        if (googleSignInRunning) {
            return
        }
        googleSignInRunning = true
        if (googleSignInCanceled) {
            googleSignInRunning = false
            return
        }
        googleSignInCanceled = false
        try {
            val response = getCredential()
            val credential = response.credential
            if (credential is CustomCredential) {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    googleSignInCanceled = true
                    val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    FirebaseAuth.getInstance().signInWithCustomToken(tokenCredential.idToken)
                        .addOnCompleteListener {
                            PreferencesHelper.instance.setAutoGoogleSignIn(it.isSuccessful)
                            if (it.isSuccessful) {
                                val exception = it.exception
                                if (exception != null) {
                                    // todo remove logged message
                                    println(exception.localizedMessage ?: defaultErrorMessage)
                                }
                            }
                        }
                }
            }
        } catch (_: GetCredentialCancellationException) {
            googleSignInCanceled = true
        } catch (e: NoCredentialException) {
            googleSignInCanceled = true
            if (e.type == android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL) {
                val message = App.instance.getString(R.string.no_google_accounts_found)
                errorCallback?.handler(message)
            } else {
                errorCallback?.handler(e.localizedMessage ?: defaultErrorMessage)
            }
        } catch (e: Exception) {
            errorCallback?.handler(e.localizedMessage ?: defaultErrorMessage)
        }
        googleSignInRunning = false
    }

    private suspend fun getCredential(): GetCredentialResponse {
        val options = GetGoogleIdOption.Builder()
            .setNonce(System.currentTimeMillis().toString())
            .setServerClientId(BuildConfig.GCLOUD_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(options)
            .build()
        return credentialManager.getCredential(App.instance.applicationContext, request)
    }

    private suspend fun clearCredential() {
        PreferencesHelper.instance.setAutoGoogleSignIn(false)
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }

    suspend fun signOut() {
        clearCredential()
        FirebaseAuth.getInstance().signOut()
    }

    fun sendResetLink(emailAddress: String, callback: BooleanCallback) {
        val address = emailAddress.trim()
        if (address.isEmpty()) {
            callback.handler(false)
        } else {
            FirebaseAuth.getInstance().sendPasswordResetEmail(address)
                .addOnCompleteListener {
                    callback.handler(it.isSuccessful)
                }
        }
    }

    fun sendVerificationLink(callback: BooleanCallback) {
        if (user != null) {
            user?.sendEmailVerification()?.addOnCompleteListener {
                callback.handler(it.isSuccessful)
            }
        } else {
            callback.handler(false)
        }
    }

    fun reauthenticate(password: String, callback: StringCallback) {
        if (user == null) {
            callback.handler(defaultErrorMessage)
        } else {
            val credential = EmailAuthProvider.getCredential(emailAddress, password)
            user!!.reauthenticate(credential).addOnCompleteListener {
                val error = if (!it.isSuccessful) {
                    App.instance.getString(R.string.invalid_password)
                } else {
                    ""
                }
                callback.handler(error)
            }
        }
    }

    fun changeEmailAddress(
        newEmailAddress: String,
        password: String,
        callback: StringCallback
    ) {
        reauthenticate(password) { error ->
            val err = error.trim()
            if (err.isEmpty()) {
                changeEmailAfterReauth(newEmailAddress, callback)
            } else {
                callback.handler(error)
            }
        }
    }

    private fun changeEmailAfterReauth(newEmailAddress: String, callback: StringCallback) {
        if (user == null) {
            callback.handler(defaultErrorMessage)
        } else {
            user!!.updateEmail(newEmailAddress).addOnCompleteListener { task ->
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
                callback.handler(error)
            }
        }
    }

    fun changePassword(newPassword: String, oldPassword: String, callback: StringCallback) {
        reauthenticate(oldPassword) { error ->
            val err = error.trim()
            if (err.isEmpty()) {
                changePasswordAfterReauth(newPassword, callback)
            } else {
                callback.handler(error)
            }
        }
    }

    private fun changePasswordAfterReauth(newPassword: String, callback: StringCallback) {
        if (user == null) {
            callback.handler(defaultErrorMessage)
        } else {
            user?.updatePassword(newPassword)?.addOnCompleteListener { task ->
                val error = if (!task.isSuccessful) {
                    task.exception?.localizedMessage ?: defaultErrorMessage
                } else {
                    ""
                }
                callback.handler(error)
            }
        }
    }

    fun deleteAccount(callback: BooleanCallback) {
        if (user == null) {
            callback.handler(false)
        } else {
            user?.delete()?.addOnCompleteListener {
                callback.handler(it.isSuccessful)
            }
        }
    }
}