package io.github.narendrakumar2259.networkinspector

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.narendrakumar2259.networkinspector.crypto.CertificateAuthority
import io.github.narendrakumar2259.networkinspector.data.model.NetworkRequest
import io.github.narendrakumar2259.networkinspector.ui.dashboard.DashboardScreen
import io.github.narendrakumar2259.networkinspector.ui.dashboard.DashboardViewModel
import io.github.narendrakumar2259.networkinspector.ui.detail.DetailScreen
import io.github.narendrakumar2259.networkinspector.ui.theme.NetworkInspectorTheme
import io.github.narendrakumar2259.networkinspector.vpn.InspectorVpnService

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var certificateAuthority: CertificateAuthority

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        certificateAuthority = CertificateAuthority(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        setContent {
            NetworkInspectorTheme {
                var selectedRequest by remember { mutableStateOf<NetworkRequest?>(null) }

                if (selectedRequest != null) {
                    DetailScreen(
                        request = selectedRequest!!,
                        onBack = { selectedRequest = null }
                    )
                } else {
                    DashboardScreen(
                        viewModel = viewModel,
                        onStartVpn = { prepareAndStartVpn() },
                        onStopVpn = { stopVpnService() },
                        onInstallCert = { installCertificate() },
                        onRequestClick = { request ->
                            selectedRequest = request
                        }
                    )
                }
            }
        }
    }

    private fun prepareAndStartVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val serviceIntent = Intent(this, InspectorVpnService::class.java)
        startService(serviceIntent)
        viewModel.setVpnRunning(true)
    }

    private fun stopVpnService() {
        val serviceIntent = Intent(this, InspectorVpnService::class.java)
        stopService(serviceIntent)
        viewModel.setVpnRunning(false)
    }

    private fun installCertificate() {
        certificateAuthority.installCA()
        Toast.makeText(
            this,
            "Certificate saved to Downloads. Go to Settings → Security → Install Certificate",
            Toast.LENGTH_LONG
        ).show()
    }
}