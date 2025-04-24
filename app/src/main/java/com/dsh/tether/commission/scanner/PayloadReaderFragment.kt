package com.dsh.tether.commission.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.dsh.tether.R
import com.dsh.tether.databinding.FragmentPayloadReaderBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import timber.log.Timber

@AndroidEntryPoint
class PayloadReaderFragment : Fragment() {

    /**
     * [PayloadReaderFragment] binding
     */
    private lateinit var binding: FragmentPayloadReaderBinding

    /**
     * [PayloadReaderViewModel] view model
     */
    private val viewModel : PayloadReaderViewModel by viewModels()

    /**
     * Camera permission request launcher declaration
     */
    private lateinit var cameraPermissionRequestLauncher : ActivityResultLauncher<String>

    /**
     * Camera provider
     */
    private lateinit var cameraProvider: ProcessCameraProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("[*** LifeCycle ***] : onCreate")
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        cameraPermissionRequestLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your app.
                    Timber.d("The camera permission is granted")
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    Timber.d("The camera permission is not granted")
                    binding.root.showSnackbar(
                        requireView(),
                        getString(R.string.camera_permission_additional_rationale),
                        Snackbar.LENGTH_INDEFINITE,
                        getString(R.string.done)
                    ) {

                    }
                }
            }
    }

    /**
     * Check camera permissions
     */
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is granted, start camera preview view
                startCamera()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.CAMERA
            ) -> {
                // Additional rationale should be displayed
                binding.root.showSnackbar(
                    requireView(),
                    getString(R.string.camera_permission_rationale),
                    Snackbar.LENGTH_INDEFINITE,
                    getString(R.string.continue_auth)
                ) {
                    cameraPermissionRequestLauncher.launch(Manifest.permission.CAMERA)
                }
            }
            else ->{
                // Permission has not been asked yet
                cameraPermissionRequestLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("[*** LifeCycle ***] : onCreateView")
        binding = FragmentPayloadReaderBinding.inflate(layoutInflater)

        // Setup Ui Elements
        setupUiLayerElements()

        // Setup observers
        setupObservers()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("[*** LifeCycle ***] : onViewCreated")
        checkCameraPermission()
    }

    private fun setupUiLayerElements() {
        // Navigate to manual code input
        binding.btnManualCode.setOnClickListener { view ->
            val action = PayloadReaderFragmentDirections.actionPayloadReaderToPairingCodeFragment()
            view.findNavController().navigate(action)
        }
    }

    private fun setupObservers() {
        // Observe barcode live data
        viewModel.setupPayload.observe(viewLifecycleOwner){ payload ->
            // Unbind use cases before processing the payload
            cameraProvider.unbindAll()
            val action =
                PayloadReaderFragmentDirections
                    .actionPayloadReaderToDeviceProvisioningFragment(payload)
            requireView().findNavController().navigate(action)
        }

        /**
         * Observe setup payload errors
         */
        viewModel.scanQrCodeError.observe(viewLifecycleOwner) { error ->
            // Unbind use cases before processing the error
            cameraProvider.unbindAll()
            Timber.d("Failed to scan QR code. [${error}]")
            // ToDo() suggest using another method
        }
    }

    private fun showUnknownErrorDialog(){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.qr_code_scan_failed_title))
            .setMessage(getString(R.string.something_went_wrong))
            .setPositiveButton(getString(R.string.exit_setup)) { _,_ ->
                requireView().findNavController().popBackStack()
            }
            .setNegativeButton(getString(R.string.try_again)) {_, _ ->
                Timber.d("Should reload the camera")
            }.show()
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())
        cameraProviderFuture.addListener({
            // Compute screen aspect ratio
            cameraProvider = cameraProviderFuture.get()
            val metrics = DisplayMetrics().also { binding.pvCamera.display?.getRealMetrics(it) }

            val screenAspectRatio =
                viewModel.getAspectRatio(metrics.widthPixels, metrics.heightPixels)

            // Camera preview
            val preview: Preview = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(binding.pvCamera.display.rotation)
                .build()

            // Set Surface provider
            preview.setSurfaceProvider(binding.pvCamera.surfaceProvider)

            // Build Image Analysis
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(binding.pvCamera.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setBackgroundExecutor(Dispatchers.Default.asExecutor())
                .build()

            // Process image
            imageAnalysis.setAnalyzer(Dispatchers.Main.immediate.asExecutor()) { imageProxy ->
                viewModel.processImageProxy(imageProxy)
            }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider
                    .bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                Timber.e("Use case binding failed")
                showUnknownErrorDialog()
            }
        }, ContextCompat.getMainExecutor(requireActivity()))
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