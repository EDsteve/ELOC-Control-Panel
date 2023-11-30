package de.eloc.eloc_control_panel.ng3.data.helpers

import android.graphics.Bitmap
import androidx.collection.LruCache
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.ng3.App
import de.eloc.eloc_control_panel.ng3.data.ElocDeviceInfo
import de.eloc.eloc_control_panel.ng3.data.UploadResult
import de.eloc.eloc_control_panel.ng3.interfaces.ElocDeviceInfoListCallback
import de.eloc.eloc_control_panel.ng3.interfaces.StringCallback
import de.eloc.eloc_control_panel.ng3.interfaces.VoidCallback
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

private const val BOUNDARY = "*****"

class HttpHelper {
    companion object {
        private var sInstance: HttpHelper? = null

        val instance: HttpHelper
            get() {
                if (sInstance == null) {
                    sInstance = HttpHelper()
                }
                return sInstance!!
            }
    }

    private val requestQueue: RequestQueue = Volley.newRequestQueue(App.instance)
    private val cache = object : ImageLoader.ImageCache {
        // 100MB cache size
        private val bitmapCache = LruCache<String, Bitmap>(100 * 1024 * 1024)

        override fun getBitmap(url: String?): Bitmap? =
            if (url != null) {
                bitmapCache.get(url)
            } else {
                null
            }

        override fun putBitmap(url: String?, bitmap: Bitmap?) {
            if ((url != null) && (bitmap != null)) {
                bitmapCache.put(url, bitmap)
            }
        }
    }
    val imageLoader: ImageLoader = ImageLoader(requestQueue, cache)

    fun getElocDevicesAsync(rangerName: String, callback: ElocDeviceInfoListCallback?) {
        // Volley will do request on background thread... no need for an executor.
        // But that also means remember to use the callback!
        val address = "http://128.199.206.198/ELOC/map/appmap.php"
        val request = StringRequest(
            address,
            { response ->
                val deviceInfos = ElocDeviceInfo.parseForRanger(response, rangerName)
                callback?.handler(deviceInfos)
            },
            { _ ->
                callback?.handler(ArrayList())
            }
        )
        requestQueue.add(request)
        requestQueue.start()
    }

    private fun onStatusUploadCompleted(
        result: UploadResult,
        snackHandler: StringCallback?
    ) {
        FileSystemHelper.clearPayloadFiles()
        val context = App.instance.applicationContext
        when (result) {
            UploadResult.Uploaded -> {
                snackHandler?.handler(context.getString(R.string.upload_was_completed_successfully))
                FileSystemHelper.clearStatusFiles()
            }

            UploadResult.Failed -> {
                snackHandler?.handler(context.getString(R.string.check_internet_connection))
            }

            else -> {}
        }
    }

    fun uploadElocStatus(
        successCallback: StringCallback? = null,
        errorCallback: VoidCallback? = null,
    ): UploadResult {
        var result = UploadResult.NoData
        val fileName = FileSystemHelper.getUploadStatusFileName()
        if (fileName?.isNotEmpty() == true) {
            result = UploadResult.Failed
            val sourceFile = File(fileName)
            try {
                if (sourceFile.isFile) {
                    val connection = openConnection(fileName)
                    if (connection != null) {
                        DataOutputStream(connection.outputStream).use { dos ->
                            val lineEnd = "\r\n"
                            val twoHyphens = "--"
                            dos.writeBytes("$twoHyphens$BOUNDARY$lineEnd")
                            dos.writeBytes(
                                "Content-Disposition: form-data; name=\"bill\";filename=\"$fileName\"$lineEnd"
                            )
                            dos.writeBytes(lineEnd)

                            // Read from file and to network stream
                            val tmp = ByteArray(8192)
                            FileInputStream(sourceFile).use { inputStream ->
                                while (true) {
                                    val count = inputStream.read(tmp)
                                    if (count > 0) {
                                        dos.write(tmp, 0, count)
                                    } else {
                                        break
                                    }
                                }
                            }

                            // Send multipart form data necesssary after file data...
                            dos.writeBytes(lineEnd)
                            dos.writeBytes("$twoHyphens$BOUNDARY$twoHyphens$lineEnd")

                            val serverResponseCode = connection.responseCode
                            val uploaded =
                                ((serverResponseCode >= 200) && (serverResponseCode < 300))
                            if (uploaded) {
                                result = UploadResult.Uploaded
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        } else {
            errorCallback?.handler()
            FileSystemHelper.clearPayloadFiles()
        }
        onStatusUploadCompleted(result) {
            successCallback?.handler(it)
            FileSystemHelper.clearPayloadFiles()
        }
        return result
    }

    private fun openConnection(sourceFileUri: String): HttpURLConnection? {
        var conn: HttpURLConnection? = null
        try {
            val uploadServerUri = "http://128.199.206.198/ELOC/upload.php?"
            val url = URL(uploadServerUri)

            conn = url.openConnection() as? HttpURLConnection
            conn?.apply {
                doInput = true // Allow Inputs
                doOutput = true // Allow Outputs
                useCaches = false // Don't use a Cached Copy
                requestMethod = "POST"
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("ENCTYPE", "multipart/form-data")
                setRequestProperty("Content-Type", "multipart/form-data;boundary=$BOUNDARY")
                setRequestProperty("bill", sourceFileUri)
            }
        } catch (_: Exception) {
        }
        return conn
    }

}