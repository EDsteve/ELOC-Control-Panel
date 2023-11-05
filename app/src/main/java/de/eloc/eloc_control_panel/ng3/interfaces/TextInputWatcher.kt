package de.eloc.eloc_control_panel.ng3.interfaces

import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputLayout

class TextInputWatcher(private val layout: TextInputLayout): TextWatcher  {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
      layout.error = null
    }

    override fun afterTextChanged(s: Editable) {}
}
