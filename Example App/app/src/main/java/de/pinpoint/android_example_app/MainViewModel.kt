package de.pinpoint.android_example_app

import android.app.Application
import android.content.Intent
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import de.pinpoint.sdk.api.PinpointLocation
import de.pinpoint.sdk.api.PinpointLocationAvailability
import de.pinpoint.sdk.api.PinpointLocationCallback
import de.pinpoint.sdk.api.PinpointLocationProviderClient
import de.pinpoint.sdk.api.PinpointLocationRequest


class MainViewModel(application: Application) : AndroidViewModel(application) {
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
    var siteId by mutableStateOf("")
    var binFile by mutableStateOf<ByteArray?>(null)
    var binFileName by mutableStateOf<String?>(null)

    private val TAG = "MainViewModel"


    lateinit var indoorLocationClient: PinpointLocationProviderClient

    lateinit var openDocumentLauncher:  ActivityResultLauncher<Array<String>>


    val isUwbNative: Boolean by lazy {
        indoorLocationClient.isUwbNativeSupported
    }

    val locationRequest = PinpointLocationRequest.Builder().build()


    val locationCallback = object: PinpointLocationCallback() {
        override fun onLocationUpdate(location: PinpointLocation) {
            Log.i(
                "PinpointLocationCallbackTest",
                "Received position: $location (relative location: ${location}, , callback: ${hashCode()}"
            )
            location.apply {
                this@MainViewModel.latitude = location.latitude
                this@MainViewModel.longitude = location.longitude
                    localX = location.x
                    localY = location.y
                    localZ = location.z
                    localAcc = location.accuracy.toDouble()


                isConnected = true
                isConnecting = false

                updateForegroundService("Position: X=${"%.2f".format(localX)}, Y=${"%.2f".format(localY)}")
            }

        }

        override fun onLocationAvailability(locationAvailability: PinpointLocationAvailability) {
            Log.i("MainFragment", "New location availability: $locationAvailability")
            if(locationAvailability.isSetupRequired()) {
                indoorLocationClient.connectTraceletIfNeeded()
            }
        }

    }


    fun setIndoorClient(client: PinpointLocationProviderClient) {
        indoorLocationClient = client
    }


    // Light up a LED on the TRACElet
    fun showMe() {
        indoorLocationClient.showMe()
    }

    // Start positioning engine

    fun startPositioning(looper: Looper, siteID: Int, binFile: ByteArray) {
        isConnecting = true
        // connectTraceletIfNeeded is required for positioning with TRACElets
        Log.i(TAG, "startPositioning: first call connectTraceletIfNeeded....")
        val locationTask = indoorLocationClient.connectTraceletIfNeeded().onSuccessTask {

            indoorLocationClient.sendBinFile(binFile).onSuccessTask {

                indoorLocationClient.setSiteId(siteID).onSuccessTask {
                    indoorLocationClient.showMe()
                    startForegroundService()
                    indoorLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        looper
                    )
                }
            }
        }
        locationTask.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "startPositioning failed with error: ${task.exception}")
                isConnecting = false
            } else {
                Log.i(TAG, "Started requesting location updates")
            }
        }

    }



    // Disconnect from device
    fun stopPositioning() {
        Log.i(TAG, "Stop positioning")
        stopForegroundService()
        val stopTask = indoorLocationClient.removeLocationUpdates(locationCallback)
        stopTask.addOnCompleteListener { task ->
            Log.i(TAG, "Stopped positioning")
            if (!task.isSuccessful) {
                Log.w(TAG,"stopPositioning failed with error: ${task.exception}!")
            }
            isConnecting = false
            isConnected = false
        }
    }

    private fun startForegroundService() {
        val intent = Intent(getApplication(), LocationForegroundService::class.java).apply {
            putExtra("location_text", "Connecting...")
        }
        getApplication<Application>().startService(intent)
    }

    private fun updateForegroundService(text: String) {
        val intent = Intent(getApplication(), LocationForegroundService::class.java).apply {
            putExtra("location_text", text)
        }
        getApplication<Application>().startService(intent)
    }

    private fun stopForegroundService() {
        val intent = Intent(getApplication(), LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }
}