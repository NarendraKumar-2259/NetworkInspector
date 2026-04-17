package io.github.narendrakumar2259.networkinspector.vpn

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.narendrakumar2259.networkinspector.packet.IpHeader
import io.github.narendrakumar2259.networkinspector.packet.TcpHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.nio.ByteBuffer

class InspectorVpnService : VpnService() {

    companion object {
        private const val TAG = "InspectorVpnService"
        private const val MAX_PACKET_SIZE = 32767
    }

    private var vpnInterface : ParcelFileDescriptor? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        establishVpn()
        return START_STICKY
    }

    private fun establishVpn() {
        vpnInterface = Builder()
            .setSession("NetworkInspector")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .establish()

        Log.d("$TAG", "VPN established with interface: ${vpnInterface?.fileDescriptor}")
        vpnInterface?.let { startPacketReader(it) }
    }

    private fun startPacketReader(vpnInterface: ParcelFileDescriptor){
        serviceScope.launch {
            val inputStream = FileInputStream(vpnInterface.fileDescriptor)
            val buffer = ByteBuffer.allocate(MAX_PACKET_SIZE)

            while (true) {
                buffer.clear()
                val length = inputStream.read(buffer.array())

                if (length > 0) {
                    try {
                        val ipHeader = IpHeader.parse(buffer)

                        if (ipHeader.protocol == IpHeader.PROTOCOL_TCP) {
                            val tcpHeader = TcpHeader.parse(buffer, ipHeader.headerLength)

                            Log.d(TAG, "${ipHeader.sourceAddress}:${tcpHeader.sourcePort} → " +
                                    "${ipHeader.destinationAddress}:${tcpHeader.destinationPort} " +
                                    "[${if (tcpHeader.isSyn) "SYN " else ""}${if (tcpHeader.isAck) "ACK " else ""}${if (tcpHeader.isFin) "FIN " else ""}]"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Parse error: ${e.message}")
                    }
                }
            }
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundNotification() {
        val channelId = "vpn_channel"

        val channel = NotificationChannel(
            channelId,
            "VPN Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Network Inspector")
            .setContentText("Inspecting network traffic...")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        vpnInterface?.close()
        vpnInterface = null
        Log.d("$TAG", "VPN service destroyed and interface closed")
        super.onDestroy()
    }
}