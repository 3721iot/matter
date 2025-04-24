package com.dsh.data.serializer

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.dsh.data.model.device.DeviceStates
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object DevicesStateSerializer : Serializer<DeviceStates> {

    override val defaultValue: DeviceStates
        get() = DeviceStates.getDefaultInstance()

    /**
     * Unmarshal object from stream.
     *
     * @param input the InputStream with the data to deserialize
     */
    override suspend fun readFrom(input: InputStream): DeviceStates {
        try {
           return DeviceStates.parseFrom(input)
        }catch (ex : InvalidProtocolBufferException ) {
            val errorMsg = "Failed read devices state from proto."
            throw CorruptionException(errorMsg, ex)
        }
    }

    /**
     *  Marshal object to a stream. Closing the provided OutputStream is a no-op.
     *
     *  @param t the data to write to output
     *  @output the OutputStream to serialize data to
     */
    override suspend fun writeTo(t: DeviceStates, output: OutputStream) {
        t.writeTo(output)
    }
}

val Context.deviceSatesDataStore: DataStore<DeviceStates> by
    dataStore(fileName = "device_states_store.proto", serializer = DevicesStateSerializer)