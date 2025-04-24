package com.dsh.tether.user.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.dsh.data.model.auth.UserAuth
import com.dsh.data.repository.UserAuthRepo
import com.dsh.data.repository.UserProfileRepo
import com.dsh.tether.model.AuthTaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userAuthRepo: UserAuthRepo,
    private val userProfileRepo: UserProfileRepo,
) : ViewModel() {

    /**
     * Authentication status live data
     */
    private val _authStatusLiveData = MutableLiveData<AuthTaskStatus>(AuthTaskStatus.NotStarted)
    val authStatusLiveData: LiveData<AuthTaskStatus>
        get() = _authStatusLiveData

    /**
     * The user log in status
     */
    private val _logInStatusLiveData = MutableLiveData<Boolean>()
    val logInStatusLiveData: LiveData<Boolean>
        get() = _logInStatusLiveData

    /**
     * Login
     *
     * @param context application context
     * @param account account info
     */
    fun login(@ApplicationContext context: Context, account: Auth0) {
        viewModelScope.launch(Dispatchers.IO) {
            _authStatusLiveData.postValue(AuthTaskStatus.InProgress)
            WebAuthProvider.login(account)
                .withScheme("demo")
                .withScope("openid profile email read:current_user")
                .withAudience("${account.getDomainUrl()}api/v2/")
                .start(context, object : Callback<Credentials, AuthenticationException> {
                    override fun onFailure(error: AuthenticationException) {
                        Timber.e("Failed to authenticate: [${error.getDescription()}]")
                        val msg = error.getDescription()
                        _authStatusLiveData.postValue(AuthTaskStatus.Failed(msg))
                    }

                    override fun onSuccess(result: Credentials) {
                        Timber.d("Credentials: [${result}]")
                        updateUserInfo(result)
                    }
                })
        }
    }

    /**
     * Updates user profile info
     *
     * @param credentials the auth credentials
     */
    private fun updateUserInfo(credentials: Credentials) {
        viewModelScope.launch {
            // update user profile
            try {
                userProfileRepo.setUserProfile(
                    id = credentials.user.getId()!!,
                    email = credentials.user.email!!,
                    name = credentials.user.name!!,
                    nickname = credentials.user.nickname!!,
                    pictureUrl = credentials.user.pictureURL!!
                )
            }catch (ex: Exception) {
                Timber.e("Failed to update user profile: ${ex.localizedMessage}")
            }

            // update user auth
            val userAuth = UserAuth.newBuilder()
                .setScope(credentials.scope)
                .setTokenType(credentials.type)
                .setAccessToken(credentials.accessToken)
                .setRefreshToken(credentials.refreshToken?:"")
                .build()
            try {
                userAuthRepo.setUserAuth(userAuth)
            }catch (ex: Exception) {
                Timber.e("Failed to update user auth: ${ex.localizedMessage}")
            }

            // update status
            _authStatusLiveData.postValue(AuthTaskStatus.Completed())
        }
    }

    /**
     * Checks if the current cached data is valid
     */
    fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                // fetch cached credentials
                val profile = userProfileRepo.getUserProfile()
                val auth = userAuthRepo.getUserAuth()

                val status = profile.id.isNotBlank() && auth.accessToken.isNotBlank()
                _logInStatusLiveData.postValue(status)
            }catch (ex: Exception) {
                Timber.e("Failed to retrieve credentials")
                _logInStatusLiveData.postValue(false)
            }
        }
    }
}