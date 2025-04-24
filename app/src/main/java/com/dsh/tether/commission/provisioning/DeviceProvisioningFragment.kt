package com.dsh.tether.commission.provisioning

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.dsh.matter.model.CommissioningErrorCode.*
import com.dsh.tether.R
import com.dsh.tether.databinding.DialogWifiCredentialsBinding
import com.dsh.tether.databinding.FragmentDeviceProvisioningBinding
import com.dsh.tether.model.CommissioningTaskStatus
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class DeviceProvisioningFragment : Fragment() {

    /**
     * [DeviceProvisioningFragment] Ui binder
     */
    private lateinit var binding: FragmentDeviceProvisioningBinding

    /**
     * WiFi credentials dialog
     */
    private lateinit var wifiCredentialsDialog: DialogWifiCredentialsBinding

    /**
     * [DeviceProvisioningFragment]'s view model
     */
    private val viewModel: DeviceProvisioningViewModel by viewModels()

    /**
     *
     */
    private val arguments : DeviceProvisioningFragmentArgs by navArgs()

    /**
     * Bluetooth permission request launcher declaration
     */
    private lateinit var permissionRequestLauncher : ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("[*** LifeCycle ***] : onCreate")
        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            showExitSetupDialog()
        }
        callback.isEnabled = true

        // init permission request launcher
        permissionRequestLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            Timber.d("Granted permissions: [${permissions}]")
            val hasMissingPermissions = permissions.values.stream().anyMatch {it != true}
            if(hasMissingPermissions) {
                binding.root.showSnackbar(
                    requireView(),
                    getString(R.string.pairing_permission_additional_rationale),
                    Snackbar.LENGTH_INDEFINITE,
                    getString(R.string.done)
                ) {
                    // ToDo() close shop and go home or post some value
                }
            }else{
                Timber.d("We have all the permissions")
                // Permission is granted, start device commissioning
                val setupPayload = arguments.setupPayload
                viewModel.commissionDevice(setupPayload)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("[*** LifeCycle ***] : onCreateView")
        binding = FragmentDeviceProvisioningBinding.inflate(layoutInflater)

        // Bind wifi credentials alert dialog
        wifiCredentialsDialog =
            DialogWifiCredentialsBinding.inflate(inflater, container, false)

        setUiLayerElements()

        setObservers()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // check the permissions
        checkPermission()
    }

    @SuppressLint("InlinedApi")
    private fun checkPermission() {
        when {
            checkSelfPermissions() -> {
                // Permission is granted, start device commissioning
                val setupPayload = arguments.setupPayload
                viewModel.commissionDevice(setupPayload)
            }
            shouldShowRequestPermissionRationale() -> {
                // Additional rationale should be displayed
                binding.root.showSnackbar(
                    requireView(),
                    getString(R.string.pairing_permission_rationale),
                    Snackbar.LENGTH_INDEFINITE,
                    getString(R.string.continue_auth)
                ) {
                    val permissions = getPermissionsList()
                    permissionRequestLauncher.launch(permissions)
                }
            }
            else ->{
                // Permission has not been asked yet
                val permissions = getPermissionsList()
                permissionRequestLauncher.launch(permissions)
            }
        }
    }

    private fun getPermissionsList(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("InlinedApi")
    private fun checkSelfPermissions(): Boolean {
        // check required permissions
        val permissions = getPermissionsList()
        val hasPermissions = permissions.map { permission ->
            ContextCompat.checkSelfPermission(
                requireContext(), permission
            ) == PackageManager.PERMISSION_GRANTED
        }.toMutableList().stream().allMatch { it == true }

        Timber.d("Has all permissions: $hasPermissions")
        return hasPermissions
    }

    private fun shouldShowRequestPermissionRationale(): Boolean {
        // check rationale status
        val permissions = getPermissionsList()
        Timber.d("List of permissions: [${permissions}]")
        val showRationale = permissions.map { permission ->
            Timber.d("Permission: $permission")
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                permission
            )
        }.toMutableList().stream().anyMatch { it == true}

        Timber.d("Should show rationale: $showRationale")
        return showRationale
    }

    private fun setUiLayerElements() {
        binding.btnNegative.setOnClickListener{
            requireView()
                .findNavController().popBackStack(R.id.home_fragment, false)
        }

        binding.btnPositive.setOnClickListener { view ->
            when(view.tag){
                getString(R.string.done) ->{
                    requireView()
                        .findNavController().popBackStack(R.id.home_fragment, false)
                }
                getString(R.string.try_again) -> {
                    // Try going to the camera view to start the process again
                    // ToDo() fix payload reader fragment bug first
                    requireView()
                        .findNavController().popBackStack(R.id.home_fragment, false)
                }
                else -> {
                    Timber.d("Failed with an unknown error")
                    requireView()
                        .findNavController().popBackStack(R.id.home_fragment, false)
                }
            }
        }
    }

    private fun setObservers() {
        // Observe the current status of a shared device action
        viewModel.commissionDeviceStatus.observe(viewLifecycleOwner) { taskStatus ->
            binding.rlButtons.visibility =
                if ((taskStatus is CommissioningTaskStatus.Failed) || (taskStatus is CommissioningTaskStatus.Completed))
                    View.VISIBLE else View.GONE

            when (taskStatus) {
                is CommissioningTaskStatus.Failed -> {
                    binding.btnNegative.tag = getString(R.string.exit_setup)
                    binding.btnPositive.tag = getString(R.string.try_again)
                    binding.piLoading.visibility = View.INVISIBLE
                    updateUiOnCommissionFailure(taskStatus)
                }
                is CommissioningTaskStatus.Warning -> {
                    binding.piLoading.visibility = View.INVISIBLE
                    showAttestationFailureDialog(taskStatus)
                }
                is CommissioningTaskStatus.Completed -> {
                    binding.piLoading.visibility = View.INVISIBLE
                    binding.btnNegative.visibility = View.INVISIBLE
                    binding.btnPositive.text = getString(R.string.done)
                    binding.btnPositive.tag = getString(R.string.done)
                    binding.ivProvisionIcon.setImageDrawable(
                        AppCompatResources.getDrawable(requireContext(), R.drawable.ic_done_round)
                    )
                    binding.tvTitle.text = getString(R.string.matter_device_connected)
                    binding.tvDescription.visibility = View.INVISIBLE
                }
                else -> {
                    binding.tvDescription.visibility = View.INVISIBLE
                    binding.piLoading.visibility = View.VISIBLE
                }
            }
        }

        // Observe WiFi credentials request
        viewModel.requestWifiCredentials.observe(viewLifecycleOwner) {devicePtr->
            if(devicePtr <= 0L){
                return@observe
            }

            showWiFiCredentialsInputDialog(devicePtr)
        }
    }

    private fun updateUiOnCommissionFailure(taskStatus : CommissioningTaskStatus.Failed) {
        // Set warning icon
        binding.ivProvisionIcon.setImageDrawable(
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_warning_round)
        )
        binding.tvDescription.text = taskStatus.msg
        binding.tvDescription.visibility = View.VISIBLE
        when(taskStatus.error){
            AttestationFailed -> {
                binding.tvTitle.text = getString(R.string.device_failed_attestation)
            }
            DeviceNotFound -> {
                binding.tvTitle.text = getString(R.string.device_not_found)
            }
            AddDeviceFailed ->{
                binding.tvTitle.text = getString(R.string.device_add_failed)
            }
            else ->{
                binding.tvTitle.text = getString(R.string.something_went_wrong)
                binding.tvDescription.visibility = View.GONE
            }
        }
    }

    /**
     * Show attestation failure dialog
     *
     * @param taskStatus attestation warning
     */
    private fun showAttestationFailureDialog(taskStatus: CommissioningTaskStatus.Warning) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.not_a_matter_certified_device))
            .setMessage(getString(R.string.not_a_matter_certified_device_hint))
            .setPositiveButton(getString(R.string.add_anyway)) { _, _ ->
                binding.piLoading.visibility = View.VISIBLE
                viewModel.continueCommissioning(taskStatus.data as Long, true)
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                viewModel.continueCommissioning(taskStatus.data as Long, false)
            }
            .create()
            .show()
    }

    /**
     * Shows the cancel setup alert dialog
     */
    private fun showExitSetupDialog(){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.exit_setup_title))
            .setMessage(getString(R.string.exit_setup_hint))
            .setPositiveButton(getString(R.string.exit)) { _, _ ->
                requireView()
                    .findNavController().popBackStack(R.id.home_fragment, false)
            }
            .setNegativeButton(getString(R.string.continue_setup)) { _, _ ->}
            .show()
    }


    /**
     * Shows the new device alert dialog.
     */
    private fun showWiFiCredentialsInputDialog(devicePtr: Long) {
        val tint =
            MaterialColors.getColor(
                requireView(),
                com.google.android.material.R.attr.colorPrimary
            )
        val icon = AppCompatResources.getDrawable(
            requireContext(), R.drawable.ic_wifi_password_round
        )
        icon?.setTint(tint)
        MaterialAlertDialogBuilder(requireContext())
            .setView(wifiCredentialsDialog.root)
            .setIcon(icon)
            .setCancelable(false)
            .setTitle(getString(R.string.wifi_credentials_title))
            .setMessage(getString(R.string.wifi_network_band_hint))
            .setPositiveButton(getString(R.string.wifi_credentials_continue)) { _, _ ->
                // Extract entered device name
                val ssid = wifiCredentialsDialog.etWifiSsid.text.toString()
                val password = wifiCredentialsDialog.etWifiPassword.text.toString()
                viewModel.continueCommissioning(
                    devicePtr = devicePtr, ssid = ssid, password = password
                )
            }.setNegativeButton(getString(R.string.exit_setup)) { _, _ ->
                requireView()
                    .findNavController().popBackStack(R.id.home_fragment, false)
            }
            .create()
            .show()
    }

    private fun View.showSnackbar(
        view: View,
        msg: String,
        length: Int,
        actionMessage: CharSequence?,
        action: (View) -> Unit
    ) {
        val snackbar = Snackbar.make(view, msg, length)
        if (actionMessage != null) {
            snackbar.setAction(actionMessage) {
                action(this)
            }.show()
        } else {
            snackbar.show()
        }
    }
}