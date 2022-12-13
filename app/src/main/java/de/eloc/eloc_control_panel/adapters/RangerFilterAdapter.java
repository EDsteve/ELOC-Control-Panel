package de.eloc.eloc_control_panel.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;

import de.eloc.eloc_control_panel.databinding.LayoutRangerFilterItemBinding;

public class RangerFilterAdapter extends RecyclerView.Adapter<RangerFilterItem> {

    private final String[] rangers;
    private static final HashSet<String> filter = new HashSet<>();

    public RangerFilterAdapter(String[] rangerNames) {
        rangers = rangerNames;
    }

    @NonNull
    @Override
    public RangerFilterItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        LayoutRangerFilterItemBinding binding = LayoutRangerFilterItemBinding.inflate(inflater);
        return new RangerFilterItem(binding.getRoot(), RangerFilterAdapter::modifyFilter);
    }

    @Override
    public void onBindViewHolder(@NonNull RangerFilterItem holder, int position) {
        String name = rangers[position];
        holder.setRangerName(name);
        boolean selected = filter.contains(name);
        holder.setSelected(selected);
    }

    @Override
    public int getItemCount() {
        return rangers.length;
    }

    private static void modifyFilter(String name, boolean select) {
        if (select) {
            filter.add(name);
        } else {
            filter.remove(name);
        }
    }

    public static HashSet<String> getFilter() {
        return filter;
    }

}
