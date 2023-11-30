package de.eloc.eloc_control_panel.ng3.data.helpers.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import de.eloc.eloc_control_panel.ng3.data.UserProfile
import de.eloc.eloc_control_panel.ng3.interfaces.BooleanCallback
import de.eloc.eloc_control_panel.ng3.interfaces.ProfileCallback
import de.eloc.eloc_control_panel.ng3.interfaces.ProfileCheckCallback
import de.eloc.eloc_control_panel.ng3.interfaces.VoidCallback

class FirestoreHelper {

    companion object {
        private var cInstance: FirestoreHelper? = null
        const val FIELD_PROFILE_PICTURE = "profile_picture"
        const val FIELD_USER_ID = "user_id"

        val instance
            get():  FirestoreHelper {
                if (cInstance == null) {
                    cInstance = FirestoreHelper()
                }
                return cInstance!!
            }
    }

    private val accountsNode: CollectionReference

    init {
        val firestore = FirebaseFirestore.getInstance()
        accountsNode = firestore.collection("accounts")
    }

    fun updateProfilePicture(url: String, id: String, callback: VoidCallback) =
        updateProfileField(url, FIELD_PROFILE_PICTURE, id, callback)

    private fun updateProfileField(
        value: String,
        fieldName: String,
        id: String,
        callback: VoidCallback?
    ) {

        val documentId = id.trim().ifEmpty {
            callback?.handler()
            return
        }

        val fieldValue = value.trim().ifEmpty {
            callback?.handler()
            return
        }

        val data = HashMap<String, Any>()
        data[fieldName] = fieldValue
        accountsNode
            .document(documentId)
            .update(data)
            .addOnCompleteListener { callback?.handler() }
    }

    fun updateProfile(id: String, data: HashMap<String, Any>, callback: BooleanCallback?) {
        val documentId = id.trim()
        if (documentId.isEmpty() || data.isEmpty()) {
            callback?.handler(false)
            return
        }

        val options = SetOptions.merge()
        accountsNode
            .document(documentId)
            .set(data, options)
            .addOnCompleteListener { callback?.handler(it.isSuccessful) }
    }

    fun hasProfile(id: String, callback: ProfileCheckCallback?) {
        val documentId = id.trim().ifEmpty {
            callback?.handler(hasProfile = false, unavailable = true)
            return
        }

        accountsNode
            .document(documentId)
            .get(Source.SERVER)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    var hasUserId = false
                    val snapshot = it.result
                    if (snapshot != null) {
                        val o = snapshot.get(FIELD_USER_ID)
                        if (o != null) {
                            val userId = o.toString().trim()
                            hasUserId = userId.isNotEmpty()
                        }
                    }
                    callback?.handler(hasUserId, false)
                } else {
                    val exception = it.exception as? FirebaseFirestoreException
                    val code = exception?.code?.name
                    val unavailable = (code == "UNAVAILABLE")
                    callback?.handler(false, unavailable)
                }
            }
    }

    fun userIdExists(userId: String, callback: BooleanCallback?) {
        val uid = userId.trim().ifEmpty {
            callback?.handler(false)
            return
        }
        accountsNode
            .whereEqualTo(FIELD_USER_ID, uid)
            .get(Source.SERVER)
            .addOnCompleteListener {
                var exists = false
                if (it.isSuccessful) {
                    val count = it.result.size()
                    exists = (count > 0)
                }
                callback?.handler(exists)
            }
    }

    fun getProfile(id: String, emailAddress: String, callback: ProfileCallback?) {
        val documentId = id.trim().ifEmpty {
            callback?.handler(null)
            return
        }
        accountsNode
            .document(documentId)
            .get(Source.SERVER)
            .addOnCompleteListener {
                var profile: UserProfile? = null
                if (it.isSuccessful) {
                    val snapshot = it.result
                    val profilePictureUrl = snapshot.get(FIELD_PROFILE_PICTURE, String::class.java)
                    val userId = snapshot.get(FIELD_USER_ID, String::class.java)
                    if (userId != null) {
                        profile = UserProfile(userId)
                        profile.profilePictureUrl = profilePictureUrl ?: ""
                        profile.emailAddress = emailAddress
                    }
                }
                callback?.handler(profile)
            }
    }

    fun deleteProfile(id: String, callback: BooleanCallback?) {
        val documentId = id.trim().ifEmpty {
            callback?.handler(false)
            return
        }
        accountsNode
            .document(documentId)
            .delete()
            .addOnCompleteListener { callback?.handler(it.isSuccessful) }
    }
}
