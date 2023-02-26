package de.eloc.eloc_control_panel.ng2.models

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.eloc.eloc_control_panel.databinding.LayoutElocInfoBinding
import de.eloc.eloc_control_panel.ng2.interfaces.AdapterItemCallback
import de.eloc.eloc_control_panel.ng2.interfaces.ListUpdateCallback

import java.util.ArrayList

class ElocInfoAdapter(val callback: ListUpdateCallback, private val itemCallback: AdapterItemCallback) :
    RecyclerView.Adapter<ElocInfoViewHolder>() {
    private val deviceInfos = ArrayList<ElocInfo>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElocInfoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LayoutElocInfoBinding.inflate(inflater, parent, false)
        return ElocInfoViewHolder(binding.root)
    }

    override fun getItemCount(): Int = deviceInfos.size

    override fun onBindViewHolder(holder: ElocInfoViewHolder, position: Int) {
        val info = deviceInfos[position]
        holder.setInfo(info, itemCallback)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        deviceInfos.clear()
        notifyDataSetChanged()
    }

    fun add(info: ElocInfo) {
        if (info.isValidDevice) {
            if (!deviceInfos.contains(info)) {
                deviceInfos.add(info)
                notifyItemInserted(deviceInfos.size - 1)
                callback.handler(deviceInfos.isEmpty(), false)
            }
        }
    }
}
