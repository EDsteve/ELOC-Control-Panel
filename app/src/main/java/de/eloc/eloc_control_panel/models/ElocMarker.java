package de.eloc.eloc_control_panel.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class ElocMarker implements ClusterItem {
    private final String title;
    private final String snippet;
    private final LatLng position;

    public ElocMarker(String title, String snippet, LatLng position) {
        this.title = title;
        this.snippet = snippet;
        this.position = position;
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return position;
    }

    @Nullable
    @Override
    public String getTitle() {
        return title;
    }

    @Nullable
    @Override
    public String getSnippet() {
        return snippet;
    }

    @Nullable
    @Override
    public Float getZIndex() {
        return 0f;
    }
}
