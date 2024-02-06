package de.eloc.eloc_control_panel.data.helpers.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import de.eloc.eloc_control_panel.data.UploadResult
import de.eloc.eloc_control_panel.data.UserProfile
import de.eloc.eloc_control_panel.data.helpers.FileSystemHelper
import de.eloc.eloc_control_panel.interfaces.BooleanCallback
import de.eloc.eloc_control_panel.interfaces.ProfileCallback
import de.eloc.eloc_control_panel.interfaces.ProfileCheckCallback
import de.eloc.eloc_control_panel.interfaces.VoidCallback
import de.eloc.eloc_control_panel.services.StatusUploadService

class FirestoreHelper {

    companion object {
        private var cInstance: FirestoreHelper? = null
        const val FIELD_PROFILE_PICTURE = "profile_picture"
        const val FIELD_USER_ID = "user_id"
        private const val KEY_APP_METADATA = "app_metadata"

        val instance
            get():  FirestoreHelper {
                if (cInstance == null) {
                    cInstance = FirestoreHelper()
                }
                return cInstance!!
            }
    }

    private val accountsNode: CollectionReference
    var rangerName: String = "unknown_ranger"

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
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    if (FIELD_USER_ID == fieldName) {
                        rangerName = fieldValue
                    }
                }
                callback?.handler()
            }
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
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    if (data.containsKey(FIELD_USER_ID)) {
                        rangerName = data[FIELD_USER_ID].toString()
                    }
                    callback?.handler(true)
                } else {
                    callback?.handler(false)
                }
            }
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
                    val profilePictureUrl =
                        snapshot.get(FIELD_PROFILE_PICTURE, String::class.java)
                    val userId = snapshot.get(FIELD_USER_ID, String::class.java)
                    if (userId != null) {
                        rangerName = userId
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

    fun uploadDataFiles(callback: VoidCallback? = null) {
        val pendingUploads = FileSystemHelper.pendingUploads
        val pattern = Regex("_[er]_")
        val totalCount = pendingUploads.size
        if (totalCount == 0) {
            StatusUploadService.uploadResult = UploadResult.NoData
        }
        var failCount = 0
        var successCount = 0
        for (fileName in pendingUploads) {
            val isConfig = FileSystemHelper.isConfig(fileName)
            val isStatus = FileSystemHelper.isStatus(fileName)
            var prefix: String
            var document: DocumentReference
            if (isConfig) {
                prefix = FileSystemHelper.PREFIX_CONFIG
                document = FirebaseFirestore.getInstance()
                    .document("eloc_app/uploads/config/$fileName")
            } else if (isStatus) {
                prefix = FileSystemHelper.PREFIX_STATUS
                document = FirebaseFirestore.getInstance()
                    .document("eloc_app/uploads/status/$fileName")
            } else {
                continue
            }

            val data = FileSystemHelper.readDataFile(fileName)
            if (data.isNullOrEmpty()) {
                continue
            }

            val metadata = fileName
                .replace(prefix, "")
                .replace(FileSystemHelper.JSON_EXT, "")
                .split(pattern)
            if (metadata.size >= 3) {
                data[KEY_APP_METADATA] = hashMapOf(
                    "capture_timestamp" to metadata[0],
                    "ranger" to metadata[2],
                    "device_name" to metadata[1],
                    "upload_timestamp" to FieldValue.serverTimestamp()
                )
            }

            document.set(data)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        successCount++
                        FileSystemHelper.deleteUploadedFile(fileName)
                    } else {
                        failCount++
                    }

                    val completed = ((successCount + failCount) == pendingUploads.size)
                    if (completed) {
                        if (successCount == totalCount) {
                            StatusUploadService.uploadResult = UploadResult.Uploaded
                        } else {
                            StatusUploadService.uploadResult = UploadResult.Failed
                        }
                        callback?.handler()
                    }
                }
        }
    }
}
