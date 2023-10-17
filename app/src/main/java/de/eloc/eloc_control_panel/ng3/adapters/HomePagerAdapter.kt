package de.eloc.eloc_control_panel.ng3.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.LayoutDetectorBinding
import de.eloc.eloc_control_panel.databinding.LayoutRecorderBinding
import de.eloc.eloc_control_panel.ng3.adapters.viewholders.DetectorPageViewHolder
import de.eloc.eloc_control_panel.ng3.adapters.viewholders.PageViewHolder
import de.eloc.eloc_control_panel.ng3.adapters.viewholders.RecorderPageViewHolder

class HomePagerAdapter(context: Context) : RecyclerView.Adapter<PageViewHolder>() {
    private val pages: Array<PageViewHolder?> = arrayOf(null, null)
    private val titles: Array<String>

    init {
        titles = arrayOf(context.getString(R.string.recorder), context.getString(R.string.detector))
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) PageViewHolder.PAGE_RECORDER else PageViewHolder.PAGE_DETECTOR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            PageViewHolder.PAGE_RECORDER -> {
                var page = pages[0]
                if (page == null) {
                    val recorderBinding = LayoutRecorderBinding.inflate(inflater, parent, false)
                    page = RecorderPageViewHolder(recorderBinding)
                    pages[0] = page
                }
                page
            }

            else -> {
                var page = pages[1]
                if (page == null) {
                    val detectorBinding = LayoutDetectorBinding.inflate(inflater, parent, false)
                    page = DetectorPageViewHolder(detectorBinding)
                    pages[1] = page
                }
                page
            }
        }
    }

    fun getTitle(position: Int) = titles[position]

    override fun getItemCount(): Int = pages.size

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        when (position) {
            0 -> {
                if (holder is RecorderPageViewHolder) {
                    holder.setText("Recorder 11")
                }
            }

            1 -> {
                if (holder is DetectorPageViewHolder) {
                    holder.setText("Detector 33")
                }
            }
        }
    }
}