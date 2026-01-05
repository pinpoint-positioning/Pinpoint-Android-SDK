# Pinpoint Android SDK


## Table of Contents

- [Introduction](#introduction)
- [Screenshots](#screenshots)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Versioning](#versioning)
- [Installation](#installation)
- [Usage](#usage)
- [License](#license)


## Introduction
This Kotlin-based demo application demonstrates how to integrate and use [Pinpoint's](https://pinpoint.de) Android SDK for [FiRa](https://www.firaconsortium.org)-compliant Ultra-Wideband (UWB) positioning.



## Screenshots

| Android Demo Application | 
|:---:|
| <img src="images/android-demo-app-screen.png" alt="Routing Solution" width="300"/> |
| *Demo Android App showing local position* |



## Features 

* Indoor Positioning for GNSS/GPS denied areas
* Accuracy of up to 30 cm
* Simple Integration

---


## Prerequisites

Before integrating the Pinpoint Android SDK, please ensure you have access to the necessary Pinpoint hardware components.

The SDK requires compatible **Pinpoint Hardware** for accurate indoor positioning. Depending on your use case, you can use one of the following hardware options:

- **[Prototyping Kit](https://pinpoint.de/en/products/hardware/prototyping-kit):**  
  Ideal for developers and researchers who want to quickly evaluate and experiment with Pinpoint’s indoor positioning capabilities.  
  The kit includes all essential components required to set up a small-scale test environment.

- **[SATlets](https://pinpoint.de/en/products/hardware/satlet):**  
  Compact satellite modules designed for scalable and permanent installations.  
  SATlets are suitable for production environments or larger deployments requiring reliable and precise indoor localization.

To ensure optimal performance, confirm that your hardware is correctly installed and configured before running the SDK.


## Versioning
This package highly depends on the Pinpoint Hardware you are using.

Make sure to use the corresponding version when adding this package to your project.

**Current supported version: 12.2.0**


## Installation

1. Add the SDK package source to your `settings.gradle.kts`.

```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        //Add this repo for Pinpoint dependencies
        maven {
            url = uri("https://gitlab.com/api/v4/projects/26571989/packages/maven")
        }
    }
}
```
2. Add the required dependencies to your app-level `build.grade.kts`

```
val easylocateVersion = "12.1.1"
dependencies {
    implementation("de.easylocate:core:$easylocateVersion")
    implementation("de.easylocate:android-sdk:$easylocateVersion")
}
```




## Usage

To use the `Pinpoint Kotlin SDK` in your project, follow the steps below.

The provided demo app is this repo can be used as an implementation example for the SDK.

The usage examples below can be found in `MainViewModel.kt` inside the demo app.

```kotlin
   // Initialize SDK
    easyLocateSdk = EasyLocateSdk(this)
    easyLocateSdk.start(viewModel.sdkEventListener)
```

1. **Create a listener for the SDK**

```kotlin

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
```

2.  **Create an `ApiEventListener` to get notified on the relevant events.**

```kotlin
    private val elApiEventListener = object : ApiEventListener {
        // Receives all scan BLE scan results
        override fun onDeviceScanResults(easyLocateDevices: List<EasyLocateBleDevice>) {}

        // Receives TRACElet settings from BLE OOB scan
        override fun onOOBConfigResults(config: OobConfigData) {
            if (config.channel != 0) {
                traceletApi?.setRadioSettings(config.channel, config.preamble)
            }
        }

        // Receives a single TRACElet that is close to the phone
        override fun onDeviceApproached(device: EasyLocateBleDevice) {
            traceletApi?.connectDevice(device.deviceAddress)
        }

        // Receives changes in the API state
        override fun onNewApiState(easyLocateState: EasyLocateState) {
            when (easyLocateState) {
                // Start Positioning when device is connected
                EasyLocateState.CONNECTED -> startPositioning()
                EasyLocateState.INIT ->  Log.d(TAG, "New API state: $easyLocateState")
                EasyLocateState.IDLE ->  Log.d(TAG, "New API state: $easyLocateState")
            }
        }
    }

```

3. **Start the BLE scan. Scan results will trigger `onDeviceApproached` and `onDeviceScanResults`.**

```kotlin
    fun startScan() {
        val api = traceletApi ?: run {
            return
        }
        api.startDeviceScan()
    }
```

4. **Once a TRACElet is close enough to the phone, it is considered as `approached`. First, connect to it then start the positioning engine.**

```kotlin
    fun startPositioning() {
        when (uwbType) {
            UwbServiceExplorer.UwbType.FIRA_20 -> {
                // Native UWB must use channel 9 UWB networks!
                traceletApi?.setRadioSettings(uwbChannel = 9, preamble = 9)
            }

            UwbServiceExplorer.UwbType.NONE -> {
                // Radio Settings for BLE TRACElet are set via onOOBConfigResults()
            }

            else -> {
                Log.d("UWB Type", "Unknown UWB type: $uwbType")
            }
        }

        traceletApi?.startPositioning(positionUpdateListener)
        // Optional: Set the WGS84 references to receive WGS84 coordinates from the callback.
        updateWgs84References()
    }
```
**Note**: `updateWgs84References()` is described below.


5. **Create a `PositionUpdateListener` that provides the local position.
You can set a WGS84 reference to get the local position converted to world coordinates (WGS84).**

```kotlin
    private val positionUpdateListener = object : PositionUpdateListener {
        override fun onPositionLocalUpdate(positionLocal: PositionLocal) {
            localX = positionLocal.x.toDouble()
            localY = positionLocal.y.toDouble()
            localZ = positionLocal.z.toDouble()
            localAcc = positionLocal.accuracy.toDouble()
        }

        override fun onPositionWgs84Update(positionWgs84: PositionWgs84) {
            latitude = positionWgs84.lat
            longitude = positionWgs84.lon
        }
    }
```

6. **Set the WGS84 reference.**
The WGS84 reference can be set via *EasyPlan*.

```kotlin
    fun updateWgs84References() {
        traceletApi?.setConfigWgs84Reference(
            Wgs84Reference(
            latitude = refLatitude,
            longitude = refLongitude,
            azimuth = refAzimuth)
        )
    }
```
7. **Disconnect from Device**

```kotlin
    fun disconnect() {
        traceletApi?.disconnectDevice()
    }
```

### License 

This package is licensed under a proprietary license. Please refer to the LICENSE file for more details.
