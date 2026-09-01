package de.eloc.eloc_control_panel.data.helpers

import android.content.Context
import android.util.Log
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.data.FirmwareRelease
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.driver.FirmwareImage
import de.eloc.eloc_control_panel.driver.FirmwareUpdater
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection

/**
 * Prefetch of the current firmware release from GitHub Releases (Phase 3 of the
 * firmware-update plan).
 *
 * Discovery cannot happen at the device — there is no connectivity in the
 * forest. The app polls the public release API while the phone is online in
 * town, downloads the binary, verifies it, and keeps it in `filesDir` so the
 * install later works fully offline. There is no backend: the release tag is the
 * version, the body is the notes, and the asset's `digest` is the integrity hash.
 *
 * Nothing here is trusted for anything safety-relevant. The tag is only a cheap
 * "is there something newer" signal before spending ~1.8 MB; once downloaded the
 * binary must describe *itself* correctly ([FirmwareImage.inspect]) or the
 * release is discarded.
 */
object FirmwareReleaseHelper {

    private const val TAG = "FirmwareReleaseHelper"
    private const val REPO = "LIFsCode/ELOC-3.0"
    private const val LATEST_ENDPOINT = "https://api.github.com/repos/$REPO/releases/latest"
    private const val NEWEST_ENDPOINT = "https://api.github.com/repos/$REPO/releases?per_page=1"

    // Unauthenticated GitHub allows 60 requests/hour per IP; a few checks a day
    // stays far below that and still catches a release while the tech is in town.
    private const val CHECK_INTERVAL_MS = 6L * 60 * 60 * 1000

    // A failed check (flaky signal in town) must not block the next attempt for
    // six hours — back off far less than the success interval.
    private const val FAILED_RETRY_INTERVAL_MS = 30L * 60 * 1000

    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val READ_TIMEOUT_MS = 60_000
    private const val MAX_ASSET_BYTES = 16L * 1024 * 1024

    private const val RELEASES_DIR = "firmware/releases"
    private const val PICKED_DIR = "firmware/picked"
    private const val PICKED_FILE = "fwupdate.bin"

    // The image's own project name. "ELOC" is allow-listed exactly as the
    // firmware's validateImageFile() does, so a future CMake project rename does
    // not silently stop the app from offering releases.
    private val acceptedProjectNames = setOf("idf-wav-sdcard", "ELOC")

    private val running = AtomicBoolean(false)

    /**
     * Where the picker stages a locally chosen .bin. In `filesDir`, never
     * `cacheDir`: a resume re-opens this file after the app has been in the
     * background, and Android may evict the cache in exactly that window.
     */
    fun pickerStagingFile(context: Context): File {
        val dir = File(context.filesDir, PICKED_DIR)
        dir.mkdirs()
        return File(dir, PICKED_FILE)
    }

    /**
     * The cached release, or null when nothing is cached. Metadata that has lost
     * its binary (storage cleared) is downgraded to "known but not downloaded"
     * rather than dropped, so the card can say "connect to the internet".
     */
    fun cachedRelease(): FirmwareRelease? {
        val release = Preferences.cachedFirmwareRelease ?: return null
        if (release.localPath.isNotEmpty() && !release.isDownloaded) {
            val without = release.copy(localPath = "")
            Preferences.cachedFirmwareRelease = without
            return without
        }
        return release
    }

    /** Rate-limited entry point, safe to call from any screen's onResume(). */
    fun maybeCheckForRelease(context: Context) {
        if (App.instance.isOnline() != true) {
            return
        }
        val since = System.currentTimeMillis() - Preferences.lastFirmwareCheckMillis
        if (since in 0 until CHECK_INTERVAL_MS) {
            return
        }
        checkForRelease(context)
    }

    /** Unconditional check; still refuses to run twice at once or during a transfer. */
    fun checkForRelease(context: Context) {
        if (FirmwareUpdater.isBusy) {
            return
        }
        if (!running.compareAndSet(false, true)) {
            return
        }
        val appContext = context.applicationContext
        Thread {
            var succeeded = false
            try {
                succeeded = runCheck(appContext)
            } catch (e: Exception) {
                Log.w(TAG, "firmware release check failed", e)
            } finally {
                val now = System.currentTimeMillis()
                Preferences.lastFirmwareCheckMillis = if (succeeded) {
                    now
                } else {
                    now - CHECK_INTERVAL_MS + FAILED_RETRY_INTERVAL_MS
                }
                running.set(false)
            }
        }.start()
    }

    /** Returns true when the release API answered and was fully processed. */
    private fun runCheck(context: Context): Boolean {
        val beta = Preferences.betaFirmwareChannel
        val body = fetchText(if (beta) NEWEST_ENDPOINT else LATEST_ENDPOINT) ?: return false
        val releaseJson = if (beta) {
            // The list endpoint includes prereleases; /releases/latest skips them.
            val array = JSONArray(body)
            if (array.length() == 0) null else array.optJSONObject(0)
        } else {
            JSONObject(body)
        } ?: return false

        val release = parseRelease(releaseJson)
        if (release == null) {
            // Nothing installable is currently published — no 'ei' asset, or no
            // digest to verify one against (the repo still has releases that
            // predate the Phase-3 convention). Forget what we were holding: the
            // app offers what is published now, which is what makes taking a
            // release down a kill switch rather than a suggestion.
            Log.i(TAG, "no installable asset on the current release — offering nothing")
            forgetCachedRelease(context)
            return true
        }

        val cached = cachedRelease()
        if ((cached != null) && (cached.version == release.version) && cached.isDownloaded) {
            // The release we already hold: refresh notes/metadata in place and
            // keep the verified binary.
            Preferences.cachedFirmwareRelease = release.copy(localPath = cached.localPath)
            return true
        }

        // Whatever the endpoint currently calls latest becomes the cached release,
        // newer or not — that is what makes un-publishing a bad release a kill
        // switch: the previous version becomes latest and the app follows it back.
        if (FirmwareUpdater.isBusy) {
            return false
        }
        val downloaded = download(context, release)
            ?: return false // keep what we have rather than losing an installable binary
        Preferences.cachedFirmwareRelease = downloaded
        Preferences.pruneFirmwareSkips(downloaded.version)
        prune(context, downloaded)
        return true
    }

    private fun parseRelease(json: JSONObject): FirmwareRelease? {
        val version = json.optString("tag_name").trim()
        if (version.isEmpty()) {
            return null
        }
        val assets = json.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name").trim()
            val lower = name.lowercase(Locale.US)
            // Only the 'ei' variant is published; 'no-ai' stays supported in the
            // schema and in the variant guard, it is simply never offered here.
            if (!lower.contains("-ei") || lower.contains("no-ai")) {
                continue
            }
            val digest = asset.optString("digest").trim()
            if (!digest.startsWith("sha256:")) {
                // Without a digest there is nothing to verify the download
                // against, so the release is not installable from the app.
                continue
            }
            val size = asset.optLong("size")
            val url = asset.optString("browser_download_url").trim()
            if ((size <= 0) || (size > MAX_ASSET_BYTES) || url.isEmpty()) {
                continue
            }
            return FirmwareRelease(
                version = version,
                notes = json.optString("body"),
                assetName = name,
                size = size,
                sha256 = digest.removePrefix("sha256:").lowercase(Locale.US),
                downloadUrl = url,
                variant = "ei",
                prerelease = json.optBoolean("prerelease"),
            )
        }
        return null
    }

    /**
     * Download, verify the published digest, then verify that the binary
     * describes itself as the version the tag claims. Returns the release with
     * its local path filled in, or null when anything did not line up.
     */
    private fun download(context: Context, release: FirmwareRelease): FirmwareRelease? {
        val versionDir = File(releasesDir(context), safeName(release.version))
        versionDir.mkdirs()
        val target = File(versionDir, safeName(release.assetName))
        val temp = File(versionDir, target.name + ".part")
        try {
            if (!fetchFile(release.downloadUrl, temp, release.size)) {
                return null
            }
            val sha256 = FirmwareImage.sha256(temp)
            if (!sha256.equals(release.sha256, ignoreCase = true)) {
                Log.w(TAG, release.version + ": digest mismatch, discarding download")
                return null
            }
            val info = FirmwareImage.inspect(temp)
            if (info == null) {
                Log.w(TAG, release.version + ": not a valid ESP32 app image, discarding")
                return null
            }
            if (info.version != release.version) {
                // The stale-PROJECT_VER guard: a release whose binary does not
                // report the tag it was published under cannot be compared
                // against a device version, so it is offered to nobody.
                Log.w(TAG, release.version + ": binary reports '" + info.version + "', discarding")
                return null
            }
            if (!acceptedProjectNames.contains(info.projectName)) {
                Log.w(
                    TAG,
                    release.version + ": foreign project '" + info.projectName + "', discarding"
                )
                return null
            }
            target.delete()
            if (!temp.renameTo(target)) {
                return null
            }
            return release.copy(localPath = target.absolutePath)
        } catch (e: Exception) {
            Log.w(TAG, release.version + ": download failed", e)
            return null
        } finally {
            temp.delete()
            // Leave nothing behind when the download did not survive its checks.
            if (versionDir.list()?.isEmpty() == true) {
                versionDir.delete()
            }
        }
    }

    /** Drop the cached release and its binary; no-op while an update is running. */
    private fun forgetCachedRelease(context: Context) {
        if (FirmwareUpdater.isBusy || (Preferences.cachedFirmwareRelease == null)) {
            return
        }
        Preferences.cachedFirmwareRelease = null
        releasesDir(context).listFiles()?.forEach { child ->
            if (child.isDirectory) {
                child.deleteRecursively()
            }
        }
    }

    /** Keep exactly one release on disk; never touch files while an update runs. */
    private fun prune(context: Context, keep: FirmwareRelease) {
        if (FirmwareUpdater.isBusy) {
            return
        }
        val keepDir = keep.localFile?.parentFile ?: return
        releasesDir(context).listFiles()?.forEach { child ->
            if (child.isDirectory && (child.absolutePath != keepDir.absolutePath)) {
                child.deleteRecursively()
            }
        }
    }

    private fun releasesDir(context: Context): File {
        val dir = File(context.filesDir, RELEASES_DIR)
        dir.mkdirs()
        return dir
    }

    // Tags and asset names come from a remote document: never let one address a
    // path outside the release directory.
    private fun safeName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifEmpty { "firmware" }

    private fun fetchText(endpoint: String): String? {
        var connection: HttpsURLConnection? = null
        try {
            connection = (URL(endpoint).openConnection() as HttpsURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "eloc-control-panel")
            }
            val status = connection.responseCode
            if ((status < 200) || (status >= 300)) {
                Log.w(TAG, endpoint + " returned HTTP " + status)
                return null
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "could not read " + endpoint, e)
            return null
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchFile(endpoint: String, target: File, expectedSize: Long): Boolean {
        var connection: HttpsURLConnection? = null
        try {
            connection = (URL(endpoint).openConnection() as HttpsURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", "eloc-control-panel")
            }
            val status = connection.responseCode
            if ((status < 200) || (status >= 300)) {
                Log.w(TAG, endpoint + " returned HTTP " + status)
                return false
            }
            var written = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) {
                            break
                        }
                        written += read
                        if (written > expectedSize) {
                            Log.w(TAG, endpoint + " is larger than the published size")
                            return false
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
            return written == expectedSize
        } catch (e: Exception) {
            Log.w(TAG, "could not download " + endpoint, e)
            return false
        } finally {
            connection?.disconnect()
        }
    }
}
