package io.github.narendrakumar2259.networkinspector.vpn

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
import java.io.FileOutputStream
import java.nio.ByteBuffer

class InspectorVpnService : VpnService() {

    companion object {
        private const val TAG = "InspectorVPN"
        private const val MAX_PACKET_SIZE = 32767
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var connectionTracker: ConnectionTracker? = null

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

        Log.d(TAG, "VPN interface established: ${vpnInterface != null}")

        vpnInterface?.let { startPacketReader(it) }
    }

    private fun startPacketReader(vpnInterface: ParcelFileDescriptor) {
        val outputStream = FileOutputStream(vpnInterface.fileDescriptor)
        val packetWriter = PacketWriter(outputStream)
        connectionTracker = ConnectionTracker(this, packetWriter, serviceScope)

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

                            // Extract payload
                            val headerSize = ipHeader.headerLength + tcpHeader.headerLength
                            val payloadSize = length - headerSize
                            val payload = if (payloadSize > 0) {
                                val data = ByteArray(payloadSize)
                                buffer.position(headerSize)
                                buffer.get(data, 0, payloadSize)
                                data
                            } else {
                                ByteArray(0)
                            }

                            // Hand off to connection tracker
                            connectionTracker?.handleTcpPacket(
                                sourceIp = ipHeader.sourceAddress,
                                sourcePort = tcpHeader.sourcePort,
                                destIp = ipHeader.destinationAddress,
                                destPort = tcpHeader.destinationPort,
                                sequenceNumber = tcpHeader.sequenceNumber,
                                acknowledgmentNumber = tcpHeader.acknowledgmentNumber,
                                isSyn = tcpHeader.isSyn,
                                isAck = tcpHeader.isAck,
                                isFin = tcpHeader.isFin,
                                isRst = tcpHeader.isRst,
                                payloadData = payload
                            )
                        }
                        // UDP packets are silently dropped for now

                    } catch (e: Exception) {
                        Log.e(TAG, "Error: ${e.message}")
                    }
                }
            }
        }
    }

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
        connectionTracker?.closeAll()
        connectionTracker = null
        vpnInterface?.close()
        vpnInterface = null
        Log.d(TAG, "VPN interface closed")
        super.onDestroy()
    }
}