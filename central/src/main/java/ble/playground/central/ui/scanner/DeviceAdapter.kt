package ble.playground.central.ui.scanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ble.playground.central.R
import ble.playground.central.entity.Device

class DeviceAdapter : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    var items: List<Device> = emptyList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(DeviceDiffCallback(field, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    var itemClickListener: (Device) -> Unit = {}

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.scanner_item_title)
        val subtitle: TextView = view.findViewById(R.id.scanner_item_subtitle)

        init {
            view.setOnClickListener {
                itemClickListener(items[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_device, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.title.text = items[position].name
        viewHolder.subtitle.text = items[position].id
    }

    override fun getItemCount() = items.size

}