package de.eloc.eloc_control_panel.data.helpers.firebase

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import de.eloc.eloc_control_panel.data.ElocDeviceInfo
import de.eloc.eloc_control_panel.data.UploadResult
import de.eloc.eloc_control_panel.data.helpers.FileSystemHelper
import de.eloc.eloc_control_panel.data.helpers.TimeHelper
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.driver.KEY_TIMESTAMP
import de.eloc.eloc_control_panel.interfaces.ProfileCheckCallback
import java.util.concurrent.TimeUnit

class FirestoreHelper {

    companion object {
        private const val TAG = "FirestoreHelper"
        private var cInstance: FirestoreHelper? = null
        const val FIELD_PROFILE_PICTURE = "profile_picture"
        const val FIELD_USER_ID = "user_id"
        private const val KEY_APP_METADATA = "app_metadata"
        private const val COL_UPLOADS = "eloc_app/uploads"
        private const val COL_CONFIG = "$COL_UPLOADS/config"
        private const val COL_STATUS = "$COL_UPLOADS/status"
        private const val COL_MAP = "$COL_UPLOADS/map"
        private const val COL_FIRMWARE_RESULTS = "firmware_update_results"

        // A Firestore write Task only completes on server acknowledgement, and a
        // read can hang on a half-open connection. Both are bounded so an upload
        // run always returns and the caller can retry on its own schedule.
        private const val WRITE_TIMEOUT_SECONDS = 60L
        private const val READ_TIMEOUT_SECONDS = 20L

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

    /**
     * Record the outcome of a Bluetooth firmware update (Phase 3 of the
     * firmware-update plan), keyed on **mac_address** — device_name is not
     * reliable enough to identify a unit.
     *
     * Fire-and-forget on purpose: Firestore's offline write queue holds this
     * until the phone next has signal, which in the field is hours or days
     * later, so no custom retry is needed and nothing waits on the result.
     */
    fun uploadFirmwareUpdateResult(
        macAddress: String,
        deviceName: String,
        fromVersion: String,
        toVersion: String,
        outcome: String,
    ) {
        val documentId = macAddress.trim().ifEmpty { return }
        val data = hashMapOf<String, Any>(
            "mac_address" to documentId,
            "device_name" to deviceName,
            "from_version" to fromVersion,
            "to_version" to toVersion,
            "outcome" to outcome,
            "ranger" to Preferences.rangerName,
            "timestamp" to FieldValue.serverTimestamp(),
        )
        try {
            FirebaseFirestore.getInstance()
                .collection(COL_FIRMWARE_RESULTS)
                .document(documentId)
                .set(data)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Log.i(TAG, "firmware update result stored for $documentId ($outcome)")
                    } else {
                        Log.w(TAG, "firmware update result write failed for $documentId", it.exception)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "could not queue firmware update result", e)
        }
    }

    fun uploadDataFiles(): UploadResult {
        val pendingUploads = FileSystemHelper.pendingUploads
        val pattern = Regex("_[er]_")
        val totalCount = pendingUploads.size
        if (totalCount == 0) {
            Log.d(TAG, "uploadDataFiles: no pending uploads")
            return UploadResult.NoData
        }

        Log.i(
            TAG,
            "uploadDataFiles: starting upload of $totalCount file(s): ${pendingUploads.toList()}"
        )

        // Files we actually sent, each paired with the write we are waiting on.
        val writes = mutableListOf<Pair<String, Task<Void>>>()
        var discardedCount = 0
        var skippedCount = 0

        for (fileName in pendingUploads) {
            val isConfig = FileSystemHelper.isConfig(fileName)
            val isStatus = FileSystemHelper.isStatus(fileName)
            val isMapData = FileSystemHelper.isMapData(fileName)

            if (!isConfig && !isStatus && !isMapData) {
                // Nothing we know how to upload, and it never will be. Drop it
                // rather than carry it on every future run.
                Log.w(TAG, "uploadDataFiles: discarding '$fileName' — unknown file type")
                FileSystemHelper.deleteUploadedFile(fileName)
                discardedCount++
                continue
            }

            val collection = when {
                isConfig -> COL_CONFIG
                isStatus -> COL_STATUS
                else -> COL_MAP
            }
            val document = FirebaseFirestore.getInstance().document("$collection/$fileName")

            val data = if (isMapData) {
                FileSystemHelper.readMapDataFile(fileName)
            } else {
                FileSystemHelper.readDataFile(fileName)
            }

            if (data.isNullOrEmpty()) {
                // A file with no payload can never be uploaded — e.g. a getConfig
                // reply that came back as `"payload": {}`. This used to be a bare
                // `continue`, which left the file in the queue forever AND meant
                // the run never counted it as finished, wedging the upload loop.
                Log.w(TAG, "uploadDataFiles: discarding '$fileName' — empty/unreadable payload")
                FileSystemHelper.deleteUploadedFile(fileName)
                discardedCount++
                continue
            }

            if (isMapData) {
                val serverDocumentTimestamp = getMapDataTimestamp(document)
                val str = data[KEY_TIMESTAMP].toString()
                val localDocumentTimestamp = str.toLongOrNull() ?: -1L
                if (localDocumentTimestamp <= serverDocumentTimestamp) {
                    // The server already holds this or newer — nothing to send.
                    Log.d(TAG, "uploadDataFiles: '$fileName' is already current on the server")
                    FileSystemHelper.deleteUploadedFile(fileName)
                    skippedCount++
                    continue
                }
            } else {
                val prefix =
                    if (isConfig) FileSystemHelper.PREFIX_CONFIG else FileSystemHelper.PREFIX_STATUS
                val macAddress = FileSystemHelper.readMacAddress(fileName)
                val metadata = fileName
                    .replace(prefix, "")
                    .replace(FileSystemHelper.JSON_EXT, "")
                    .split(pattern)
                if (metadata.size >= 3) {
                    data[KEY_APP_METADATA] = hashMapOf(
                        "capture_timestamp" to metadata[0],
                        "ranger" to metadata[2],
                        "device_name" to metadata[1],
                        "upload_timestamp" to FieldValue.serverTimestamp(),
                        "mac_address" to macAddress
                    )
                }
            }

            Log.d(TAG, "uploadDataFiles: dispatching set() for '$fileName' -> ${document.path}")
            writes.add(fileName to document.set(data))
        }

        if (writes.isEmpty()) {
            Log.i(
                TAG,
                "uploadDataFiles: nothing to send (discarded=$discardedCount skipped=$skippedCount)"
            )
            return UploadResult.NoData
        }

        // Wait for the writes, but never indefinitely. A Firestore set() Task only
        // completes once the SERVER acknowledges the write, so with no connectivity
        // it simply never completes. Time out and let the caller retry instead: the
        // local file is kept, and Firestore's own offline queue may still flush it.
        try {
            Tasks.await(
                Tasks.whenAllComplete(writes.map { it.second }),
                WRITE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            )
        } catch (e: Exception) {
            Log.w(
                TAG,
                "uploadDataFiles: gave up waiting after ${WRITE_TIMEOUT_SECONDS}s — " +
                        "unacknowledged writes stay queued locally",
                e
            )
        }

        var successCount = 0
        var failCount = 0
        for ((fileName, task) in writes) {
            if (task.isSuccessful) {
                successCount++
                Log.i(TAG, "uploadDataFiles: server acked '$fileName' — deleting local file")
                FileSystemHelper.deleteUploadedFile(fileName)
            } else {
                failCount++
                val code = (task.exception as? FirebaseFirestoreException)?.code?.name
                Log.w(
                    TAG,
                    "uploadDataFiles: '$fileName' not acked (code=$code) — kept for retry",
                    task.exception
                )
            }
        }

        val result = if (failCount == 0) UploadResult.Uploaded else UploadResult.Failed
        Log.i(
            TAG,
            "uploadDataFiles: done — success=$successCount fail=$failCount " +
                    "discarded=$discardedCount skipped=$skippedCount result=$result"
        )
        return result
    }

    private fun getMapDataTimestamp(doc: DocumentReference): Long =
        try {
            // Bounded, unlike the spin loop this replaces: offline, get() falls back
            // to the local cache, but a half-open connection could hang forever.
            val snapshot = Tasks.await(doc.get(), READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            snapshot?.get(KEY_TIMESTAMP)?.toString()?.toLongOrNull() ?: -1L
        } catch (e: Exception) {
            // -1 means "assume the server has nothing", so the file gets uploaded.
            Log.w(TAG, "getMapDataTimestamp: could not read ${doc.path}", e)
            -1L
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
