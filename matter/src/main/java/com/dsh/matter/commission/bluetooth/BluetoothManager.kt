package com.dsh.matter.commission.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import androidx.core.content.ContextCompat.getSystemService
import chip.platform.BleCallback
import com.dsh.matter.MtrClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume

@SuppressLint("MissingPermission")
internal class BluetoothManager @Inject constructor(@ApplicationContext context: Context) : BleCallback {

    /**
     * Bluetooth Adapter
     */
    private val bluetoothAdapter: BluetoothAdapter

    /**
     * Bluetooth GATT
     */
    private lateinit var bluetoothGatt: BluetoothGatt

    /**
     * Connect identifier
     */
    var connectedId = 0
        private set

    init {
        val bluetoothManager = getSystemService(
            context, android.bluetooth.BluetoothManager::class.java
        )!!
        bluetoothAdapter = bluetoothManager.adapter
    }

    override fun onCloseBleComplete(connId: Int) {
        Timber.d("Close [BLE] completed")
        connectedId = 0
    }

    @SuppressLint("MissingPermission")
    override fun onNotifyChipConnectionClosed(connId: Int) {
        Timber.d("[CHIP] connection closed")
        bluetoothGatt.close()
        connectedId = 0
    }

    /**
     * Builds the bluetooth service data
     *
     * @param discriminator bluetooth discriminator
     */
    private fun getServiceData(discriminator: Int): ByteArray {
        val opCode = 0
        val version = 0
        val versionDiscriminator =  ((version and 0xf) shl 12) or (discriminator and 0xfff)
        return intArrayOf(opCode, versionDiscriminator, versionDiscriminator shr 8)
            .map { it.toByte() }
            .toByteArray()
    }

    /**
     * Infers the service data mask
     *
     * @param isShortDiscriminator whether the discriminator is short or not
     */
    private fun getServiceDataMask(isShortDiscriminator: Boolean): ByteArray {
        val shortDiscriminatorMask = when(isShortDiscriminator) {
            true -> 0x00
            false -> 0xff
        }
        return intArrayOf(0xff, shortDiscriminatorMask, 0xff).map { it.toByte() }.toByteArray()
    }

    /**
     * Returns the discovered bluetooth device
     *
     * @param discriminator device discriminator
     * @param isShortDiscriminator whether is short discriminator or not
     */
    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getBleDevice(discriminator: Int, isShortDiscriminator: Boolean): BluetoothDevice? {
        if(!bluetoothAdapter.isEnabled){
            bluetoothAdapter.enable()
        }

        val bleScanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            Timber.e("No BLE scanner found")
            return null
        }

        return withTimeoutOrNull(BLUETOOTH_SCAN_TIMEOUT_MS) {
            callbackFlow {
                val bleScanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        val bleDevice = result.device
                        Timber.d("Scanned device. address=[${bleDevice.address}]")
                        val producerScope: ProducerScope<BluetoothDevice> = this@callbackFlow
                        if (producerScope.channel.isClosedForSend) {
                            Timber.w("BLE device scanned, but channel is already closed")
                            return
                        } else {
                            this@callbackFlow.trySend(bleDevice).isSuccess
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        Timber.e("BLE scan failed with code $errorCode")
                    }
                }

                val serviceData = getServiceData(discriminator)
                val serviceDataMask = getServiceDataMask(isShortDiscriminator)

                val bleScanFilter =
                    ScanFilter.Builder().setServiceData(
                        ParcelUuid(UUID.fromString(MTR_UUID)),
                        serviceData,
                        serviceDataMask
                    ).build()

                val bleScanSettings =
                    ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build()

                Timber.d("Starting BLE scan")
                bleScanner.startScan(listOf(bleScanFilter), bleScanSettings, bleScanCallback)
                awaitClose {
                    bleScanner.stopScan(bleScanCallback)
                }
            }.first()
        }
    }

    /**
     * Connects to device
     *
     * @param context android context
     * @param device bluetooth device
     */
    suspend fun connectDevice(context: Context, device: BluetoothDevice): BluetoothGatt? {
        return suspendCancellableCoroutine { continuation ->
            val bleGattCallback = getBluetoothGattCallback(context, continuation)
            Timber.d("Connecting device ...")
            bluetoothGatt = device.connectGatt(context, false, bleGattCallback)

            // Updated connected id
            connectedId = MtrClient.getAndroidPlatform(context).bleManager.addConnection(bluetoothGatt)

            // Set ChipAndroidPlatform callback
            MtrClient.getAndroidPlatform(context).bleManager.setBleCallback(this)

            continuation.invokeOnCancellation { bluetoothGatt.disconnect() }
        }
    }

    /**
     * Builds the device bluetooth gatt callback
     *
     * @param context android context
     * @param continuation [CancellableContinuation]
     */
    private fun getBluetoothGattCallback(
        context: Context,
        continuation: CancellableContinuation<BluetoothGatt?>
    ): BluetoothGattCallback{

         val bluetoothGattCallback = object : BluetoothGattCallback() {
             private val wrappedCallback = MtrClient.getAndroidPlatform(context).bleManager.callback
             private val coroutineContinuation = continuation

             override fun onConnectionStateChange(
                 gatt: BluetoothGatt?,
                 status: Int,
                 newState: Int
             ) {
                 super.onConnectionStateChange(gatt, status, newState)
                 val logMsg =
                     "conn:device=[${gatt?.device?.address}], status=${status}, newState=${newState}"
                 Timber.d(logMsg)

                 wrappedCallback.onConnectionStateChange(gatt, status, newState)
                 val isConnected = newState == BluetoothProfile.STATE_CONNECTED
                 val isGattSuccess = status == BluetoothGatt.GATT_SUCCESS
                 if(isConnected && isGattSuccess) {
                     Timber.d("Discovering [GATT] services...")
                     gatt?.discoverServices()
                 }
             }

             override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                 super.onServicesDiscovered(gatt, status)
                 Timber.d("disc:device=[${gatt?.device?.address}], status=${status}")
                 wrappedCallback.onServicesDiscovered(gatt, status)

                 Timber.d("Services discovered")
                 gatt?.requestMtu(247)
             }

             @Deprecated("Deprecated in Java")
             override fun onCharacteristicRead(
                 gatt: BluetoothGatt?,
                 characteristic: BluetoothGattCharacteristic?,
                 status: Int
             ) {
                 val logMsg =
                     "device=${gatt?.device?.address} uuid=${characteristic?.uuid}| r-status=${status}"
                 Timber.d(logMsg)
                 wrappedCallback.onCharacteristicRead(gatt, characteristic, status)
             }

             override fun onCharacteristicWrite(
                 gatt: BluetoothGatt?,
                 characteristic: BluetoothGattCharacteristic?,
                 status: Int
             ) {
                 val logMsg =
                     "device=${gatt?.device?.address} uuid=${characteristic?.uuid}| w-status=${status}"
                 Timber.d(logMsg)
                 wrappedCallback.onCharacteristicWrite(gatt, characteristic, status)
             }

             @Deprecated("Deprecated in Java")
             override fun onCharacteristicChanged(
                 gatt: BluetoothGatt?,
                 characteristic: BluetoothGattCharacteristic?,
             ) {
                 Timber.d("device=${gatt?.device?.address} uuid=${characteristic?.uuid}")
                 wrappedCallback.onCharacteristicChanged(gatt, characteristic)
             }

             @Deprecated("Deprecated in Java")
             override fun onDescriptorRead(
                 gatt: BluetoothGatt?,
                 descriptor: BluetoothGattDescriptor?,
                 status: Int
             ) {
                 super.onDescriptorRead(gatt, descriptor, status)
                 val logMsg =
                     "device=${gatt?.device?.address} uuid=${descriptor?.uuid}| r-status=${status}"
                 Timber.d(logMsg)
                 wrappedCallback.onDescriptorRead(gatt, descriptor, status)
             }

             override fun onDescriptorWrite(
                 gatt: BluetoothGatt?,
                 descriptor: BluetoothGattDescriptor?,
                 status: Int
             ) {
                 super.onDescriptorWrite(gatt, descriptor, status)
                 val logMsg =
                     "device=${gatt?.device?.address} uuid=${descriptor?.uuid}| w-status=${status}"
                 Timber.d(logMsg)
                 wrappedCallback.onDescriptorWrite(gatt, descriptor, status)
             }

             override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
                 super.onReliableWriteCompleted(gatt, status)
                 Timber.d("device=${gatt?.device?.address} wc-status=${status}")
                 wrappedCallback.onReliableWriteCompleted(gatt, status)
             }

             override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
                 super.onReadRemoteRssi(gatt, rssi, status)
                 Timber.d("device=${gatt?.device?.address} rssi=${rssi} status=${status}")
             }

             override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                 super.onMtuChanged(gatt, mtu, status)
                 Timber.d("device=[${gatt?.device?.address}], connecting to chip device")
                 wrappedCallback.onMtuChanged(gatt, mtu, status)
                 if(coroutineContinuation.isActive) {
                     coroutineContinuation.resume(gatt)
                 }
             }
         }

        return bluetoothGattCallback
    }

    companion object {

        /**
         * CHIP devices bluetooth filter UUID
         */
        private const val MTR_UUID = "0000FFF6-0000-1000-8000-00805F9B34FB"

        /**
         * Bluetooth scan job timeout
         */
        private const val BLUETOOTH_SCAN_TIMEOUT_MS = 10000L
    }
}