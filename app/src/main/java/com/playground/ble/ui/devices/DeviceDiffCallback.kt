package com.playground.ble.ui.devices

import androidx.recyclerview.widget.DiffUtil
import com.playground.ble.data.device.model.Device

class DeviceDiffCallback(private val old: List<Device>, private val new: List<Device>) :
    DiffUtil.Callback() {
    override fun getOldListSize() = old.size

    override fun getNewListSize() = new.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        old[oldItemPosition].id == new[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        old[oldItemPosition].id == new[newItemPosition].id &&
                old[oldItemPosition].name == new[newItemPosition].name
}