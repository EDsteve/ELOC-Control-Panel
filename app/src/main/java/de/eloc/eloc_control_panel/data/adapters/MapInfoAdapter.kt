package de.eloc.eloc_control_panel.data.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.ElocDeviceInfo
import de.eloc.eloc_control_panel.databinding.LayoutMapTableItemBinding

class MapInfoAdapter(context: Context, resource: Int) :
    ArrayAdapter<ElocDeviceInfo>(context, resource) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val binding = if (convertView != null) {
            LayoutMapTableItemBinding.bind(convertView)
        } else {
            LayoutMapTableItemBinding.inflate(inflater, parent, false)
        }

        val item = getItem(position)
        if (item != null) {
            binding.titleTextView.text = item.name
            val coordinates = item.location
            binding.gpsLocationTextView.text = if (coordinates != null) {
                context.getString(
                    R.string.gps_location_template,
                    coordinates.latitude,
                    coordinates.latitude
                )
            } else {
                context.getString(R.string.gps_location_not_available)
            }
            binding.gpsAccuracyTextView.text =
                context.getString(R.string.gps_accuracy_template, item.accuracy)
            binding.batteryTextView.text =
                context.getString(R.string.battery_template, item.batteryVolts)
            binding.timeTextView.text =
                context.getString(R.string.recording_time_template, item.recTime)
            binding.timestampTextView.text =
                context.getString(R.string.timestamp_template, item.time)
        }
        return binding.root
    }
}