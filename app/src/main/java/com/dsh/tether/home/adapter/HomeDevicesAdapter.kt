package com.dsh.tether.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.dsh.tether.R
import com.dsh.tether.databinding.ItemHomeDeviceViewBinding
import com.dsh.tether.model.TetherDevice

class HomeDevicesAdapter(
    private val onItemClickListener: ((TetherDevice) -> Unit) ? = null,
    private val onItemLongClickListener: ((TetherDevice) -> Unit) ? = null
) : ListAdapter<TetherDevice, HomeDeviceViewHolder>(DEVICES_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeDeviceViewHolder {
        val homeDeviceView =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_home_device_view, parent, false)
        val mBinding = ItemHomeDeviceViewBinding.bind(homeDeviceView)
        val holder = HomeDeviceViewHolder(mBinding,
            { pos ->
                if(pos < 0 || pos >= itemCount){
                    return@HomeDeviceViewHolder
                }
                val thrDevice = getItem(pos)
                onItemClickListener?.invoke(thrDevice)
            },
            { pos ->
                if(pos < 0 || pos >= itemCount){
                    return@HomeDeviceViewHolder
                }
                val thrDevice = getItem(pos)
                onItemLongClickListener?.invoke(thrDevice)
            }
        )
        return holder
    }

    override fun onBindViewHolder(holder: HomeDeviceViewHolder, position: Int) {
        val thrDevice =  getItem(position)
        holder.bind(thrDevice)
    }

    companion object {
        /**
         * DiffUtil is a utility class that calculates the difference between two lists and outputs a list
         * of update operations that converts the first list into the second one. It is used here to
         * calculate updates for the RecyclerView Adapter.
         */
        private val DEVICES_COMPARATOR =
            object : DiffUtil.ItemCallback<TetherDevice> (){
                override fun areItemsTheSame(
                    oldItem: TetherDevice,
                    newItem: TetherDevice
                ): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(
                    oldItem: TetherDevice,
                    newItem: TetherDevice
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }
}