package de.eloc.eloc_control_panel.ng3.data.helpers.firebase

import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.ng3.App
import de.eloc.eloc_control_panel.ng3.interfaces.BooleanCallback
import de.eloc.eloc_control_panel.ng3.interfaces.StringCallback

class AuthHelper {
    companion object {
        val instance: AuthHelper = AuthHelper()
    }

    private val googleSignInClient: GoogleSignInClient

    private val user: FirebaseUser? get() = FirebaseAuth.getInstance().currentUser

    val googleSignInIntent get() = googleSignInClient.signInIntent

    val isSignedIn get() = user != null

    val emailAddress get() = user?.email ?: ""

    val isEmailAddressVerified get() = user?.isEmailVerified ?: false

    val userId get() = user?.uid ?: ""

    private val defaultErrorMessage get() = App.instance.getString(R.string.something_went_wrong)

    init {
        val context = App.instance
        val clientId = context.getString(R.string.default_web_client_id)
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(clientId)
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, signInOptions)
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

    fun signInWithGoogle(data: Intent, callback: BooleanCallback) {
        GoogleSignIn.getSignedInAccountFromIntent(data).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val account = task.result
                if (account != null) {
                    val idToken = account.idToken
                    if (idToken != null) {
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        FirebaseAuth.getInstance().signInWithCredential(credential)
                            .addOnCompleteListener {
                                googleSignInClient.signOut()
                                callback.handler(true)
                            }
                    }
                }
            }
        }
    }

    fun signOut() {
        googleSignInClient.signOut()
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