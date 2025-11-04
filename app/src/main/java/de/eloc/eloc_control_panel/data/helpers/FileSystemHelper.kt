package de.eloc.eloc_control_panel.data.helpers

import android.util.Base64
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.data.UploadType
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.driver.DeviceDriver
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val UPLOAD_CACHE_NAME = "upload_cache"
private const val PROFILES_DIR_NAME = "profiles"

private const val RANGER_SEPARATOR = "_r_"
private const val NAME_SEPARATOR = "_e_"

object FileSystemHelper {

    const val JSON_EXT = ".json"
    const val PREFIX_CONFIG = "config_"
    const val PREFIX_STATUS = "status_"
    const val PREFIX_MAP = "map_"
    private const val HEADER_MAC = "header_mac"
    private const val HEADER_DELIMITER = ":"

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

    private var profilesDir: File? = null
        get() {
            if (field == null) {
                val parent = App.instance.filesDir
                val dir = File(parent, PROFILES_DIR_NAME)

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

    fun saveDataFile(
        macAddress: String?,
        json: String,
        uploadType: UploadType,
        saveTime: Date
    ): Boolean {
        try {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-z", Locale.US)
            val captureTimestamp = dateFormatter.format(saveTime).replace(":", "")
            val prefix = when (uploadType) {
                UploadType.Config -> PREFIX_CONFIG
                UploadType.Status -> PREFIX_STATUS
                UploadType.Map -> ""
            }

            val fileName = if (uploadType == UploadType.Map) {
                generateMapDataFileName(DeviceDriver.name)
            } else {
                generateDataFileName(
                    prefix,
                    Preferences.rangerName,
                    DeviceDriver.name,
                    captureTimestamp
                )
            }
            val file = File(uploadCache, fileName)
            val mac = macAddress?.lowercase()?.trim() ?: ""
            val contents = if (mac.isEmpty()) {
                json
            } else {
                "$HEADER_MAC$HEADER_DELIMITER$mac\n$json".trim()
            }
            file.writeText(contents)
            return true
        } catch (_: Exception) {

        }
        return false
    }

    fun isConfig(fileName: String) =
        fileName.startsWith(PREFIX_CONFIG) && fileName.endsWith(JSON_EXT)

    fun isStatus(fileName: String) =
        fileName.startsWith(PREFIX_STATUS) && fileName.endsWith(JSON_EXT)

    fun isMapData(fileName: String) =
        fileName.startsWith(PREFIX_MAP) && fileName.endsWith(JSON_EXT)

    fun readDataFile(fileName: String): HashMap<String, Any>? {
        val file = File(uploadCache, fileName)
        if (file.exists()) {
            val contents = file.readText()
            val start = contents.indexOf('{')
            var json = ""
            if (start >= 0) {
                json = contents.substring(start)
            }
            return parsePayloadMap(json)
        }
        return null
    }

    fun readMacAddress(fileName: String): String {
        val file = File(uploadCache, fileName)
        var macAddress = "<not available>"
        if (file.exists()) {
            val contents = file.readText()
            val end = contents.indexOf('{')
            var rawHeaders = ""
            if (end >= 0) {
                rawHeaders = contents.substring(0, end)
            }
            val headerList = rawHeaders.split("\n")
            for (h in headerList) {
                val header = h.trim()
                if (header.startsWith(HEADER_MAC)) {
                    macAddress = getHeaderValue(header)
                }
            }
        }
        return macAddress.lowercase()
    }

    private fun getHeaderValue(header: String): String {
        val fullHeader = header.trim()
        var headerValue = ""
        // Do not use split - the value of the header might contain the value of the delimiter
        val valueOffset = fullHeader.indexOf(HEADER_DELIMITER)
        if (valueOffset > 0) {
            headerValue = fullHeader.substring(valueOffset + HEADER_DELIMITER.length)
        }
        return headerValue
    }

    fun readMapDataFile(fileName: String): HashMap<String, Any>? =
        try {
            val file = File(uploadCache, fileName)
            if (file.exists()) {
                val json = file.readText()
                val root = JSONObject(json)
                toMap(root)
            } else {
                null
            }
        } catch (_: Exception) {
            null
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
                (value is Int) || (value is Long) || (value is String) || (value is Boolean) || (value is Double)
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

    private fun generateMapDataFileName(elocName: String): String =
        buildString {
            append(PREFIX_MAP)
            append(elocName)
            append(JSON_EXT)
        }

    private fun generateDataFileName(
        prefix: String,
        rangerName: String,
        elocName: String,
        captureTimestamp: String
    ): String =
        buildString {
            append(prefix)
            append(captureTimestamp)
            append(NAME_SEPARATOR)
            append(elocName)
            append(RANGER_SEPARATOR)
            append(rangerName)
            append(JSON_EXT)
        }

    private fun generateProfileFilename() = generateProfileFilename(AuthHelper.instance.userId)

    private fun generateProfileFilename(profileId: String): String {
        val toHex = fun(b: Byte) = "%02x".format(b)
        return MessageDigest.getInstance("SHA-256")
            .digest(profileId.toByteArray())
            .map {
                toHex(it)
            }
            .toList()
            .joinToString("")
    }

    fun saveProfile() {
        val filename = generateProfileFilename()
        val profileFile = File(profilesDir, filename)
        if (profileFile.exists()) {
            profileFile.delete()
        }
        val content = buildString {
            appendLine(Preferences.rangerName)
            appendLine(AuthHelper.instance.emailAddress)
            appendLine(Preferences.profilePictureUrl)
        }
        val bytes = Base64.encode(content.toByteArray(), Base64.DEFAULT)
        profileFile.writeBytes(bytes)
    }

    fun isProvisionedProfile(profileId: String): Boolean {
        val localId = generateProfileFilename(profileId)
        val files = profilesDir!!.listFiles()
        if (files != null) {
            for (f in files) {
                if (f.isFile && f.name == localId) {
                    return true
                }
            }
        }
        return false
    }

    fun getSavedProfileFile(): File? {
        val profileFilename = generateProfileFilename()
        val files = profilesDir!!.listFiles()
        if (files != null) {
            for (f in files) {
                if (f.name == profileFilename) {
                    return f
                }
            }
        }
        return null
    }
}