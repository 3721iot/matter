package com.dsh.tether.device.color.adapter

import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.dsh.tether.R
import com.dsh.tether.databinding.ItemDeviceColorViewBinding

class DeviceColorsViewHolder (
    private val binding : ItemDeviceColorViewBinding,
    itemClickPosition: (Int) -> Unit
): RecyclerView.ViewHolder(binding.root) {

    init {
        itemView.setOnClickListener{
            itemClickPosition(adapterPosition)
        }
    }

    fun bind(colorId: Int) {
        val drawable =
            AppCompatResources.getDrawable(itemView.context, R.drawable.shape_circle_filled)
        drawable?.setTint(colorId)
        binding.ivColor.setImageDrawable(drawable)
    }
}