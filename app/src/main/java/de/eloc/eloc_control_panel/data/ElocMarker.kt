package de.eloc.eloc_control_panel.data

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class ElocMarker(
    private val title: String,
    private val snippet: String,
    private val position: LatLng
) : ClusterItem {

    override fun getPosition(): LatLng = position

    override fun getTitle(): String = title

    override fun getSnippet(): String = snippet

    override fun getZIndex(): Float = 0f
}
