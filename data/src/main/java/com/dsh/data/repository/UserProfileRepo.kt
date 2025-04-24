package com.dsh.data.repository

import android.content.Context
import com.dsh.data.model.auth.UserProfile
import com.dsh.data.serializer.userProfileDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepo @Inject constructor(@ApplicationContext context: Context) {

    /**
     * The user profile data store
     */
    private val userProfileDataStore = context.userProfileDataStore

    /**
     * The user profile flow
     */
    val userProfileFlow : Flow<UserProfile> =
        userProfileDataStore.data.catch { ex ->
            if(ex is IOException) {
                Timber.e(ex.localizedMessage)
                emit(UserProfile.getDefaultInstance())
            }else{
               throw  ex
            }
        }

    /**
     * The getter for the user profile info
     *
     * @return the user profile [UserProfile]
     */
    suspend fun getUserProfile(): UserProfile {
        return userProfileFlow.first()
    }

    /**
     * The setter for the user profile
     *
     * @param id user identifier
     * @param email user email
     * @param name user's name
     * @param nickname user's nickname
     * @param pictureUrl user's picture URL
     */
    suspend fun setUserProfile(
        id: String,
        email: String,
        name: String,
        nickname: String,
        pictureUrl: String
    ) {
        userProfileDataStore.updateData { profile ->
            profile.toBuilder()
                .setId(id)
                .setEmail(email)
                .setName(name)
                .setNickname(nickname)
                .setPictureUrl(pictureUrl)
                .build()
        }
    }

    /**
     * Hopefully, this clears whatever is stored in the user profile data store
     */
    suspend fun clear() {
        userProfileDataStore.updateData { userProfile ->
            userProfile.toBuilder().clear().build()
        }
    }
}