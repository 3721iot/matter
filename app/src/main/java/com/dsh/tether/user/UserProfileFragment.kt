package com.dsh.tether.user

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.auth0.android.Auth0
import com.bumptech.glide.Glide
import com.dsh.data.model.auth.UserProfile
import com.dsh.tether.R
import com.dsh.tether.databinding.FragmentUserProfileBinding
import com.dsh.tether.model.AuthTaskStatus
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class UserProfileFragment : Fragment() {

    /**
     * The user profile view model
     */
    private val viewModel: UserProfileViewModel by viewModels()

    /**
     * The user profile UI binder
     */
    private lateinit var binding: FragmentUserProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("[*** LifeCycle ***] : onCreateView")
        binding = FragmentUserProfileBinding.inflate(inflater, container, false)

        // setup UI layer elements
        setupUiLayerElements()

        // setup observers
        setupObservers()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("[*** LifeCycle ***] : onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        viewModel.getAppVersion(requireContext())
    }

    private fun setupUiLayerElements() {
        // listen to logout events
        binding.btnLogout.setOnClickListener {
            Timber.d("Do some logging out")
            // start login
            val clientId = getString(R.string.com_auth0_client_id)
            val domain = getString(R.string.com_auth0_domain)
            val account = Auth0(clientId, domain)
            viewModel.logout(requireContext(), account)
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
        val webpage: Uri = Uri.parse("https://dsh-tether.web.app")
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        }
    }

    private fun setupObservers() {
        // Observe logout status
        viewModel.logoutStatusLiveData.observe(viewLifecycleOwner) { status ->
            if(null == status){
                return@observe
            }

            when(status) {
                is AuthTaskStatus.Failed -> {
                    Timber.e("Failed to logout")
                    val action = UserProfileFragmentDirections.actionUserProfileToHomeFragment()
                    requireView().findNavController().navigate(action)
                }
                is AuthTaskStatus.Completed -> {
                    Timber.d("Logout successfully")
                    val action = UserProfileFragmentDirections.actionUserProfileToAuthFragment()
                    requireView().findNavController().navigate(action)
                }
            }
        }

        // Observe user profile info
        viewModel.userProfileLiveData.observe(viewLifecycleOwner) { userProfile ->
            if(null == userProfile){
                return@observe
            }
            updateUserInfo(userProfile)
        }

        // Observe app version
        viewModel.appVersionLiveData.observe(viewLifecycleOwner) { version ->
            binding.tvAppVersion.text = version
        }
    }

    private fun updateUserInfo(userProfile: UserProfile) {
        // set name and address
        binding.tvName.text = userProfile.name
        binding.tvEmail.text = userProfile.email

        // set avatar
        Glide.with(this)
            .load(userProfile.pictureUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_dot_filled)
            .into(binding.ivAvatar)
    }
}