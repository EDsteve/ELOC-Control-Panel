package de.eloc.eloc_control_panel.ng3.adapters.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView

open class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    companion object {
        const val PAGE_RECORDER = 0
        const val PAGE_DETECTOR = 1
    }
}