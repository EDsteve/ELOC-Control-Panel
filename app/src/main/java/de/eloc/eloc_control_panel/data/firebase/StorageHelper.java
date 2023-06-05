package de.eloc.eloc_control_panel.data.firebase;

import android.graphics.Bitmap;
import android.net.Uri;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.eloc.eloc_control_panel.data.DataHelper;
import de.eloc.eloc_control_panel.ng2.interfaces.BooleanCallback;
import de.eloc.eloc_control_panel.ng2.interfaces.StringCallback;

public class StorageHelper {
    private static final String REF_PROFILE_PICTURES = "profile_pictures";
    private static StorageHelper instance;

    public static StorageHelper getInstance() {
        if (instance == null) {
            instance = new StorageHelper();
        }
        return instance;
    }

    private final StorageReference profilePicturesFolder;

    private StorageHelper() {
        profilePicturesFolder = FirebaseStorage.getInstance().getReference(REF_PROFILE_PICTURES);
    }

    public void uploadProfilePicture(String id, Bitmap bitmap, StringCallback callback) {
        File temp = DataHelper.getTempFile();

        boolean saved = false;
        try (FileOutputStream fos = new FileOutputStream(temp)) {
            saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException ignore) {

        }

        if (saved) {
            Uri tempUri = DataHelper.getUriForFile(temp);
            final StorageReference remoteProfilePicture = profilePicturesFolder.child(id).child("profile_picture");
            remoteProfilePicture.putFile(tempUri).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    remoteProfilePicture.getDownloadUrl().addOnCompleteListener(urlTask -> {
                        String downloadUrl = "";
                        if (urlTask.isSuccessful()) {
                            Uri uri = urlTask.getResult();
                            if (uri != null) {
                                downloadUrl = uri.toString();
                            }
                        }
                        if (callback != null) {
                            callback.handler(downloadUrl);
                        }
                    });
                } else {
                    if (callback != null) {
                        callback.handler("");
                    }
                }
            });
        } else {
            if (callback != null) {
                callback.handler("");
            }
        }
    }

    public void deleteAccount(String id, BooleanCallback callback) {
        final AtomicInteger pendingItems = new AtomicInteger(0);
        BooleanCallback caller = b -> {
            if (callback != null) {
                callback.handler(b);
            }
        };

        // At present (May 2023), firebase storage does not allow deleting a folder
        // so list all files, then delete them one by one
        // https://stackoverflow.com/questions/37749647/firebasestorage-how-to-delete-directory
        final StorageReference accountDirectory = profilePicturesFolder.child(id);
        accountDirectory.listAll().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ListResult result = task.getResult();
                if (result != null) {
                    List<StorageReference> items = result.getItems();
                    pendingItems.set(items.size());
                    if (pendingItems.get() <= 0) {
                        caller.handler(true);
                    } else {
                        for (StorageReference file : items) {
                            file.delete().addOnCompleteListener(d -> {
                                int remaining = pendingItems.decrementAndGet();
                                if (remaining <= 0) {
                                    caller.handler(true);
                                }
                            });
                        }
                    }
                }
            } else {
                caller.handler(false);
            }
        });
    }
}
