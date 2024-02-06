package de.eloc.eloc_control_panel.data.helpers

import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import de.eloc.eloc_control_panel.driver.DeviceDriver
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val UPLOAD_CACHE_NAME = "upload_cache"

private const val RANGER_SEPARATOR = "_r_"
private const val NAME_SEPARATOR = "_e_"

object FileSystemHelper {

    const val JSON_EXT = ".json"
    const val PREFIX_CONFIG = "config_"
    const val PREFIX_STATUS = "status_"

    val pendingUploads: Array<String>
        get() = uploadCache?.list() ?: arrayOf()

    private var uploadCache: File? = null
        get() {
            if (field == null) {
                val parent = App.instance.filesDir
                val dir = File(parent, UPLOAD_CACHE_NAME)

                if (dir.exists() && (!dir.isDirectory)) {
                    dir.delete()
                }
                if (!dir.exists()) {
                    dir.mkdirs()
                }

                field = dir
            }
            return field
        }

    fun saveDataFile(isConfig: Boolean, json: String): Boolean {
        try {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-z", Locale.US)
            val now = Date(System.currentTimeMillis())
            val captureTimestamp = dateFormatter.format(now)
            val prefix = if (isConfig) {
                PREFIX_CONFIG
            } else {
                PREFIX_STATUS
            }
            val fileName =
                generateDataFileName(prefix, DeviceDriver.name, captureTimestamp)
            val file = File(uploadCache, fileName)
            file.writeText(json)
            return true
        } catch (_: Exception) {

        }
        return false
    }

    fun isConfig(fileName: String) =
        fileName.startsWith(PREFIX_CONFIG) && fileName.endsWith(JSON_EXT)

    fun isStatus(fileName: String) =
        fileName.startsWith(PREFIX_STATUS) && fileName.endsWith(JSON_EXT)

    fun readDataFile(fileName: String): HashMap<String, Any>? {
        val file = File(uploadCache, fileName)
        if (file.exists()) {
            val json = file.readText()
            return parsePayloadMap(json)
        }
        return null
    }

    private fun parsePayloadMap(json: String): HashMap<String, Any>? =
        try {
            val payload = JSONObject(json).getJSONObject("payload")
            toMap(payload)
        } catch (_: Exception) {
            null
        }


    private fun toMap(root: JSONObject): HashMap<String, Any> {
        val map = HashMap<String, Any>()
        for (key in root.keys()) {
            val value = root.get(key)
            val isPrimitiveType =
                (value is Int) || (value is String) || (value is Boolean) || (value is Double)
            if (isPrimitiveType) {
                map[key] = value
            } else if (value is JSONObject) {
                val valueMap = toMap(value)
                if (valueMap.isNotEmpty()) {
                    map[key] = valueMap
                }
            } else {
                map[key] = value.toString()
            }
        }
        return map
    }

    fun deleteUploadedFile(fileName: String) {
        val localFile = File(uploadCache, fileName)
        if (localFile.exists()) {
            localFile.delete()
        }
    }

    private fun generateDataFileName(
        prefix: String,
        elocName: String,
        captureTimestamp: String
    ): String =
        buildString {
            append(prefix)
            append(captureTimestamp)
            append(NAME_SEPARATOR)
            append(elocName)
            append(RANGER_SEPARATOR)
            append(FirestoreHelper.instance.rangerName)
            append(JSON_EXT)
        }
}