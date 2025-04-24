package com.dsh.tether.user.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.auth0.android.Auth0
import com.dsh.tether.R
import com.dsh.tether.databinding.FragmentAuthBinding
import com.dsh.tether.model.AuthTaskStatus
import com.dsh.tether.utils.LoadingDialog
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class AuthFragment : Fragment() {

    /**
     * The view model
     */
    private val viewModel: AuthViewModel by viewModels()

    /**
     * The UI binder
     */
    private lateinit var binding: FragmentAuthBinding

    /**
     * The loading binder
     */
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("[*** LifeCycle ***] : onCreate")

        // override on back pressed
        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireActivity().finish()
        }
        callback.isEnabled = true

        // check login credentials
        viewModel.checkLoginStatus()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("[*** LifeCycle ***] : onCreateView")
        // set binding
        binding = FragmentAuthBinding.inflate(inflater, container, false)

        // init loading dialog
        loadingDialog = LoadingDialog(requireContext())

        // Setup ui layer elements
        setupUiLayerElements()

        // Setup observers
        setupObservers()

        return binding.root
    }

    private fun setupUiLayerElements() {
        // listen to continue events
        binding.btnContinue.setOnClickListener {
            // start login
            val clientId = getString(R.string.com_auth0_client_id)
            val domain = getString(R.string.com_auth0_domain)
            val account = Auth0(clientId, domain)
            viewModel.login(requireContext(), account)
        }

        // listen to privacy policy
        binding.tvPrivacyPolicy.setOnClickListener {
            openLandingPage()
        }

        // listen to terms of service
        binding.tvTermsOfService.setOnClickListener {
            openLandingPage()
        }
    }

    private fun openLandingPage() {
        val webpage = Uri.parse("https://dsh-tether.web.app")
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        }
    }

    private fun setupObservers(){
        // Observe credentials
        viewModel.authStatusLiveData.observe(viewLifecycleOwner) { status ->
            Timber.d("Current auth status: [${status}]")
            if(null == status){
                return@observe
            }

            when(status) {
                is AuthTaskStatus.InProgress -> {
                    loadingDialog.show()
                    // hide btn and links
                    hideElements()
                }
                is AuthTaskStatus.Failed ->{
                    showElements()
                    loadingDialog.dismiss()
                }
                is AuthTaskStatus.Completed -> {
                    loadingDialog.dismiss()
                    val action =
                        AuthFragmentDirections.actionAuthToHomeFragment()
                    requireView().findNavController().navigate(action)
                }
            }
        }

        // Observe login status
        viewModel.logInStatusLiveData.observe(viewLifecycleOwner) { status ->
            Timber.d("Current login status: $status")
            if((null == status) || !status) {
                // show the button
                showElements()
                return@observe
            }
            val action =
                AuthFragmentDirections.actionAuthToHomeFragment()
            requireView().findNavController().navigate(action)
        }
    }

    /**
     * Hides the login btn and the links
     */
    private fun hideElements() {
        binding.btnContinue.visibility = View.INVISIBLE
        binding.llLinks.visibility = View.INVISIBLE
    }

    /**
     * Displays the login btn and the links
     */
    private fun showElements() {
        binding.btnContinue.visibility = View.VISIBLE
        binding.llLinks.visibility = View.VISIBLE
    }
}

