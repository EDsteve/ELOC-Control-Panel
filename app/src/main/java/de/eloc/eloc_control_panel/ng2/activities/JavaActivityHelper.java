package de.eloc.eloc_control_panel.ng2.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import de.eloc.eloc_control_panel.databinding.LayoutAlertOkBinding;
import de.eloc.eloc_control_panel.databinding.LayoutAlertOptionBinding;
import de.eloc.eloc_control_panel.ng2.interfaces.VoidCallback;

// todo: translate to kotlin later
// todo: set camera permissions
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

    public static void showModalOptionAlert(Context context, String title, String message, VoidCallback positiveCallback) {
        showModalOptionAlert(context, title, message, null, null, positiveCallback, null);
    }

    public static void showModalOptionAlert(Context context, String title, String message, String positiveButtonLabel, String negativeButtonLabel, VoidCallback positiveCallback, VoidCallback negativeCallback) {
        if (message == null) {
            message = "";
        }
        message = message.trim();
        if ((context == null) || message.isEmpty()) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        LayoutAlertOptionBinding binding = LayoutAlertOptionBinding.inflate(inflater);
        final Dialog dialog = new Dialog(context);

        dialog.setContentView(binding.getRoot());

        if (positiveButtonLabel == null) {
            positiveButtonLabel = "";
        }
        positiveButtonLabel = positiveButtonLabel.trim();
        if (positiveButtonLabel.isEmpty()) {
            positiveButtonLabel = context.getString(android.R.string.ok);
        }

        if (negativeButtonLabel == null) {
            negativeButtonLabel = "";
        }
        negativeButtonLabel = negativeButtonLabel.trim();
        if (negativeButtonLabel.isEmpty()) {
            negativeButtonLabel = context.getString(android.R.string.cancel);
        }

        binding.titleTextView.setText(title);
        binding.messageTextView.setText(message);
        binding.positiveButton.setText(positiveButtonLabel);
        binding.negativeButton.setText(negativeButtonLabel);
        binding.negativeButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (negativeCallback != null) {
                negativeCallback.handler();
            }
        });
        binding.positiveButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (positiveCallback != null) {
                positiveCallback.handler();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
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
