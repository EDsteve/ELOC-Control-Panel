package de.eloc.eloc_control_panel.ng2.activities;

import android.content.Intent;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

// todo: translate to kotlin later
// todo: set camera permissions
public class JavaActivityHelper {

    private JavaActivityHelper() {
    }



    public static PickVisualMediaRequest getPickImageRequest() {
        return new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build();

    }
}
