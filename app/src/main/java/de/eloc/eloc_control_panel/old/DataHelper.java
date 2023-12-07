package de.eloc.eloc_control_panel.old;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;

import de.eloc.eloc_control_panel.App;

public class DataHelper {
    private DataHelper() {
    }

    public static Uri getUriForFile(File file) {
        Context context = App.Companion.getInstance();
        return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
    }

    public static File getTempFile() {
        Context context = App.Companion.getInstance();
        return new File(context.getCacheDir(), String.valueOf(System.currentTimeMillis()));
    }

    public static Uri getTempFileUri() {
        return getUriForFile(getTempFile());
    }
}
