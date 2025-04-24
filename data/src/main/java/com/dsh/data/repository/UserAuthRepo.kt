package com.dsh.data.repository

import android.content.Context
import com.dsh.data.model.auth.UserAuth
import com.dsh.data.serializer.userAuthDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAuthRepo @Inject constructor(@ApplicationContext context: Context) {

    /**
     * The user auth data store
     */
    private val userAuthDataStore = context.userAuthDataStore

    /**
     * The user auth flow
     */
    private val userAuthFlow : Flow<UserAuth> =
        userAuthDataStore.data.catch { ex ->
            if(ex is IOException) {
                val errorMsg = "Failed to interact with user auth store"
                Timber.e(ex, errorMsg)
                emit(UserAuth.getDefaultInstance())
            }else {
                throw ex
            }
        }

    /**
     * The getter for the user auth info
     *
     * @return the user auth info [UserAuth]
     */
    suspend fun getUserAuth(): UserAuth {
        return userAuthFlow.first()
    }

    /**
     * The setter for the user's auth info
     *
     * @param userAuth [UserAuth]
     */
    suspend fun setUserAuth(userAuth: UserAuth) {
        userAuthDataStore.updateData { auth ->
            auth.toBuilder()
                .setAccessToken(userAuth.accessToken)
                .setRefreshToken(userAuth.refreshToken)
                .setExpiresAt(userAuth.expiresAt)
                .setIdToken(userAuth.idToken)
                .setTokenType(userAuth.tokenType)
                .setScope(userAuth.scope)
                .build()
        }
    }

    /**
     * Hopefully, this clears whatever is stored in the user auth data store
     */
    suspend fun clear() {
        userAuthDataStore.updateData { userAuth ->
            userAuth.toBuilder().clear().build()
        }
    }
}