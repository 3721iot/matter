package com.dsh.tether.device.color.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.dsh.tether.R
import com.dsh.tether.databinding.ItemDeviceColorViewBinding

class DeviceColorsAdapter (
    private val onItemClickListener: ((Int) -> Unit) ? = null
) : ListAdapter<Int, DeviceColorsViewHolder>(DEV_COLOR_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceColorsViewHolder {
        val deviceColorView =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_device_color_view, parent, false)

        val binding = ItemDeviceColorViewBinding.bind(deviceColorView)
        val holder = DeviceColorsViewHolder(binding) {position ->
            val colorId = getItem(position)
            onItemClickListener?.invoke(colorId)
        }
        return holder
    }

    override fun onBindViewHolder(holder: DeviceColorsViewHolder, position: Int) {
        val colorId = getItem(position)
        holder.bind(colorId)
    }

    companion object {
        /**
         * DiffUtil is a utility class that calculates the difference between two lists and outputs a list
         * of update operations that converts the first list into the second one. It is used here to
         * calculate updates for the RecyclerView Adapter.
         */
        private val DEV_COLOR_COMPARATOR =
            object : DiffUtil.ItemCallback<Int> (){
                override fun areItemsTheSame(
                    oldItem: Int,
                    newItem: Int
                ): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(
                    oldItem: Int,
                    newItem: Int
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }
}