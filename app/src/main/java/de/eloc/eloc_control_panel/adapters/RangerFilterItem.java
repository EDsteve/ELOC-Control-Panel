package de.eloc.eloc_control_panel.adapters;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import de.eloc.eloc_control_panel.databinding.LayoutRangerFilterItemBinding;

public class RangerFilterItem extends RecyclerView.ViewHolder {
    private final LayoutRangerFilterItemBinding binding;

    public RangerFilterItem(@NonNull View itemView, ModifyFilterCallback callback) {
        super(itemView);
        binding = LayoutRangerFilterItemBinding.bind(itemView);

        binding.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (callback != null) {
                callback.modify(binding.rangerNameTextView.getText().toString(), isChecked);
            }
        });
    }

    void setSelected(boolean b) {
        binding.checkbox.setChecked(b);
    }

    void setRangerName(String name) {
        binding.rangerNameTextView.setText(name);
    }
}
