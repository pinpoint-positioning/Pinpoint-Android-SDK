package de.pinpoint.android_demo_app

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import de.easylocate.mobile.api.ApiEventListener
import de.easylocate.mobile.api.EasyLocateBleDevice
import de.easylocate.mobile.api.EasyLocateState
import de.easylocate.mobile.api.OobConfigData
import de.easylocate.mobile.api.PositionUpdateListener
import de.easylocate.mobile.api.TraceletApi
import de.easylocate.mobile.data.coords.Wgs84Reference
import de.easylocate.mobile.data.position.PositionLocal
import de.easylocate.mobile.data.position.PositionWgs84
import de.easylocate.sdk.android.service.EasyLocateSdk
import de.easylocate.sdk.android.service.UwbServiceExplorer

class MainViewModel : ViewModel() {
    var isConnecting by mutableStateOf(false)
    var isConnected by mutableStateOf(false)

    var localX by mutableDoubleStateOf(0.0)
    var localY by mutableDoubleStateOf(0.0)
    var localZ by mutableDoubleStateOf(0.0)
    var localAcc by mutableDoubleStateOf(0.0)

    var latitude by mutableDoubleStateOf(0.0)
    var longitude by mutableDoubleStateOf(0.0)

    var refLatitude by mutableDoubleStateOf(50.0)
    var refLongitude by mutableDoubleStateOf(17.0)
    var refAzimuth  by mutableDoubleStateOf(172.0)

    var traceletApi: TraceletApi? = null

    private var uwbType = UwbServiceExplorer.UwbType.NONE
    private val TAG = "MainViewModel"

    // SDK Event Listener
    val sdkEventListener = object : EasyLocateSdk.EventListener {

        override fun onTraceletApiReady(traceletApi: TraceletApi) {
            this@MainViewModel.traceletApi = traceletApi
            traceletApi.registerEventListener(elApiEventListener)

            Log.d(TAG, "Tracelet API is ready: $traceletApi")
        }

        override fun onMissingRights(missingRights: List<String>) {
            Log.d(TAG, "Missing rights: $missingRights")
        }
    }

    private val elApiEventListener = object : ApiEventListener {
        // Receives all scan BLE scan results
        override fun onDeviceScanResults(easyLocateDevices: List<EasyLocateBleDevice>) {}

        // Receives TRACElet settings from BLE OOB scan
        override fun onOOBConfigResults(config: OobConfigData) {
            Log.d(TAG, "Received OOB Config: $config")
            if (config.channel != 0) {
                traceletApi?.setRadioSettings(config.channel, config.preamble)
            }
        }

        // Receives a single TRACElet that is close to the phone
        override fun onDeviceApproached(device: EasyLocateBleDevice) {
            traceletApi?.connectDevice(device.deviceAddress)
            Log.d(TAG, "Device approached: ${device.deviceAddress}")
            isConnected = true
            isConnecting = false
        }

        // Receives changes in the API state
        override fun onNewApiState(easyLocateState: EasyLocateState) {
            Log.d("elApiEventListener", "New API state: $easyLocateState")
            when (easyLocateState) {
                // Start Positioning when device is connected
                EasyLocateState.CONNECTED -> startPositioning()
                EasyLocateState.INIT ->  Log.d(TAG, "New API state: $easyLocateState")
                EasyLocateState.IDLE ->  Log.d(TAG, "New API state: $easyLocateState")
            }
        }
    }


    fun updateUwbType(type: UwbServiceExplorer.UwbType) {
        uwbType = type

    }

    // Light up a LED on the TRACElet
    fun showMe() {
        traceletApi?.showDevice()
    }

    // Start positioning engine on TRACElet or native UWB chip
    fun startPositioning() {
        when (uwbType) {
            UwbServiceExplorer.UwbType.FIRA_20 -> {
                Log.d("UWB Type", "Phone supports native UWB positioning")
                // Native UWB must use channel 9 UWB networks
                traceletApi?.setRadioSettings(uwbChannel = 9, preamble = 9)
            }

            UwbServiceExplorer.UwbType.NONE -> {
                Log.d("UWB Type", "Phone does NOT support native UWB positioning")
                // Radio Settings for BLE TRACElet are set via onOOBConfigResults()
            }

            else -> {
                Log.d("UWB Type", "Unknown UWB type: $uwbType")
            }
        }

        traceletApi?.startPositioning(positionUpdateListener)
        updateWgs84References()
    }



    fun updateWgs84References() {
        traceletApi?.setConfigWgs84Reference(
            Wgs84Reference(
            latitude = refLatitude,
            longitude = refLongitude,
            azimuth = refAzimuth
        )
        )
    }


    private val positionUpdateListener = object : PositionUpdateListener {
        override fun onPositionLocalUpdate(positionLocal: PositionLocal) {
            Log.d(TAG, "New Pos: $positionLocal")
            localX = positionLocal.x.toDouble()
            localY = positionLocal.y.toDouble()
            localZ = positionLocal.z.toDouble()
            localAcc = positionLocal.accuracy.toDouble()
        }

        override fun onPositionWgs84Update(positionWgs84: PositionWgs84) {
            Log.d(TAG, "New WGS84 Pos: $positionWgs84")
            latitude = positionWgs84.lat
            longitude = positionWgs84.lon
        }
    }



    // Start the connection process
    fun connect() {
        val api = traceletApi ?: run {
            Log.d(TAG, "Tracelet API not ready yet")
            return
        }
        Log.d(TAG, "Starting device scan...")
        isConnecting = true
        isConnected = false
        api.startDeviceScan()
    }

    // Disconnect from device
    fun disconnect() {
        traceletApi?.disconnectDevice()
        isConnected = false
        isConnecting = false
        Log.d(TAG, "Disconnected")
    }
}