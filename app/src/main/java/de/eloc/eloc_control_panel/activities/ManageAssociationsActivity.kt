package de.eloc.eloc_control_panel.activities

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.AssociatedDeviceInfo
import de.eloc.eloc_control_panel.data.BtDevice
import de.eloc.eloc_control_panel.data.adapters.ElocInfoAdapter
import de.eloc.eloc_control_panel.data.helpers.BluetoothHelper
import de.eloc.eloc_control_panel.databinding.ActivityManageAssociationsBinding

class ManageAssociationsActivity : ThemableActivity() {
    private lateinit var binding: ActivityManageAssociationsBinding
    private val adapter = ElocInfoAdapter(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageAssociationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        checkAssociatedDevices()
        setListeners()
    }

    private fun setListeners() {
        binding.unpairButton.setOnClickListener { disassociate() }
    }

    private fun checkAssociatedDevices() {
        binding.noPairsTextView.visibility = View.VISIBLE
        binding.unpairButton.visibility = View.GONE
        binding.pairedDevicesRecyclerView.visibility = View.GONE
        val associatedDevices = BluetoothHelper.associatedDevices(this)
        if (associatedDevices.isNotEmpty()) {
            binding.noPairsTextView.visibility = View.GONE
            binding.unpairButton.visibility = View.VISIBLE
            binding.pairedDevicesRecyclerView.visibility = View.VISIBLE

            adapter.clear()
            for (info in associatedDevices) {
                if (info.address != null) {
                    val device = BluetoothHelper.getDevice(info.address!!)
                    if (device != null) {
                        val btDevice = BtDevice(device, associationId = info.id)
                        adapter.add(btDevice, skipElocCheck = true)
                    }
                }
            }
            binding.pairedDevicesRecyclerView.adapter = adapter
            binding.pairedDevicesRecyclerView.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { goBack() }
    }

    private fun disassociate() {
        val selected = adapter.getSelected()
        if (selected.isEmpty()) {
            showModalAlert(
                getString(R.string.required),
                getString(R.string.no_paired_devices_selected)
            )
        } else {
            for (device in selected) {
                val info = AssociatedDeviceInfo(id = device.associationId, address = device.address)
                BluetoothHelper.disassociateDevice(this, info)
            }
            checkAssociatedDevices()
        }
    }
}