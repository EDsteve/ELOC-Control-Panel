package de.eloc.eloc_control_panel.data

import org.json.JSONObject
import java.io.File

/**
 * One published firmware release, as read from the repo's GitHub Releases page
 * (Phase 3 of the firmware-update plan). GitHub's release JSON is the whole
 * manifest: the tag is the version, the body is the notes, and the asset
 * carries size, sha256 digest and download URL.
 *
 * Cached in [de.eloc.eloc_control_panel.data.util.Preferences] so the
 * "update available" card renders with no network — discovery happens in town,
 * installation happens offline in the forest.
 */
data class FirmwareRelease(
    /** Release tag; by convention equal to the version the binary reports. */
    val version: String,
    /** Release body, conventionally "## Features" and "## Fixes" sections. */
    val notes: String,
    val assetName: String,
    val size: Long,
    val sha256: String,
    val downloadUrl: String,
    /** Build variant read from the asset name ("ei" / "no-ai"). */
    val variant: String,
    val prerelease: Boolean,
    /** Absolute path of the verified local copy, empty until downloaded. */
    val localPath: String = "",
) {

    val localFile: File? get() = if (localPath.isEmpty()) null else File(localPath)

    /** True only when the verified binary is actually on this phone right now. */
    val isDownloaded: Boolean
        get() {
            val file = localFile ?: return false
            return file.isFile && (file.length() == size)
        }

    /**
     * The release body split into the two sections a ranger reads at the foot
     * of a tree. Lines come back display-ready: a Markdown bullet becomes a
     * "• " line, anything else stays a plain paragraph line. Inline Markdown
     * (`**bold**`, backticks) is left in place for the UI to render — see
     * `markdownToSpanned`. When neither section yields lines, [body] carries the
     * body verbatim and both lists are empty.
     */
    data class Notes(val features: List<String>, val fixes: List<String>, val body: String)

    val parsedNotes: Notes
        get() {
            val features = mutableListOf<String>()
            val fixes = mutableListOf<String>()
            var current: MutableList<String>? = null
            for (rawLine in notes.lines()) {
                val line = rawLine.trim()
                if (line.startsWith("#")) {
                    val heading = line.trimStart('#').trim().lowercase()
                    current = when {
                        heading.contains("feature") -> features
                        heading.contains("fix") -> fixes
                        else -> null
                    }
                    continue
                }
                if (line.isEmpty()) {
                    continue
                }
                // A bullet marker only counts when whitespace follows it —
                // otherwise "**Known issue:**" loses one of its asterisks and
                // the emphasis it opens never closes.
                val isBullet = bulletMarker.containsMatchIn(line)
                val text = if (isBullet) bulletMarker.replaceFirst(line, "").trim() else line
                if (text.isNotEmpty()) {
                    current?.add(if (isBullet) "• " + text else text)
                }
            }
            return if (features.isEmpty() && fixes.isEmpty()) {
                Notes(emptyList(), emptyList(), notes.trim())
            } else {
                Notes(features, fixes, "")
            }
        }

    fun serialize(): String = JSONObject().apply {
        put(KEY_VERSION, version)
        put(KEY_NOTES, notes)
        put(KEY_ASSET_NAME, assetName)
        put(KEY_SIZE, size)
        put(KEY_SHA256, sha256)
        put(KEY_DOWNLOAD_URL, downloadUrl)
        put(KEY_VARIANT, variant)
        put(KEY_PRERELEASE, prerelease)
        put(KEY_LOCAL_PATH, localPath)
    }.toString()

    companion object {
        private val bulletMarker = Regex("""^[-*+\u2022]\s+""")

        private const val KEY_VERSION = "version"
        private const val KEY_NOTES = "notes"
        private const val KEY_ASSET_NAME = "assetName"
        private const val KEY_SIZE = "size"
        private const val KEY_SHA256 = "sha256"
        private const val KEY_DOWNLOAD_URL = "downloadUrl"
        private const val KEY_VARIANT = "variant"
        private const val KEY_PRERELEASE = "prerelease"
        private const val KEY_LOCAL_PATH = "localPath"

        fun deserialize(data: String): FirmwareRelease? {
            if (data.isBlank()) {
                return null
            }
            return try {
                val json = JSONObject(data)
                val version = json.optString(KEY_VERSION).trim()
                if (version.isEmpty()) {
                    return null
                }
                FirmwareRelease(
                    version = version,
                    notes = json.optString(KEY_NOTES),
                    assetName = json.optString(KEY_ASSET_NAME),
                    size = json.optLong(KEY_SIZE),
                    sha256 = json.optString(KEY_SHA256),
                    downloadUrl = json.optString(KEY_DOWNLOAD_URL),
                    variant = json.optString(KEY_VARIANT),
                    prerelease = json.optBoolean(KEY_PRERELEASE),
                    localPath = json.optString(KEY_LOCAL_PATH),
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
