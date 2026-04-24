package io.github.narendrakumar2259.networkinspector

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.narendrakumar2259.networkinspector.crypto.CertificateAuthority
import io.github.narendrakumar2259.networkinspector.ui.theme.NetworkInspectorTheme
import io.github.narendrakumar2259.networkinspector.vpn.InspectorVpnService

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){ result ->
        if(result.resultCode == RESULT_OK) {
            startVpnService()
        }

    }

    lateinit var certificateAuthority: CertificateAuthority

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        certificateAuthority = CertificateAuthority(this)
        setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { prepareAndStartVpn() }) {
                    Text("Start Inspection")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    certificateAuthority.installCA()
                    // Show a message telling user where to find the file
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Certificate saved to Downloads folder. Go to Settings → Security → Install Certificate",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }) {
                    Text("Install CA Certificate")
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
    }

}