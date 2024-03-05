package de.eloc.eloc_control_panel.data.helpers.firebase

import android.graphics.Bitmap
import com.google.firebase.storage.FirebaseStorage
import de.eloc.eloc_control_panel.old.DataHelper
import de.eloc.eloc_control_panel.interfaces.BooleanCallback
import de.eloc.eloc_control_panel.interfaces.StringCallback
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger

class StorageHelper {
    companion object {
        private const val REF_PROFILE_PICTURES = "profile_pictures"
        val instance = StorageHelper()
    }

    private val profilePicturesFolder =
        FirebaseStorage.getInstance().getReference(REF_PROFILE_PICTURES)

    fun uploadProfilePicture(id: String, bitmap: Bitmap, callback: StringCallback?) {
        val temp = DataHelper.getTempFile()

        var saved = false
        try {
            FileOutputStream(temp).use { fos ->
                saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
        } catch (_: Exception) {
        }

        if (saved) {
            val tempUri = DataHelper.getUriForFile(temp)
            val remoteProfilePicture = profilePicturesFolder.child(id).child("profile_picture")
            remoteProfilePicture.putFile(tempUri).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    remoteProfilePicture.downloadUrl.addOnCompleteListener { urlTask ->
                        var downloadUrl = ""
                        if (urlTask.isSuccessful) {
                            val uri = urlTask.result
                            if (uri != null) {
                                downloadUrl = uri.toString()
                            }
                        }
                        callback?.handler(downloadUrl)
                    }
                } else {
                    callback?.handler("")
                }
            }
        } else {
            callback?.handler("")
        }
    }

    fun deleteAccount(id: String, callback: BooleanCallback?) {
        val pendingItems = AtomicInteger(0)

        // At present (May 2023), firebase storage does not allow deleting a folder
        // so list all files, then delete them one by one
        // https://stackoverflow.com/questions/37749647/firebasestorage-how-to-delete-directory
        val accountDirectory = profilePicturesFolder.child(id)
        accountDirectory.listAll().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val result = task.result
                if (result != null) {
                    val items = result.items
                    pendingItems.set(items.size)
                    if (pendingItems.get() <= 0) {
                        callback?.handler(true)
                    } else {
                        for (file in items) {
                            file.delete().addOnCompleteListener {
                                val remaining = pendingItems.decrementAndGet()
                                if (remaining <= 0) {
                                    callback?.handler(true)
                                }
                            }
                        }
                    }
                }
            } else {
                callback?.handler(false)
            }
        }
    }
}
