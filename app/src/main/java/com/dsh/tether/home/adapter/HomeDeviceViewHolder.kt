package com.dsh.tether.home.adapter

import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.dsh.matter.model.device.DeviceType
import com.dsh.matter.model.device.FanMode
import com.dsh.matter.model.device.StateAttribute
import com.dsh.tether.R
import com.dsh.tether.databinding.ItemHomeDeviceViewBinding
import com.dsh.tether.model.TetherDevice

class HomeDeviceViewHolder(
    private val binding: ItemHomeDeviceViewBinding,
    itemClickPosition: (Int) -> Unit,
    itemLongClickPosition: (Int) -> Unit
): RecyclerView.ViewHolder(binding.root) {

    init {
        itemView.setOnClickListener {
            itemClickPosition(adapterPosition)
        }

        itemView.setOnLongClickListener {
            itemLongClickPosition(adapterPosition)
            return@setOnLongClickListener true
        }
    }

    fun bind(thrDevice: TetherDevice) {
        binding.tvDeviceName.text = thrDevice.name

        val states = thrDevice.states
        val online = if(states.containsKey(StateAttribute.Online)) {
            states[StateAttribute.Online] as Boolean
        }else{false}

        val on = if(states.containsKey(StateAttribute.Switch)) {
            states[StateAttribute.Switch] as Boolean
        }else{false}

        val brightness = if(states.containsKey(StateAttribute.Brightness)) {
            states[StateAttribute.Brightness] as Int
        }else{0}

        val fanMode = if(states.containsKey(StateAttribute.FanMode)) {
            states[StateAttribute.FanMode] as FanMode
        }else{FanMode.Off}

        binding.tvDeviceMainStatus.text = when (online) {
            true -> {
                itemView.context
                    .getString(if(on) R.string.on_status else R.string.off_status)
            }
            false -> {
                itemView.context.getString(R.string.offline_status)
            }
        }

        binding.ivOfflineWarning.visibility = when(online) {
            true -> View.GONE
            false -> View.VISIBLE
        }

        when(thrDevice.type.toLong()) {
            DeviceType.DimmableLight.type,
            DeviceType.ExtendedColorLight.type,
            DeviceType.ColorTemperatureLight.type-> {
                // set device's brightness
                if(online && (brightness> 0)){
                    val value = "${brightness}%"
                    binding.tvDeviceAuxStatus.text = value
                }

                binding.tvDeviceAuxStatus.visibility = when((brightness > 0) && on && online){
                    true -> {
                        View.VISIBLE
                    }
                    else-> View.GONE
                }

                val drawable = when(on) {
                    true -> AppCompatResources.getDrawable(
                        itemView.context, R.drawable.ic_light_bulb_filled
                    )
                    false -> AppCompatResources.getDrawable(
                        itemView.context, R.drawable.ic_light_bulb_outline
                    )
                }
                binding.ivOnOffStatus.setImageDrawable(drawable)
            }
            DeviceType.Fan.type -> {
                when(on) {
                    true -> {
                        val drawable = AppCompatResources.getDrawable(
                            itemView.context, R.drawable.ic_fan_filled
                        )
                        binding.ivOnOffStatus.setImageDrawable(drawable)
                        if(fanMode != FanMode.Off) {
                            binding.tvDeviceAuxStatus.text = fanMode.name
                            binding.tvDeviceAuxStatus.visibility = View.VISIBLE
                        }else{
                            binding.tvDeviceAuxStatus.visibility = View.INVISIBLE
                        }
                    }
                    false -> {
                        val drawable = AppCompatResources.getDrawable(
                            itemView.context, R.drawable.ic_fan_outline
                        )
                        binding.ivOnOffStatus.setImageDrawable(drawable)
                        binding.tvDeviceAuxStatus.visibility = View.INVISIBLE
                    }
                }
                if(!online) {
                    binding.tvDeviceAuxStatus.visibility = View.INVISIBLE
                }
            }
            else -> {
                binding.tvDeviceAuxStatus.visibility = View.INVISIBLE
                val drawable = when(on) {
                    true -> AppCompatResources.getDrawable(
                        itemView.context, R.drawable.ic_outlet_filled
                    )
                    false -> AppCompatResources.getDrawable(
                        itemView.context, R.drawable.ic_outlet_outline
                    )
                }
                binding.ivOnOffStatus.setImageDrawable(drawable)
            }

        }
    }
}