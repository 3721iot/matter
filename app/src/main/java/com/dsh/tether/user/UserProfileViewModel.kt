package com.dsh.tether.user

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.*
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
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
class UserProfileViewModel @Inject constructor(
    private val userAuthRepo: UserAuthRepo,
    private val userProfileRepo: UserProfileRepo
) : ViewModel() {

    /**
     * Authentication status live data
     */
    private val _logoutStatusLiveData = MutableLiveData<AuthTaskStatus>(AuthTaskStatus.NotStarted)
    val logoutStatusLiveData: LiveData<AuthTaskStatus>
        get() = _logoutStatusLiveData

    /**
     * App version live data
     */
    private val _appVersionLiveData = MutableLiveData<String?>()
    val appVersionLiveData: LiveData<String?>
        get() = _appVersionLiveData

    /**
     * Initial setup event which triggers the [UserProfileFragment] to get the data required for its UI
     */
    init {
        liveData { emit(userProfileRepo.getUserProfile()) }
    }

    /**
     * The user profile live data
     */
    private val userProfileFlow = userProfileRepo.userProfileFlow
    val userProfileLiveData = userProfileFlow.asLiveData()

    /**
     * Logout
     *
     * @param context application context
     * @param account account info
     */
    fun logout(@ApplicationContext context: Context, account: Auth0) {
        _logoutStatusLiveData.postValue(AuthTaskStatus.InProgress)
        viewModelScope.launch(Dispatchers.IO){
            WebAuthProvider.logout(account)
                .withScheme("demo")
                .start(context, object: Callback<Void?, AuthenticationException> {
                    override fun onFailure(error: AuthenticationException) {
                        val msg = error.getDescription()
                        _logoutStatusLiveData.postValue(AuthTaskStatus.Failed(msg))
                    }

                    override fun onSuccess(result: Void?) {
                        clearCachedCredentials()
                    }
                })
        }
    }

    /**
     * Clears all the user credentials
     */
    private fun clearCachedCredentials(){
        viewModelScope.launch {
            try {
                userAuthRepo.clear()
                userProfileRepo.clear()
            }catch (ex: Exception) {
                Timber.d("Failed to clear cached data.")
            }
            _logoutStatusLiveData.postValue(AuthTaskStatus.Completed())
        }
    }

    /**
     * Getter for the app's live data
     *
     * @param context application context
     */
    fun getAppVersion(@ApplicationContext context: Context) {
        val manager: PackageManager = context.packageManager
        try {
            val info: PackageInfo = manager.getPackageInfo(
                context.packageName, 0
            )
            val version = "v${info.versionName}"
            _appVersionLiveData.postValue(version)
        }catch (ex: Exception) {
            Timber.e(ex.localizedMessage)
        }
    }
}