package de.eloc.eloc_control_panel.data.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.eloc.eloc_control_panel.data.BtDevice
import de.eloc.eloc_control_panel.data.viewholders.BtDeviceViewHolder
import de.eloc.eloc_control_panel.databinding.LayoutElocInfoBinding
import de.eloc.eloc_control_panel.interfaces.BooleanCallback
import de.eloc.eloc_control_panel.interfaces.StringCallback

import java.util.ArrayList

class ElocInfoAdapter(
    val callback: BooleanCallback,
    private val itemCallback: StringCallback
) :
    RecyclerView.Adapter<BtDeviceViewHolder>() {
    private val deviceInfos = ArrayList<BtDevice>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BtDeviceViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LayoutElocInfoBinding.inflate(inflater, parent, false)
        return BtDeviceViewHolder(binding.root)
    }

    override fun getItemCount(): Int = deviceInfos.size

    override fun onBindViewHolder(holder: BtDeviceViewHolder, position: Int) {
        val info = deviceInfos[position]
        holder.setInfo(info, itemCallback)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        deviceInfos.clear()
        notifyDataSetChanged()
    }

    fun add(device: BtDevice) {
        if (BtDevice.isEloc(device)) {
            if (!deviceInfos.contains(device)) {
                deviceInfos.add(device)
                notifyItemInserted(deviceInfos.size - 1)
                callback.handler(false)
            }
        }
    }
}
