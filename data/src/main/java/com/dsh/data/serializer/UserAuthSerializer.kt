package com.dsh.data.serializer

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.dsh.data.model.auth.UserAuth
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object UserAuthSerializer : Serializer<UserAuth>{

    /**
     * Value to return if there is no data on disk.
     */
    override val defaultValue: UserAuth
        get() = UserAuth.getDefaultInstance()

    /**
     * Unmarshal object from stream.
     *
     * @param input the InputStream with the data to deserialize
     */
    override suspend fun readFrom(input: InputStream): UserAuth {
        try {
            return UserAuth.parseFrom(input)
        } catch (ex: InvalidProtocolBufferException) {
            val errorMsg = "Failed to read user auth proto"
            throw CorruptionException(errorMsg, ex)
        }
    }

    /**
     *  Marshal object to a stream. Closing the provided OutputStream is a no-op.
     *
     *  @param t the data to write to output
     *  @output the OutputStream to serialize data to
     */
    override suspend fun writeTo(t: UserAuth, output: OutputStream) {
        t.writeTo(output)
    }
}

val Context.userAuthDataStore : DataStore<UserAuth> by dataStore(
    fileName = "user_auth.proto", serializer = UserAuthSerializer
)