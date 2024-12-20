package de.eloc.eloc_control_panel.data.helpers.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.android.gms.maps.model.LatLng
import de.eloc.eloc_control_panel.data.ElocDeviceInfo
import de.eloc.eloc_control_panel.data.UploadResult
import de.eloc.eloc_control_panel.data.helpers.FileSystemHelper
import de.eloc.eloc_control_panel.data.helpers.TimeHelper
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.driver.KEY_TIMESTAMP
import de.eloc.eloc_control_panel.interfaces.ProfileCheckCallback

class FirestoreHelper {

    companion object {
        private var cInstance: FirestoreHelper? = null
        const val FIELD_PROFILE_PICTURE = "profile_picture"
        const val FIELD_USER_ID = "user_id"
        private const val KEY_APP_METADATA = "app_metadata"
        private const val COL_UPLOADS = "eloc_app/uploads"
        private const val COL_CONFIG = "$COL_UPLOADS/config"
        private const val COL_STATUS = "$COL_UPLOADS/status"
        private const val COL_MAP = "$COL_UPLOADS/map"

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

    fun updateProfilePicture(url: String, id: String, callback: (() -> Unit)?) {

        val documentId = id.trim().ifEmpty {
            callback?.invoke()
            return
        }

        val fieldValue = url.trim().ifEmpty {
            callback?.invoke()
            return
        }

        val data = HashMap<String, Any>()
        data[FIELD_PROFILE_PICTURE] = fieldValue
        accountsNode
            .document(documentId)
            .update(data)
            .addOnCompleteListener { callback?.invoke() }
    }

    fun updateProfile(id: String, data: HashMap<String, Any>, callback: ((Boolean) -> Unit)?) {
        val documentId = id.trim()
        if (documentId.isEmpty() || data.isEmpty()) {
            callback?.invoke(false)
            return
        }

        val options = SetOptions.merge()
        accountsNode
            .document(documentId)
            .set(data, options)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    if (data.containsKey(FIELD_USER_ID)) {
                        Preferences.rangerName = data[FIELD_USER_ID].toString()
                    }
                    callback?.invoke(true)
                } else {
                    callback?.invoke(false)
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
                    // Check if accounts is saved in preferences
                    if (FileSystemHelper.isProvisionedProfile(documentId)) {
                        callback?.handler(hasProfile = true, unavailable = true)
                    } else {
                        val exception = it.exception as? FirebaseFirestoreException
                        val code = exception?.code?.name
                        val unavailable = (code == "UNAVAILABLE")
                        callback?.handler(false, unavailable)
                    }
                }
            }
    }

    fun userIdExists(userId: String, callback: ((Boolean) -> Unit)?) {
        val uid = userId.trim().ifEmpty {
            callback?.invoke(false)
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
                callback?.invoke(exists)
            }
    }

    fun getProfile(id: String, uiCallback: (() -> Unit)?) {
        val documentId = id.trim().ifEmpty {
            uiCallback?.invoke()
            return
        }
        accountsNode
            .document(documentId)
            .get(Source.SERVER)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val snapshot = it.result
                    val profilePictureUrl =
                        snapshot.get(FIELD_PROFILE_PICTURE, String::class.java)
                    val rangerName = snapshot.get(FIELD_USER_ID, String::class.java)
                    Preferences.rangerName = rangerName ?: ""
                    Preferences.profilePictureUrl = profilePictureUrl ?: ""
                    FileSystemHelper.saveProfile()
                }
                uiCallback?.invoke()
            }
    }

    fun deleteProfile(id: String, callback: ((Boolean) -> Unit)?) {
        val documentId = id.trim().ifEmpty {
            callback?.invoke(false)
            return
        }
        accountsNode
            .document(documentId)
            .delete()
            .addOnCompleteListener { callback?.invoke(it.isSuccessful) }
    }

    fun uploadDataFiles(): UploadResult {
        val pendingUploads = FileSystemHelper.pendingUploads
        val pattern = Regex("_[er]_")
        val totalCount = pendingUploads.size
        var completed: Boolean
        var wait = true
        if (totalCount == 0) {
            return UploadResult.NoData
        }

        var failCount = 0
        var successCount = 0
        var result = UploadResult.NoData
        val completeListener = fun(successful: Boolean, fileName: String) {
            if (successful) {
                successCount++
                FileSystemHelper.deleteUploadedFile(fileName)
            } else {
                failCount++
            }

            completed = ((successCount + failCount) == pendingUploads.size)
            if (completed) {
                result = if (successCount == totalCount) {
                    UploadResult.Uploaded
                } else {
                    UploadResult.Failed
                }
                wait = false
            }
        }

        for (fileName in pendingUploads) {
            val isConfig = FileSystemHelper.isConfig(fileName)
            val isStatus = FileSystemHelper.isStatus(fileName)
            val isMapData = FileSystemHelper.isMapData(fileName)
            var prefix: String
            var document: DocumentReference
            if (isConfig) {
                prefix = FileSystemHelper.PREFIX_CONFIG
                document = FirebaseFirestore.getInstance()
                    .document("$COL_CONFIG/$fileName")
            } else if (isStatus) {
                prefix = FileSystemHelper.PREFIX_STATUS
                document = FirebaseFirestore.getInstance()
                    .document("$COL_STATUS/$fileName")
            } else if (isMapData) {
                prefix = FileSystemHelper.PREFIX_MAP
                document = FirebaseFirestore.getInstance()
                    .document("$COL_MAP/$fileName")
            } else {
                continue
            }

            val data = if (isMapData) {
                FileSystemHelper.readMapDataFile(fileName)
            } else {
                FileSystemHelper.readDataFile(fileName)
            }

            if (data.isNullOrEmpty()) {
                continue
            }

            if (isMapData) {
                val serverDocumentTimestamp = getMapDataTimestamp(document)
                val str = data[KEY_TIMESTAMP].toString()
                val localDocumentTimestamp = str.toLongOrNull() ?: -1L
                if (localDocumentTimestamp <= serverDocumentTimestamp) {
                    completeListener(true, fileName)
                    continue
                }
            } else {
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
            }

            document
                .set(data)
                .addOnCompleteListener { completeListener(it.isSuccessful, fileName) }
        }

        // Safe to wait, since function runs on background thread
        while (wait) {
            try {
                Thread.sleep(1000)
            } catch (_: Exception) {
            }
        }
        return result
    }

    private fun getMapDataTimestamp(doc: DocumentReference): Long {
        var timestamp = -1L
        var wait = true
        doc.get().addOnCompleteListener {
            if (it.isSuccessful) {
                val snapshot = it.result
                if (snapshot != null) {
                    val str = snapshot.get(KEY_TIMESTAMP).toString()
                    timestamp = str.toLongOrNull() ?: -1L
                }
            }
            wait = false
        }
        while (wait) {
            try {
                Thread.sleep(500)
            } catch (_: Exception) {
            }
        }

        return timestamp
    }

    fun getElocDevicesAsync(
        addDeviceCallback: (ElocDeviceInfo) -> Unit,
        onCompletedCallback: () -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection(COL_MAP)
            .whereEqualTo("rangerName", Preferences.rangerName)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val snapshot = it.result
                    if (snapshot != null) {
                        for (doc in snapshot.documents) {
                            val latitude = doc.get("latitude", Double::class.java) ?: continue
                            val longitude = doc.get("longitude", Double::class.java) ?: continue
                            val location = LatLng(latitude, longitude)
                            val locationAccuracy =
                                doc.get("gpsAccuracyMeters", Double::class.java) ?: continue
                            val deviceName = doc.id
                                .replace("map_", "")
                                .replace(".json", "")
                                .trim()
                            val timestamp =
                                doc.get("timestamp", Long::class.java) ?: continue
                            val capturedTime = TimeHelper.prettyDate(timestamp)
                            val batteryVolts = doc.get("batteryVolts", Double::class.java) ?: -1.0
                            val recordingTimeSinceBoot =
                                doc.get("recTimeHours", Double::class.java) ?: -1.0

                            val info = ElocDeviceInfo(
                                location,
                                deviceName,
                                batteryVolts,
                                capturedTime,
                                recordingTimeSinceBoot,
                                locationAccuracy
                            )
                            addDeviceCallback(info)

                        }
                    }
                }
                onCompletedCallback()
            }
    }
}
