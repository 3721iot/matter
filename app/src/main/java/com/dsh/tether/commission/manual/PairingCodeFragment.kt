package com.dsh.tether.commission.manual

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.dsh.tether.R
import com.dsh.tether.databinding.FragmentPairingCodeBinding
import com.dsh.tether.model.ValidationTaskStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class PairingCodeFragment : Fragment() {

    /**
     * [PairingCodeFragment]'s view model
     */
    private val viewModel: PairingCodeViewModel by viewModels()

    /**
     * WiFi credentials UI binder
     */
    private lateinit var binding: FragmentPairingCodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("[*** LifeCycle ***] : onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("[*** LifeCycle ***] : onCreateView")
        binding = FragmentPairingCodeBinding.inflate(layoutInflater)

        // Setup UI layer elements
        setupUiLayerElements()

        // Setup observers
        setupObservers()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("[*** LifeCycle ***] : onViewCreated")
    }

    private fun setupUiLayerElements() {
        binding.btnCancel.setOnClickListener{
            it.findNavController().popBackStack()
        }

        binding.btnNext.setOnClickListener{
            var pairingCode = binding.etPairingCode.text.toString()
            viewModel.validateManualCode(pairingCode)
        }
    }

    private fun setupObservers() {
        viewModel.validationStatus.observe(viewLifecycleOwner){status ->
            if(null == status){
                return@observe
            }

            when(status){
                is ValidationTaskStatus.Failed -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.invalid_manual_code_title))
                        .setCancelable(false)
                        .setMessage(status.msg)
                        .setNegativeButton(getString(R.string.try_again)) { _, _ -> }
                        .setPositiveButton(getString(R.string.setup_with_qr_code)) { _, _ ->
                            requireView().findNavController().popBackStack()
                        }
                        .create()
                        .show()
                }
                is ValidationTaskStatus.Passed -> {
                    val pairingCode = status.data!! as String
                    val action =
                        PairingCodeFragmentDirections
                            .actionWifiCredentialsToDeviceProvisioningFragment(pairingCode)
                    requireView().findNavController().navigate(action)
                }
            }
        }
    }
}