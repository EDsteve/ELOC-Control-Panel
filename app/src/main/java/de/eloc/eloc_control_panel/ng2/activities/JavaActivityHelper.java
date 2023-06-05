package de.eloc.eloc_control_panel.ng2.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import de.eloc.eloc_control_panel.databinding.LayoutAlertOkBinding;
import de.eloc.eloc_control_panel.ng2.interfaces.VoidCallback;

// todo: translate to kotlin later
public class JavaActivityHelper {

    private JavaActivityHelper() {
    }

    public static void open(AppCompatActivity activity, Class<?> target, boolean finish) {
        if (activity != null) {
            Intent intent = new Intent(activity, target);
            activity.startActivity(intent);
            if (finish) {
                activity.finish();
            }
        }
    }

    private static void showDialog(Context context, String title, String message, VoidCallback callback) {
        if (message == null) {
            message = "";
        }
        message = message.trim();
        if ((context == null) || message.isEmpty()) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        LayoutAlertOkBinding binding = LayoutAlertOkBinding.inflate(inflater);
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(binding.getRoot());
        binding.titleTextView.setText(title);
        binding.messageTextView.setText(message);
        binding.okButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) {
                callback.handler();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    public static void showModalAlert(Context context, String title, String message) {
        showDialog(context, title, message, null);
    }

    public static void showModalAlert(Context context, String title, String message, VoidCallback callback) {
        showDialog(context, title, message, callback);
    }

    public static PickVisualMediaRequest getPickImageRequest() {
        return new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build();

    }
}
