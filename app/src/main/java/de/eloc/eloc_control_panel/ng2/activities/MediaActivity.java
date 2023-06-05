package de.eloc.eloc_control_panel.ng2.activities;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;

public interface MediaActivity {
    void setImage(Uri src);

    void takePhoto();

    void pickImage();

    default Bitmap downScaleAvatar(Bitmap bitmap) {
        final int maxDimension = 800;
        int bitmapHeight = bitmap.getHeight();
        int bitmapWidth = bitmap.getWidth();
        int bigDimension = Math.max(bitmapWidth, bitmapHeight);
        if (bigDimension > maxDimension) {
            float scaleFactor = (float) maxDimension / bigDimension;
            Matrix matrix = new Matrix();
            matrix.setScale(scaleFactor, scaleFactor);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, matrix, false);
        }
        return bitmap;
    }
}
