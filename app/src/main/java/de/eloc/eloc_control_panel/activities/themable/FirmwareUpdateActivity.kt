package de.eloc.eloc_control_panel.activities.themable

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.goBack
import de.eloc.eloc_control_panel.activities.markdownToSpanned
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.activities.showModalOptionAlert
import de.eloc.eloc_control_panel.data.FirmwareRelease
import de.eloc.eloc_control_panel.data.RecordState
import de.eloc.eloc_control_panel.data.helpers.FirmwareReleaseHelper
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.databinding.ActivityFirmwareUpdateBinding
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.driver.FirmwareImage
import de.eloc.eloc_control_panel.driver.FirmwareUpdater
import de.eloc.eloc_control_panel.driver.FirmwareVersion
import de.eloc.eloc_control_panel.services.FirmwareUpdateService
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Firmware-update screen. Two entry points, one pipeline.
 *
 * **Picker mode** (Phase 2, the default): pick a local .bin (SAF), show its
 * embedded version + SHA-256, run the preflight-gated transfer via
 * [FirmwareUpdater], then confirm the new version after the device's
 * flash-and-restart cycle. Reached from Device Settings -> Advanced, this path
 * is permanent — it is how a device is deliberately reverted to older firmware.
 *
 * **Release mode** (Phase 3, [EXTRA_RELEASE_VERSION]): the prefetched GitHub
 * release replaces the picker; the binary comes straight from `filesDir`, its
 * variant is known from the asset name, and its notes are shown. Everything
 * after the confirm dialog — preflight, transfer, abort, reconnect, verify — is
 * the picker path unchanged.
 *
 * Only reachable when the connected firmware advertises fwUpdateProto in getStatus.
 */
class FirmwareUpdateActivity : ThemableActivity() {

    companion object {
        /** Version tag of a prefetched release to install instead of a picked file. */
        const val EXTRA_RELEASE_VERSION = "release_version"
    }

    private lateinit var binding: ActivityFirmwareUpdateBinding
    private val listenerId = "firmwareUpdateActivity"
    private val executor = Executors.newSingleThreadExecutor()

    private var stagedFile: File? = null
    private var imageInfo: FirmwareImage.Info? = null
    private var imageVariant = ""
    private var release: FirmwareRelease? = null
    private var releaseNotesExpanded = false

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                loadImageFile(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirmwareUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { goBack() }
        binding.chooseFileButton.setOnClickListener {
            filePicker.launch(arrayOf("application/octet-stream", "application/*"))
        }
        binding.startButton.setOnClickListener { confirmAndStart() }
        binding.releaseNotesToggle.setOnClickListener { showReleaseNotes(!releaseNotesExpanded) }
        binding.abortButton.setOnClickListener { FirmwareUpdater.abort() }
        binding.stopRecordingButton.setOnClickListener { stopRecording() }

        binding.installedVersionValue.text = DeviceDriver.general.version
        binding.variantValue.text = DeviceDriver.general.buildVariant

        FirmwareUpdater.addStateListener(listenerId, ::onUpdaterState)
        FirmwareUpdater.addProgressListener(listenerId, ::onUpdaterProgress)
        // Reflect an update that is already running (e.g. re-entered via the notification)
        onUpdaterState(FirmwareUpdater.state, FirmwareUpdater.lastMessage)
        updateFileViews()

        val requestedRelease = intent.extras?.getString(EXTRA_RELEASE_VERSION)?.trim() ?: ""
        if (requestedRelease.isNotEmpty()) {
            loadRelease(requestedRelease, fromCard = true)
        } else {
            offerCachedRelease()
        }
    }

    /**
     * Opened from Device Settings -> Advanced. If the app is already holding a
     * release this device could install, offer it right here — otherwise the
     * screen greets the tech with a disabled "Update now" and no explanation of
     * the update the badge just told them about. The picker stays visible: this
     * entry point is the revert path and must never be gated.
     */
    private fun offerCachedRelease() {
        val cached = FirmwareReleaseHelper.cachedRelease() ?: return
        val installable = cached.isDownloaded &&
                (DeviceDriver.general.fwUpdateProto >= 1) &&
                (DeviceDriver.general.buildVariant == cached.variant) &&
                FirmwareVersion.isNewer(cached.version, DeviceDriver.general.version)
        if (installable) {
            loadRelease(cached.version, fromCard = false)
        }
    }

    /**
     * Release mode: install the prefetched release instead of a picked file.
     * The binary was downloaded, digest-checked and self-description-checked by
     * [FirmwareReleaseHelper]; here it is only re-inspected so the same views the
     * picker fills (version, project, size, SHA-256) show the real bytes on disk.
     * If the file has gone missing the screen falls back to the picker rather
     * than dead-ending.
     */
    private fun loadRelease(version: String, fromCard: Boolean) {
        val cached = FirmwareReleaseHelper.cachedRelease()
        val file = cached?.localFile
        if ((cached == null) || (cached.version != version) || (file == null) || !cached.isDownloaded) {
            // Only worth an alert when the tech explicitly asked to install this
            // release; the Advanced entry point just falls back to the picker.
            if (fromCard) {
                showModalAlert(
                    getString(R.string.firmware_update),
                    getString(R.string.firmware_release_missing)
                )
            }
            return
        }
        release = cached
        binding.chooseFileButton.visibility = if (fromCard) View.GONE else View.VISIBLE
        binding.releaseLayout.visibility = View.VISIBLE
        binding.releaseNotesText.text = markdownToSpanned(formatNotes(cached))
        showReleaseNotes(false)
        binding.fileStatusText.text = getString(R.string.firmware_reading_file)
        executor.execute {
            val info = FirmwareImage.inspect(file)
            runOnUiThread {
                if (info == null) {
                    showModalAlert(
                        getString(R.string.error),
                        getString(R.string.firmware_invalid_image)
                    )
                } else {
                    stagedFile = file
                    imageInfo = info
                    // Known from the asset name, not sniffed at pick time.
                    imageVariant = cached.variant
                }
                updateFileViews()
            }
        }
    }

    /**
     * Why there is nothing to install. "No firmware file selected" is true but
     * useless when the app *has* a release and simply cannot offer it here —
     * the tech needs to know whether to go and find signal, pick a file, or do
     * nothing at all.
     */
    private fun nothingOnOfferReason(): String {
        val cached = FirmwareReleaseHelper.cachedRelease()
            ?: return getString(
                if (Preferences.betaFirmwareChannel) {
                    R.string.firmware_no_release_cached
                } else {
                    // Nothing is published on the stable channel yet, so a stable
                    // phone will never cache anything; say so rather than implying
                    // a download is on its way.
                    R.string.firmware_release_beta_hint
                }
            )
        if (!cached.isDownloaded) {
            return getString(R.string.firmware_release_not_downloaded)
        }
        if (!FirmwareVersion.isNewer(cached.version, DeviceDriver.general.version)) {
            return getString(R.string.firmware_up_to_date, cached.version)
        }
        return getString(R.string.firmware_no_file_selected)
    }

    /** Collapsed by default: the version and the action matter more than the prose. */
    private fun showReleaseNotes(expanded: Boolean) {
        releaseNotesExpanded = expanded
        binding.releaseNotesText.visibility = if (expanded) View.VISIBLE else View.GONE
        binding.releaseNotesToggle.text =
            getString(R.string.firmware_whats_new) + if (expanded) "  ▴" else "  ▾"
    }

    /** Release body as the two plain-language sections a ranger reads in the field. */
    private fun formatNotes(source: FirmwareRelease): String {
        val notes = source.parsedNotes
        if (notes.features.isEmpty() && notes.fixes.isEmpty()) {
            return notes.body
        }
        val sections = mutableListOf<String>()
        if (notes.features.isNotEmpty()) {
            sections.add(
                getString(R.string.firmware_notes_features).uppercase(Locale.US) + "\n" +
                        notes.features.joinToString("\n")
            )
        }
        if (notes.fixes.isNotEmpty()) {
            sections.add(
                getString(R.string.firmware_notes_fixes).uppercase(Locale.US) + "\n" +
                        notes.fixes.joinToString("\n")
            )
        }
        return sections.joinToString("\n\n")
    }

    override fun onDestroy() {
        FirmwareUpdater.removeStateListener(listenerId)
        FirmwareUpdater.removeProgressListener(listenerId)
        executor.shutdown()
        super.onDestroy()
    }

    // Don't auto-close on disconnect like other device screens: disconnects are
    // an expected part of the update cycle (device restarts to flash).

    private fun loadImageFile(uri: Uri) {
        // A deliberately picked file wins over the release offered on arrival,
        // and carries no notes of its own, so What's-new goes with it.
        release = null
        binding.releaseLayout.visibility = View.GONE
        binding.fileStatusText.text = getString(R.string.firmware_reading_file)
        executor.execute {
            var info: FirmwareImage.Info? = null
            var staged: File? = null
            try {
                // filesDir, not cacheDir: a resume re-opens this file after the
                // app has been backgrounded (the "walk back to the tree tomorrow"
                // case), and Android may evict the cache in exactly that window.
                val target = FirmwareReleaseHelper.pickerStagingFile(this@FirmwareUpdateActivity)
                contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                info = FirmwareImage.inspect(target)
                staged = target
            } catch (_: Exception) {
            }

            // Release-file naming convention carries the build variant
            // (ELOC-<version>-<variant>.bin) since the image itself has no
            // variant marker.
            val fileName = displayName(uri).lowercase(Locale.US)
            imageVariant = when {
                fileName.contains("no-ai") -> "no-ai"
                fileName.contains("-ei") || fileName.contains("_ei") -> "ei"
                else -> ""
            }

            runOnUiThread {
                if (info == null) {
                    stagedFile = null
                    imageInfo = null
                    showModalAlert(
                        getString(R.string.error),
                        getString(R.string.firmware_invalid_image)
                    )
                } else {
                    stagedFile = staged
                    imageInfo = info
                }
                updateFileViews()
            }
        }
    }

    /**
     * Real file name of a SAF document. [Uri.getLastPathSegment] is not
     * reliable for this — e.g. the Downloads provider returns an opaque
     * "msf:<id>" — which would silently disable the variant guard.
     */
    private fun displayName(uri: Uri): String {
        try {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        return cursor.getString(index) ?: ""
                    }
                }
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment ?: ""
    }

    private fun updateFileViews() {
        val info = imageInfo
        // "different" only reads correctly once something is already on offer.
        binding.chooseFileButton.setText(
            if (info == null) R.string.firmware_choose_file else R.string.firmware_choose_different
        )
        if (info == null) {
            binding.fileDetailsLayout.visibility = View.GONE
            binding.fileStatusText.text = nothingOnOfferReason()
            binding.startButton.isEnabled = false
            return
        }
        binding.fileDetailsLayout.visibility = View.VISIBLE
        binding.fileStatusText.text = ""
        binding.fileVersionValue.text = info.version
        binding.fileProjectValue.text = info.projectName
        binding.fileSizeValue.text = formatSize(info.size)
        binding.fileShaValue.text = info.sha256
        binding.fileVariantValue.text =
            imageVariant.ifEmpty { getString(R.string.firmware_variant_unknown) }
        binding.startButton.isEnabled = !FirmwareUpdater.isBusy
    }

    private fun confirmAndStart() {
        val info = imageInfo ?: return
        val deviceVariant = DeviceDriver.general.buildVariant

        // Variant guard: refuse a definite mismatch (an "ei" build on a "no-ai"
        // device or vice versa); warn when the filename didn't reveal a variant.
        if (imageVariant.isNotEmpty() && deviceVariant.isNotEmpty() && imageVariant != deviceVariant) {
            showModalAlert(
                getString(R.string.firmware_variant_mismatch_title),
                getString(R.string.firmware_variant_mismatch, imageVariant, deviceVariant)
            )
            return
        }

        var warning = if (imageVariant.isEmpty()) {
            getString(R.string.firmware_variant_unknown_warning) + "\n\n"
        } else {
            ""
        }
        // Reverting below the first firmware that speaks the BT update protocol is
        // one-way: the app cannot update that device again. Warn and allow — this
        // path exists precisely so a bad release can be undone.
        if (FirmwareVersion.isBelowOtaSource(info.version)) {
            warning += getString(
                R.string.firmware_revert_warning,
                info.version,
                FirmwareVersion.MIN_OTA_SOURCE_VERSION
            ) + "\n\n"
        }
        showModalOptionAlert(
            getString(R.string.firmware_update),
            warning + getString(
                R.string.firmware_confirm_update,
                DeviceDriver.general.version,
                info.version
            ),
            getString(R.string.firmware_update_now),
            positiveCallback = { startUpdate() }
        )
    }

    private fun startUpdate() {
        val file = stagedFile ?: return
        val info = imageInfo ?: return
        // Preflight the app can see immediately: recording/AI must be off. The
        // firmware enforces this (and battery/SD checks) again in Begin.
        val recordingOff =
            DeviceDriver.session.recordingState == RecordState.RecordOffDetectOff
        if (!recordingOff) {
            binding.stopRecordingButton.visibility = View.VISIBLE
            showModalAlert(
                getString(R.string.firmware_update),
                getString(R.string.firmware_stop_recording_first)
            )
            return
        }
        FirmwareUpdater.startUpdate(file, info.sha256, info.version, imageVariant)
        FirmwareUpdateService.start(this)
    }

    private fun stopRecording() {
        binding.stopRecordingButton.isEnabled = false
        DeviceDriver.setRecordState(RecordState.RecordOffDetectOff, null) {
            runOnUiThread {
                binding.stopRecordingButton.isEnabled = true
                binding.stopRecordingButton.visibility = View.GONE
            }
        }
    }

    private fun onUpdaterState(state: FirmwareUpdater.State, message: String) {
        runOnUiThread {
            binding.statusText.text = message
            val busy = FirmwareUpdater.isBusy
            binding.startButton.isEnabled = !busy && (imageInfo != null)
            binding.chooseFileButton.isEnabled = !busy
            binding.abortButton.visibility =
                if (state == FirmwareUpdater.State.Transferring) View.VISIBLE else View.GONE
            binding.progressBar.visibility = if (busy) View.VISIBLE else View.GONE
            binding.progressBar.isIndeterminate = state != FirmwareUpdater.State.Transferring

            val icon = when (state) {
                FirmwareUpdater.State.Success -> "✅ "
                FirmwareUpdater.State.RolledBack, FirmwareUpdater.State.Failed -> "⚠️ "
                else -> ""
            }
            if (icon.isNotEmpty()) {
                binding.statusText.text = icon + message
                binding.installedVersionValue.text = DeviceDriver.general.version
            }
        }
    }

    private fun onUpdaterProgress(sent: Long, total: Long, rate: Double) {
        runOnUiThread {
            if (total > 0) {
                val percent = ((sent * 100) / total).toInt()
                binding.progressBar.isIndeterminate = false
                binding.progressBar.max = 100
                binding.progressBar.progress = percent
                val remaining = if (rate > 1) {
                    val eta = ((total - sent) / rate).toInt()
                    getString(R.string.firmware_progress_eta, eta)
                } else {
                    ""
                }
                binding.statusText.text = getString(
                    R.string.firmware_progress_detail,
                    formatSize(sent),
                    formatSize(total),
                    (rate / 1024).toInt()
                ) + remaining
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        return if (bytes >= 1024 * 1024) {
            String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
        } else {
            String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        }
    }
}
