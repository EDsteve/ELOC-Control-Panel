package de.eloc.eloc_control_panel.data.helpers

import android.graphics.Bitmap
import androidx.collection.LruCache
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.Volley
import de.eloc.eloc_control_panel.App
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

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

    fun getAppProtocolVersion(): Double {
        try {
            val endpoint = "https://eloc-b1e63.web.app/api/protocol_version.json"
            val connection = URL(endpoint).openConnection() as? HttpsURLConnection
            connection?.connectTimeout = 30000
            val statusCode = connection?.responseCode ?: -1
            if ((statusCode >= 200) && (statusCode < 300)) {
                val response =
                    connection?.inputStream?.bufferedReader().use { it?.readText() } ?: ""
                val data = JSONObject(response)
                val versionKey = "version"
                if (data.has(versionKey)) {
                    return data.getDouble(versionKey)
                }
            }
        } catch (_: Exception) {
        }
        return 0.0
    }

}