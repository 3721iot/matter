package com.dsh.data.serializer

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.dsh.data.model.auth.UserProfile
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object UserProfileSerializer : Serializer<UserProfile> {
    /**
     * Value to return if there is no data on disk.
     */
    override val defaultValue: UserProfile
        get() = UserProfile.getDefaultInstance()

    /**
     * Unmarshal object from stream.
     *
     * @param input the InputStream with the data to deserialize
     */
    override suspend fun readFrom(input: InputStream): UserProfile {
        try {
            return UserProfile.parseFrom(input)
        } catch (ex: InvalidProtocolBufferException) {
            val errorMsg = "Failed to read user profile proto"
            throw CorruptionException(errorMsg, ex)
        }
    }

    /**
     *  Marshal object to a stream. Closing the provided OutputStream is a no-op.
     *
     *  @param t the data to write to output
     *  @output the OutputStream to serialize data to
     */
    override suspend fun writeTo(t: UserProfile, output: OutputStream) {
        t.writeTo(output)
    }
}

val Context.userProfileDataStore : DataStore<UserProfile> by dataStore(
    fileName = "user_profile.proto", serializer = UserProfileSerializer
)