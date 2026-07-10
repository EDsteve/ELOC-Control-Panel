package de.eloc.eloc_control_panel.driver

import de.eloc.eloc_control_panel.data.ConnectionStatus
import de.eloc.eloc_control_panel.data.helpers.JsonHelper
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.zip.CRC32

/**
 * Bluetooth firmware transfer engine (Phase 2 of the firmware-update plan;
 * protocol counterpart of the firmware's FwUpdateTransfer/FwFrameParser).
 *
 * Flow: setFwUpdateBegin#meta={...} -> stream stop-and-wait binary frames
 * [seq:u16 LE][len:u16 LE][payload][crc32:u32 LE] (CRC over seq+len+payload,
 * java.util.zip.CRC32 == the firmware's software CRC32) -> setFwUpdateApply ->
 * device restarts and flashes -> reconnect -> verify reported version.
 *
 * All socket writes go through [DeviceDriver] ([DeviceDriver.writeRaw] for
 * frames); acks arrive via [DeviceDriver.fwFrameListener] from the normal
 * EOT-JSON read loop. BT drops and firmware-side timeouts are recovered by
 * reconnecting and re-issuing Begin, which returns the resume offset.
 */
object FirmwareUpdater {

    enum class State {
        Idle,
        Starting,       // preflight/Begin exchange
        Transferring,   // streaming frames
        Applying,       // staged; setFwUpdateApply sent, device about to restart
        Reconnecting,   // waiting for the device to come back (flash + reboot)
        Verifying,      // reading getStatus to confirm the new version
        Success,
        RolledBack,     // device came back with the old version
        Aborted,
        Failed,
    }

    private const val CHUNK_SIZE = 4096
    private const val BEGIN_TIMEOUT_MS = 20_000L
    private const val ACK_TIMEOUT_MS = 10_000L
    private const val APPLY_TIMEOUT_MS = 30_000L
    private const val MAX_TRANSFER_ATTEMPTS = 5
    // Device reboots twice after apply (trigger boot + flash, then final boot).
    private const val RECONNECT_TIMEOUT_MS = 150_000L
    private const val RECONNECT_RETRY_INTERVAL_MS = 5_000L

    @Volatile
    var state = State.Idle
        private set

    @Volatile
    var lastMessage = ""
        private set

    @Volatile
    var bytesSent = 0L
        private set

    @Volatile
    var totalBytes = 0L
        private set

    @Volatile
    var bytesPerSecond = 0.0
        private set

    val isBusy
        get() = when (state) {
            State.Idle, State.Success, State.RolledBack, State.Aborted, State.Failed -> false
            else -> true
        }

    private val stateListeners = mutableMapOf<String, (State, String) -> Unit>()
    private val progressListeners = mutableMapOf<String, (Long, Long, Double) -> Unit>()

    private val commandResponses = ArrayBlockingQueue<String>(4)
    private val frameAcks = ArrayBlockingQueue<JSONObject>(8)

    @Volatile
    private var abortRequested = false

    @Volatile
    private var connectionStatus = ConnectionStatus.Inactive

    private val connectionListenerId = "firmwareUpdater"

    fun addStateListener(id: String, listener: (State, String) -> Unit) {
        synchronized(stateListeners) { stateListeners[id] = listener }
    }

    fun removeStateListener(id: String) {
        synchronized(stateListeners) { stateListeners.remove(id) }
    }

    fun addProgressListener(id: String, listener: (Long, Long, Double) -> Unit) {
        synchronized(progressListeners) { progressListeners[id] = listener }
    }

    fun removeProgressListener(id: String) {
        synchronized(progressListeners) { progressListeners.remove(id) }
    }

    /** Ask a running transfer to stop cleanly. The partial stays on the device for resume. */
    fun abort() {
        abortRequested = true
    }

    /**
     * Start an update with a locally staged binary. Must not be called while
     * [isBusy]. [sha256] and [imageVersion] come from [FirmwareImage.inspect].
     */
    fun startUpdate(file: File, sha256: String, imageVersion: String, variant: String) {
        if (isBusy) {
            return
        }
        abortRequested = false
        bytesSent = 0
        totalBytes = file.length()
        bytesPerSecond = 0.0
        setState(State.Starting, "Preparing update…")
        Thread {
            runUpdate(file, sha256, imageVersion, variant)
        }.start()
    }

    private fun setState(newState: State, message: String) {
        state = newState
        lastMessage = message
        val listeners = synchronized(stateListeners) { stateListeners.values.toList() }
        listeners.forEach {
            try {
                it(newState, message)
            } catch (_: Exception) {
            }
        }
    }

    private fun notifyProgress() {
        val listeners = synchronized(progressListeners) { progressListeners.values.toList() }
        listeners.forEach {
            try {
                it(bytesSent, totalBytes, bytesPerSecond)
            } catch (_: Exception) {
            }
        }
    }

    private fun runUpdate(file: File, sha256: String, imageVersion: String, variant: String) {
        val oldVersion = DeviceDriver.general.version
        DeviceDriver.addConnectionChangedListener(connectionListenerId) { connectionStatus = it }
        connectionStatus =
            if (DeviceDriver.isConnected) ConnectionStatus.Active else ConnectionStatus.Inactive
        DeviceDriver.fwFrameListener = { json ->
            try {
                frameAcks.offer(JSONObject(json))
            } catch (_: Exception) {
            }
        }

        try {
            // ---- stage the binary onto the device's SD card -----------------
            var attempt = 0
            var staged = false
            while (!staged) {
                if (abortRequested) {
                    finishAborted()
                    return
                }
                attempt++
                if (attempt > MAX_TRANSFER_ATTEMPTS) {
                    setState(State.Failed, "Transfer failed after $MAX_TRANSFER_ATTEMPTS attempts")
                    return
                }

                if (!DeviceDriver.isConnected && !reconnect()) {
                    setState(State.Failed, "Could not reconnect to the device")
                    return
                }

                setState(State.Starting, "Requesting transfer…")
                val begin = sendCommandAwait(
                    "setFwUpdateBegin#meta=" +
                            """{"size":$totalBytes,"sha256":"$sha256","version":"$imageVersion","variant":"$variant","chunkSize":$CHUNK_SIZE}""",
                    BEGIN_TIMEOUT_MS
                )
                if (begin == null) {
                    // no response — link may have died; retry via reconnect
                    continue
                }
                val ecode = begin.optInt("ecode", -1)
                if (ecode != 0) {
                    // Preflight refusals (recording on, low battery, no SD…) are
                    // final — retrying won't change them. Show the device's reason.
                    val reason = begin.optString("error", "device refused the transfer")
                    setState(State.Failed, reason)
                    return
                }
                val resumeOffset =
                    JsonHelper.getJSONNumberAttribute("payload:resumeOffset", begin).toLong()
                val chunkSize =
                    JsonHelper.getJSONNumberAttribute("payload:chunkSize", begin, CHUNK_SIZE.toDouble())
                        .toInt()
                val beginState = JsonHelper.getJSONStringAttribute("payload:state", begin)

                staged = when {
                    // Newer firmware reports an already-complete staged file
                    // directly and never enters binary mode for it.
                    beginState == "staged" -> {
                        bytesSent = totalBytes
                        notifyProgress()
                        true
                    }
                    // Older firmware (V1.47..V1.50) enters binary mode even when
                    // nothing is left to receive; the end-of-stream sentinel makes
                    // it ack "staged" and leave binary mode cleanly.
                    resumeOffset >= totalBytes -> finishFullyStagedResume()

                    else -> streamFrames(file, resumeOffset, chunkSize)
                }
                if (abortRequested && !staged) {
                    finishAborted()
                    return
                }
            }

            // ---- apply: firmware verifies SHA-256 and restarts to flash ------
            setState(State.Applying, "Verifying and applying — the device will restart…")
            val apply = sendCommandAwait("setFwUpdateApply", APPLY_TIMEOUT_MS)
            if (apply == null) {
                setState(State.Failed, "No response to apply request")
                return
            }
            if (apply.optInt("ecode", -1) != 0) {
                setState(State.Failed, apply.optString("error", "device refused to apply the update"))
                return
            }

            // ---- wait out the double reboot, then reconnect and verify -------
            setState(State.Reconnecting, "Device is flashing and restarting…")
            try {
                Thread.sleep(10_000)
            } catch (_: Exception) {
            }
            if (!reconnect()) {
                setState(
                    State.Failed,
                    "Update was applied but the device did not reconnect — check it manually"
                )
                return
            }

            setState(State.Verifying, "Checking firmware version…")
            val newVersion = readDeviceVersion()
            when {
                newVersion.isEmpty() ->
                    setState(
                        State.Failed,
                        "Reconnected but could not read the firmware version"
                    )

                newVersion == oldVersion && imageVersion != oldVersion ->
                    setState(
                        State.RolledBack,
                        "Device still reports $oldVersion — the update failed and was rolled back"
                    )

                else ->
                    setState(State.Success, "Device updated — now running $newVersion")
            }
        } catch (e: Exception) {
            setState(State.Failed, "Update error: ${e.message}")
        } finally {
            DeviceDriver.firmwareTransferActive = false
            DeviceDriver.fwFrameListener = null
            DeviceDriver.removeConnectionChangedListener(connectionListenerId)
        }
    }

    private fun finishAborted() {
        // Keep the partial file on the device: a later attempt resumes from it.
        sendCommandAwait("setFwUpdateAbort", 5_000L)
        setState(State.Aborted, "Update cancelled — progress is kept for resume")
    }

    /**
     * The device already holds the complete file but entered binary frame mode
     * anyway (firmware V1.47..V1.50 does this on a fully-staged resume). Send
     * the len==0 end-of-stream sentinel: the firmware acks it with
     * state "staged" and leaves binary mode. Returns true when that ack arrived.
     */
    private fun finishFullyStagedResume(): Boolean {
        DeviceDriver.firmwareTransferActive = true
        frameAcks.clear()
        try {
            if (!DeviceDriver.writeRaw(buildFrame(0, ByteArray(0), 0))) {
                return false
            }
            val ack = awaitAck(0) ?: return false
            if (ack.optInt("ecode", -1) != 0) {
                return false
            }
            val staged = JsonHelper.getJSONStringAttribute("payload:state", ack) == "staged"
            if (staged) {
                bytesSent = totalBytes
                notifyProgress()
            }
            return staged
        } finally {
            DeviceDriver.firmwareTransferActive = false
        }
    }

    /**
     * Stream the file from [startOffset] in stop-and-wait frames.
     * Returns true when the device reports the image fully staged.
     * Returns false to trigger a resume attempt (or abort/disconnect handling).
     */
    private fun streamFrames(file: File, startOffset: Long, chunkSize: Int): Boolean {
        setState(State.Transferring, "Sending firmware…")
        DeviceDriver.firmwareTransferActive = true
        frameAcks.clear()
        val sessionStart = System.currentTimeMillis()
        var sessionBytes = 0L
        try {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(startOffset)
                var offset = startOffset
                var seq = 0
                bytesSent = offset
                notifyProgress()
                val payload = ByteArray(chunkSize)
                while (offset < totalBytes) {
                    if (abortRequested) {
                        // In-band end-of-stream sentinel (len == 0): the firmware
                        // leaves binary mode cleanly and keeps the partial file.
                        DeviceDriver.writeRaw(buildFrame(seq, payload, 0))
                        awaitAck(seq)
                        return false
                    }
                    val len = raf.read(payload, 0, minOf(chunkSize.toLong(), totalBytes - offset).toInt())
                    if (len <= 0) {
                        return false
                    }
                    if (!DeviceDriver.writeRaw(buildFrame(seq, payload, len))) {
                        return false  // socket died — resume after reconnect
                    }
                    val ack = awaitAck(seq) ?: return false
                    if (ack.optInt("ecode", -1) != 0) {
                        // NAK: the firmware left binary mode and kept the partial
                        // file; the next Begin resumes from its offset.
                        return false
                    }
                    offset += len
                    sessionBytes += len
                    seq++
                    bytesSent = offset
                    val elapsed = (System.currentTimeMillis() - sessionStart) / 1000.0
                    if (elapsed > 0.5) {
                        bytesPerSecond = sessionBytes / elapsed
                    }
                    notifyProgress()
                    if (JsonHelper.getJSONStringAttribute("payload:state", ack) == "staged") {
                        return offset >= totalBytes
                    }
                }
                // Sent everything but never saw "staged" — treat as retryable.
                return false
            }
        } finally {
            DeviceDriver.firmwareTransferActive = false
        }
    }

    private fun buildFrame(seq: Int, payload: ByteArray, len: Int): ByteArray {
        val frame = ByteArray(4 + len + 4)
        frame[0] = (seq and 0xFF).toByte()
        frame[1] = ((seq shr 8) and 0xFF).toByte()
        frame[2] = (len and 0xFF).toByte()
        frame[3] = ((len shr 8) and 0xFF).toByte()
        System.arraycopy(payload, 0, frame, 4, len)
        val crc = CRC32()
        crc.update(frame, 0, 4 + len)
        val crcValue = crc.value
        frame[4 + len] = (crcValue and 0xFF).toByte()
        frame[5 + len] = ((crcValue shr 8) and 0xFF).toByte()
        frame[6 + len] = ((crcValue shr 16) and 0xFF).toByte()
        frame[7 + len] = ((crcValue shr 24) and 0xFF).toByte()
        return frame
    }

    private fun awaitAck(seq: Int): JSONObject? {
        val deadline = System.currentTimeMillis() + ACK_TIMEOUT_MS
        while (true) {
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0) {
                return null
            }
            val ack = frameAcks.poll(remaining, TimeUnit.MILLISECONDS) ?: return null
            val ackSeq = JsonHelper.getJSONNumberAttribute("payload:seq", ack, -1.0).toInt()
            if (ackSeq == seq || ack.optInt("ecode", -1) != 0) {
                return ack
            }
            // stale ack from a previous session — keep waiting
        }
    }

    /** Send a command through the normal queue and wait for its response. */
    private fun sendCommandAwait(rawCommand: String, timeoutMs: Long): JSONObject? {
        commandResponses.clear()
        DeviceDriver.sendCustomCommand(rawCommand) { json ->
            commandResponses.offer(json)
        }
        val response = commandResponses.poll(timeoutMs, TimeUnit.MILLISECONDS) ?: return null
        return try {
            JSONObject(response)
        } catch (_: Exception) {
            null
        }
    }

    private fun reconnect(): Boolean {
        val address = DeviceDriver.deviceAddress ?: return false
        setState(State.Reconnecting, "Reconnecting to the device…")
        val deadline = System.currentTimeMillis() + RECONNECT_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (abortRequested) {
                return false
            }
            connectionStatus = ConnectionStatus.Inactive
            try {
                DeviceDriver.connect(address)
            } catch (_: Exception) {
            }
            // connect() is asynchronous; poll for the outcome of this attempt
            val attemptDeadline =
                minOf(System.currentTimeMillis() + 15_000L, deadline)
            while (System.currentTimeMillis() < attemptDeadline) {
                if (DeviceDriver.isConnected && connectionStatus == ConnectionStatus.Active) {
                    // Give the firmware a moment to send its greeting before
                    // the next command goes out.
                    try {
                        Thread.sleep(2_000)
                    } catch (_: Exception) {
                    }
                    return true
                }
                try {
                    Thread.sleep(500)
                } catch (_: Exception) {
                }
            }
            try {
                Thread.sleep(RECONNECT_RETRY_INTERVAL_MS)
            } catch (_: Exception) {
            }
        }
        return false
    }

    private fun readDeviceVersion(): String {
        val response = ArrayBlockingQueue<String>(2)
        DeviceDriver.getStatus(saveToDatabase = false) { json ->
            response.offer(json)
        }
        response.poll(BEGIN_TIMEOUT_MS, TimeUnit.MILLISECONDS) ?: return ""
        return DeviceDriver.general.version
    }
}

/**
 * Local inspection of an ESP32 application image (.bin): validates the image
 * magic and reads the embedded esp_app_desc_t so the UI can show what is about
 * to be flashed, plus the SHA-256 the firmware verifies against.
 */
object FirmwareImage {
    data class Info(
        val size: Long,
        val sha256: String,
        val version: String,
        val projectName: String,
    )

    private const val ESP_IMAGE_MAGIC = 0xE9
    private const val APP_DESC_MAGIC = 0xABCD5432L
    // esp_app_desc_t sits after esp_image_header_t (24) + esp_image_segment_header_t (8)
    private const val APP_DESC_OFFSET = 32
    private const val VERSION_OFFSET = APP_DESC_OFFSET + 16
    private const val PROJECT_NAME_OFFSET = APP_DESC_OFFSET + 48

    /** Returns null if the file is not a valid ESP32 app image. */
    fun inspect(file: File): Info? {
        try {
            val header = ByteArray(PROJECT_NAME_OFFSET + 32)
            RandomAccessFile(file, "r").use { raf ->
                if (raf.read(header) != header.size) {
                    return null
                }
            }
            if ((header[0].toInt() and 0xFF) != ESP_IMAGE_MAGIC) {
                return null
            }
            var magicWord = 0L
            for (i in 3 downTo 0) {
                magicWord = (magicWord shl 8) or (header[APP_DESC_OFFSET + i].toLong() and 0xFF)
            }
            if (magicWord != APP_DESC_MAGIC) {
                return null
            }
            return Info(
                size = file.length(),
                sha256 = sha256(file),
                version = readCString(header, VERSION_OFFSET, 32),
                projectName = readCString(header, PROJECT_NAME_OFFSET, 32),
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun readCString(data: ByteArray, offset: Int, maxLength: Int): String {
        var end = offset
        while (end < offset + maxLength && data[end] != 0.toByte()) {
            end++
        }
        return String(data, offset, end - offset, Charsets.UTF_8)
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
