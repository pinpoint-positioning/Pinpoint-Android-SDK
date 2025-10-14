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
import de.easylocate.sdk.android.hw.uwb.FiraPositioningApi
import de.easylocate.sdk.android.service.EasyLocateSdk
import de.easylocate.sdk.android.service.UwbServiceExplorer

class MainViewModel : ViewModel() {
    var isConnecting by mutableStateOf(false)
    var isConnected by mutableStateOf(false)
    var uwbType = UwbServiceExplorer.UwbType.NONE

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

    // SDK Event Listener
    val sdkEventListener = object : EasyLocateSdk.EventListener {
        private val TAG = "MainViewModel"

        override fun onTraceletApiReady(api: TraceletApi) {
            traceletApi = api
            api.registerEventListener(satletScanListener)
            api.registerEventListener(elApiEventListener)


            Log.d(TAG, "✅ Tracelet API is ready: $api")
        }

        override fun onMissingRights(missingRights: List<String>) {
            Log.d(TAG, "Missing rights: $missingRights")
        }
    }

    private val elApiEventListener = object : ApiEventListener {
        override fun onDeviceScanResults(easyLocateDevices: List<EasyLocateBleDevice>) {}
        override fun onOOBConfigResults(config: OobConfigData) {
            traceletApi?.setRadioSettings(uwbChannel = config.channel, preamble = config.preamble)
        }
        override fun onDeviceApproached(device: EasyLocateBleDevice) {
            traceletApi?.connectDevice(device.deviceAddress)
            Log.d("MainViewModel", "Device approached: ${device.deviceAddress}")
            isConnected = true
            isConnecting = false
        }
        override fun onNewApiState(easyLocateState: EasyLocateState) {
            Log.d("MainViewModel", "New API state: $easyLocateState")
            when (easyLocateState) {
                EasyLocateState.CONNECTED -> startPositioning()
                EasyLocateState.INIT ->  Log.d("MainViewModel", "New API state: $easyLocateState")
                EasyLocateState.IDLE ->  Log.d("MainViewModel", "New API state: $easyLocateState")
            }

        }
    }
    fun updateUwbType(type: UwbServiceExplorer.UwbType) {
        uwbType = type

    }

    fun showMe() {
        traceletApi?.showDevice()
    }

    fun startPositioning() {
        when (uwbType) {
            UwbServiceExplorer.UwbType.FIRA_20 -> {
                Log.d("UWB Type", "Phone supports native UWB positioning")
                // Native UWB must use channel 9 UWB networks
                traceletApi?.setRadioSettings(uwbChannel = 9, preamble = 9)
            }

            UwbServiceExplorer.UwbType.NONE -> {
                Log.d("UWB Type", "Phone does NOT support native UWB positioning")
                // Radio Settings for BLE TRACElet are configured automatically
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
            Log.d("Position", "New Pos: $positionLocal")
            localX = positionLocal.x.toDouble()
            localY = positionLocal.y.toDouble()
            localZ = positionLocal.z.toDouble()
            localAcc = positionLocal.accuracy.toDouble()
        }

        override fun onPositionWgs84Update(positionWgs84: PositionWgs84) {
            Log.d("Position", "New WGS85 Pos: $positionWgs84")
            latitude = positionWgs84.lat
            longitude = positionWgs84.lon
        }
    }

    private val satletScanListener = object : ApiEventListener {
        override fun onDeviceScanResults(easyLocateDevices: List<EasyLocateBleDevice>) {
            Log.d("MainViewModel", "Device scan results: $easyLocateDevices")
        }
        override fun onOOBConfigResults(config: OobConfigData) {
            Log.d("MainViewModel", "OOB Config: $config")
            if (config.channel != 0) {

                traceletApi?.setRadioSettings(config.channel, config.preamble)
            }

        }
        override fun onDeviceApproached(device: EasyLocateBleDevice) {
            traceletApi?.connectDevice(device.deviceAddress)
        }
        override fun onNewApiState(easyLocateState: EasyLocateState) {
            Log.d("MainViewModel", "New API state: $easyLocateState")
        }
    }

    fun connect() {
        val api = traceletApi ?: run {
            Log.d("MainViewModel", "Tracelet API not ready yet")
            return
        }

        Log.d("MainViewModel", "Starting device scan...")
        isConnecting = true
        isConnected = false
        api.startDeviceScan()
    }

    fun disconnect() {
        traceletApi?.disconnectDevice()
        isConnected = false
        isConnecting = false
        Log.d("MainViewModel", "Disconnected")
    }
}