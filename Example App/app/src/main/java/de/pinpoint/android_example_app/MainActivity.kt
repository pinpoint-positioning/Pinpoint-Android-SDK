package de.pinpoint.android_example_app

import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.os.Build
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import de.pinpoint.sdk.api.PinpointLicensingCallback
import de.pinpoint.sdk.api.PinpointLocationServices.getPinpointLocationProviderClient
import kotlin.time.Duration.Companion.seconds


class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                contentResolver.openInputStream(uri)?.use { input ->
                    viewModel.binFile = input.readBytes()
                    // Try to get filename
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            cursor.moveToFirst()
                            viewModel.binFileName = cursor.getString(nameIndex)
                        }
                    }
                }
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                Log.d("MainActivity", "All permissions granted")
            } else {
                Log.e("MainActivity", "Permissions denied: ${permissions.filter { !it.value }.keys}")
            }
        }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RANGING,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestRequiredPermissions()

        val licensingCallback = object: PinpointLicensingCallback() {

            val TAG = "PinpointLicensingCallback"
            /**
             *  This function is called when the offline tokens are expired
             */
            override fun onTokenExpired() {
                Log.e(TAG, "Token expired")
                viewModel.stopPositioning()
            }

            /**
             * This function is called 2 days before the token is expiring
             * @param expiringIn: the duration in seconds from now when the licensing tokens are expiring
             */
            override fun onTokenExpiringSoon(expiringIn: Long) {
                Log.w(TAG,"Token expiring in ${expiringIn.seconds}")
            }

        }
        getPinpointLocationProviderClient(
            this,
            BuildConfig.PINPOINT_API_KEY,
            licensingCallback
        ).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.i("onComplete","Set indoorClient")
                // do something with the client
                val client = task.result
                viewModel.setIndoorClient(client)
                viewModel.openDocumentLauncher = openDocumentLauncher
                setContent {
                    MaterialTheme {
                        MainScreen(viewModel)
                    }
                }


            } else {
                Log.w("getPinpointLocationProviderClient", "Received error ${task.exception}")
            }


        }



    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopPositioning()
    }
}


// --------------------------------------------------
// UI Composable
// --------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {


    var showStartSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pinpoint SDK Example App",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Banner
            ConnectionStatusBanner(
                isConnected = viewModel.isConnected,
                isConnecting = viewModel.isConnecting
            )

            // Local Position Card
            PositionCard(
                title = "Local Position",
                icon = Icons.Default.Place,
                localX = viewModel.localX,
                localY = viewModel.localY,
                localAccuracy = viewModel.localAcc
            )

            // WGS84 Coordinates Card
            Wgs84Card(
                viewModel = viewModel
            )

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Connect/Disconnect Button
                Button(
                    onClick = {
                        if (!viewModel.isConnected && !viewModel.isConnecting) {
                            showStartSheet = true
                        } else {
                            viewModel.stopPositioning()
                        }
                    },
                    enabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.isConnected)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {


                    Text(
                        if (viewModel.isConnected || viewModel.isConnecting) "Stop Positioning" else "Start Positioning",
                        style = MaterialTheme.typography.labelLarge
                    )

                }

                // Show Me Button
                if (viewModel.isConnected) {
                    FilledTonalButton(
                        onClick = { viewModel.showMe() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Show Me",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                if (!viewModel.isUwbNative) {
                    // Info Card
                    InfoCard(
                        text = "Hold your TRACElet close to your phone."
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Version Footer
            Text(
                text = "App v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) " +
                        "- ${BuildConfig.BUILD_TYPE} - ${BuildConfig.GIT_BRANCH}@${BuildConfig.GIT_COMMIT}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                textAlign = TextAlign.Center
            )

            Text(
                text = "SDK v${BuildConfig.PINPOINT_SDK_VERSION}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                textAlign = TextAlign.Center
            )


        }
    }

    if (showStartSheet) {
        StartPositioningSheet(
            viewModel = viewModel,
            onDismiss = { showStartSheet = false },
            onStart = { siteId, binFile ->
                showStartSheet = false
                viewModel.startPositioning(Looper.getMainLooper(), siteId, binFile)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartPositioningSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onStart: (Int, ByteArray) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var siteId by remember { mutableStateOf(viewModel.siteId) }
    val binFile = viewModel.binFile
    val binFileName = viewModel.binFileName

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Start Positioning", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = siteId,
                onValueChange = {
                    val filtered = it.filter { c -> c.isDigit() || c.lowercaseChar() in 'a'..'f' }
                    if (filtered.length <= 8) {
                        siteId = filtered
                        viewModel.siteId = filtered
                    }
                },
                label = { Text("Site ID (Hex)") },
                prefix = { Text("0x") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.openDocumentLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (binFileName != null) "Selected: $binFileName" else "Select BIN File")
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val id = siteId.toLongOrNull(16)?.toInt() ?: 0
                    if (binFile != null) {
                        onStart(id, binFile)
                    }
                },
                enabled = binFile != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start")
            }
        }
    }
}

// --------------------------------------------------
// Components
// --------------------------------------------------

@Composable
fun ConnectionStatusBanner(isConnected: Boolean, isConnecting: Boolean) {
    val statusColor = when {
        isConnecting -> MaterialTheme.colorScheme.tertiary
        isConnected -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val statusText = when {
        isConnecting -> "Connecting..."
        isConnected -> "Connected"
        else -> "Not Connected"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = statusColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(statusColor, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = statusColor
            )
        }
    }
}

@Composable
fun PositionCard(
    title: String,
    icon: ImageVector,
    localX: Double,
    localY: Double,
    localAccuracy: Double
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider()

            // Coordinates Grid
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                CoordinateView("X", localX, Modifier.weight(1f))
                CoordinateView("Y", localY, Modifier.weight(1f))
            }

            // Accuracy Banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Accuracy: ± ${"%.2f".format(localAccuracy)} m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CoordinateView(label: String, value: Double, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "%.2f".format(value),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "m",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Wgs84Card(viewModel: MainViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }

    // Main Card
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "WGS84 Coordinates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CoordinateRow("Latitude", viewModel.latitude.toString())
                CoordinateRow("Longitude", viewModel.longitude.toString())
            }
        }
    }

    // Bottom Sheet
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            var latitude by remember { mutableStateOf(viewModel.refLatitude.toString()) }
            var longitude by remember { mutableStateOf(viewModel.refLongitude.toString()) }
            var azi by remember { mutableStateOf(viewModel.refAzimuth.toString()) }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Edit Coordinates", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = azi,
                    onValueChange = { azi = it },
                    label = { Text("Azimuth") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.refLatitude = latitude.toDouble()
                        viewModel.refLongitude = longitude.toDouble()
                        viewModel.refAzimuth = azi.toDouble()
                        showSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
            }
        }
    }
}


@Composable
fun CoordinateRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun InfoCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}