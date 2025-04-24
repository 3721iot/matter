package com.dsh.tether.device.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.findNavController
import com.dsh.tether.R
import com.dsh.tether.databinding.DialogDeviceNameBinding
import com.dsh.tether.databinding.FragmentDeviceSettingsBinding
import com.dsh.tether.device.SelectedDeviceViewModel
import com.dsh.tether.model.GeneralTaskStatus
import com.dsh.tether.model.MtrDeviceMetadata
import com.dsh.tether.model.TetherDevice
import com.dsh.tether.utils.TimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class DeviceSettingsFragment : Fragment() {

    /**
     * UI layer binder
     */
    private lateinit var binding : FragmentDeviceSettingsBinding

    /**
     * [DeviceSettingsFragment]'s view model
     */
    private val viewModel: DeviceSettingsViewModel by viewModels()

    /**
     * [SelectedDeviceViewModel] holding the device being worked on at the moment
     */
    private val selectedDeviceViewModel: SelectedDeviceViewModel by activityViewModels()

    /**
     * Device name dialog
     */
    private lateinit var deviceNameDialog : AlertDialog
    private lateinit var deviceNameDialogBinding: DialogDeviceNameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("[*** LifeCycle ***] : onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("[*** LifeCycle ***] : onCreateView")

        //Initialize ui binder
        binding = FragmentDeviceSettingsBinding.inflate(layoutInflater)

        // Bind device device alert dialog
        deviceNameDialogBinding =
            DialogDeviceNameBinding.inflate(inflater, container, false)

        // Setup Ui Elements
        setupUiLayerElements()

        // Setup observers
        setupObservers()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                Timber.d("[*** LifeCycle ***] : onDestroy")
                deviceNameDialog.dismiss()
            }
        })
    }

    private fun setupUiLayerElements() {
        // Start device sharing
        binding.btnShare.setOnClickListener {
            val thrDevice = selectedDeviceViewModel.selectedDeviceLiveData.value!!
            Timber.d("Commence sharing of device: [${thrDevice}]")
            requireView().findNavController().navigate(R.id.action_device_settings_to_share_device_fragment)
        }

        // Remove device
        binding.btnDelete.setOnClickListener {
            val deviceId = selectedDeviceViewModel.selectedDeviceIdLiveData.value!!
            MaterialAlertDialogBuilder(it.context)
                .setTitle(getString(R.string.remove_device_title))
                .setMessage(getString(R.string.remove_device_hint))
                .setNegativeButton(getString(R.string.cancel)) { _,_ -> }
                .setPositiveButton(getString(R.string.yes_delete)) { _, _ ->
                    viewModel.deleteDevice(deviceId)
                    selectedDeviceViewModel.reset()
                }.show()
        }

        deviceNameDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(deviceNameDialogBinding.root)
            .setCancelable(false)
            .setTitle(getString(R.string.rename_device_title))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val thrDevice = selectedDeviceViewModel.selectedDeviceLiveData.value!!
                val deviceName = deviceNameDialogBinding.etDeviceName.text.toString()
                viewModel.updateDeviceName(thrDevice.id, deviceName)
                selectedDeviceViewModel.updateDeviceName(deviceName)
                updateDeviceInfo(deviceName)
            }.setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .create()

        binding.llDeviceName.setOnClickListener {
            deviceNameDialog.show()
        }
    }

    private fun setupObservers() {
        // Observe the currently selected device
        selectedDeviceViewModel.selectedDeviceIdLiveData.observe(viewLifecycleOwner) {
            updateDeviceInfo(null)
        }

        // Observe the currently selected device
        selectedDeviceViewModel.selectedDeviceLiveData.observe(viewLifecycleOwner) { thrDevice ->
            updateDeviceInfo(thrDevice)
        }

        viewModel.deleteDeviceTaskLiveData.observe(viewLifecycleOwner) { status ->
            when(status) {
                is GeneralTaskStatus.Failed -> {
                    Timber.e("Failed to delete device")
                }
                is GeneralTaskStatus.Completed -> {
                    selectedDeviceViewModel.reset()
                    requireView().findNavController().popBackStack(
                        R.id.home_fragment, false
                    )
                }
            }
        }
    }

    /**
     * Updates the device info
     *
     * @param thrDevice device ui data
     */
    private fun updateDeviceInfo(thrDevice: TetherDevice?) {
        if(thrDevice == null){
            return
        }

        val metadata = thrDevice.metadata as MtrDeviceMetadata
        val deviceName =  thrDevice.name
        val deviceId =  thrDevice.id
        val vendorId = metadata.vendorId

        // ToDo() get date added
        val dateAdded = TimeUtils.getTimestamp()
        val deviceType = thrDevice.type

        // Update device name
        updateDeviceInfo(deviceName)

        if(deviceId != -1L){
            binding.tvDeviceId.text = deviceId.toString()
        }

        binding.tvDateAdded.text = TimeUtils.formatTimestamp(dateAdded)
        binding.tvDeviceType.text = deviceType
        binding.tvDeviceManufacturer.text = vendorId.toString()
    }

    /**
     * Updates device name
     *
     * @param deviceName device name
     */
    private fun updateDeviceInfo(deviceName: String) {
        binding.etDeviceName.text = deviceName
        binding.tvDeviceName.text = deviceName
    }
}