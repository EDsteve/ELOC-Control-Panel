package de.eloc.eloc_control_panel.ng2.activities;

import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.textfield.TextInputLayout;

public class TextInputWatcher implements TextWatcher {
    private final TextInputLayout layout;

    public TextInputWatcher(TextInputLayout layout) {
        this.layout = layout;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        layout.setError(null);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}
