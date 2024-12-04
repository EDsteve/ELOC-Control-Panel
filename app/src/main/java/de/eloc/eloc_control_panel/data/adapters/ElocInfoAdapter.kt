package de.eloc.eloc_control_panel.data.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.eloc.eloc_control_panel.data.BtDevice
import de.eloc.eloc_control_panel.data.viewholders.BtDeviceViewHolder
import de.eloc.eloc_control_panel.databinding.LayoutElocInfoBinding
import de.eloc.eloc_control_panel.interfaces.BooleanCallback

class ElocInfoAdapter(
    private val checkable: Boolean,
    val callback: BooleanCallback? = null,
    private val itemCallback: ((BtDevice) -> Unit)? = null
) :
    RecyclerView.Adapter<BtDeviceViewHolder>() {
    private val deviceList = mutableListOf<BtDevice>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BtDeviceViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LayoutElocInfoBinding.inflate(inflater, parent, false)
        return BtDeviceViewHolder(binding.root)
    }

    override fun getItemCount(): Int = deviceList.size

    override fun onBindViewHolder(holder: BtDeviceViewHolder, position: Int) {
        val info = deviceList[position]
        holder.setInfo(info, checkable, itemCallback)
    }

    fun getSelected(): List<BtDevice> {
        val selected = mutableListOf<BtDevice>()
        for (device in deviceList) {
            if (device.selected) {
                selected.add(device)
            }
        }
        return selected
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        deviceList.clear()
        notifyDataSetChanged()
    }

    fun add(device: BtDevice, skipElocCheck: Boolean = false) {
        val validDevice = if (skipElocCheck) true else device.isEloc()
        if (validDevice) {
            if (!deviceList.contains(device)) {
                deviceList.add(device)
                notifyItemInserted(deviceList.size - 1)
                callback?.handler(false)
            }
        }
    }
}
