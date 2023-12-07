package de.eloc.eloc_control_panel.data.helpers

import android.content.Context
import de.eloc.eloc_control_panel.App
import java.io.FileReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val UPD_EXTENSION = ".upd"
private const val TXT_EXTENSION = ".txt"

object FileSystemHelper {
    fun clearStatusFiles() {
        val fileArray = App.instance.filesDir.listFiles()
        if (fileArray != null) {
            for (file in fileArray) {
                if (file.name.endsWith(TXT_EXTENSION)) {
                    file.delete()
                }
            }
        }
    }

    fun clearPayloadFiles() {
        val fileArray = App.instance.filesDir.listFiles()
        if (fileArray != null) {
            for (file in fileArray) {
                if (file.name.endsWith(UPD_EXTENSION)) {
                    file.delete()
                }
            }
        }
    }

    fun getUploadStatusFileName(): String? {
        var hasContentToUpload = false
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val now = dateFormatter.format(Date())
        val fileName = "update $now$UPD_EXTENSION"
        val context = App.instance.applicationContext
        val statusDirectory = context.filesDir
        try {
            val fileArray = statusDirectory.listFiles()
            if (fileArray != null) {
                context.openFileOutput(fileName, Context.MODE_PRIVATE).use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        for (file in fileArray) {
                            if (!file.isDirectory) {
                                if (file.name.endsWith(".txt")) {
                                    hasContentToUpload = true
                                    val content = FileReader(file).readText()
                                    writer.write("$content\n\n\n")
                                    writer.flush()
                                }
                            }
                        }
                        writer.write("\n\n\n end of updates")
                    }
                }
            }
        } catch (_: Exception) {
        }

        return if (hasContentToUpload) {
            statusDirectory.absolutePath + "/" + fileName
        } else {
            clearPayloadFiles()
            null
        }
    }
}