package io.github.narendrakumar2259.networkinspector.vpn

import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class ConnectionTracker(
    private val vpnService: VpnService,
    private val packetWriter: PacketWriter,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "ConnectionTracker"
        private const val BUFFER_SIZE = 32767
    }

    private val connections = mutableMapOf<ConnectionKey, TcpConnection>()

    fun handleTcpPacket(
        sourceIp: String,
        sourcePort: Int,
        destIp: String,
        destPort: Int,
        sequenceNumber: Long,
        acknowledgmentNumber: Long,
        isSyn: Boolean,
        isAck: Boolean,
        isFin: Boolean,
        isRst: Boolean,
        payloadData: ByteArray
    ) {
        val key = ConnectionKey(sourceIp, sourcePort, destIp, destPort)

        when {
            isRst -> handleRst(key)
            isSyn && !isAck -> handleSyn(key, sequenceNumber, destIp, destPort)
            isAck && !isSyn && !isFin -> handleAck(key, payloadData, sequenceNumber)
            isFin -> handleFin(key)
        }
    }

    private fun handleSyn(key: ConnectionKey, seqNumber: Long, destIp: String, destPort: Int) {
        Log.d(TAG, "SYN → ${key.destIp}:${key.destPort}")

        connections[key]?.close()

        val connection = TcpConnection(key)
        connections[key] = connection

        scope.launch(Dispatchers.IO) {
            try {
                val channel = SocketChannel.open()
                channel.configureBlocking(false)
                vpnService.protect(channel.socket())
                channel.connect(InetSocketAddress(destIp, destPort))

                var waited = 0
                while (!channel.finishConnect() && waited < 50) {
                    Thread.sleep(100)
                    waited++
                }

                if (channel.isConnected) {
                    synchronized(connection.lock) {
                        connection.remoteChannel = channel
                        connection.state = TcpConnection.State.SYN_RECEIVED
                        packetWriter.writeSynAck(connection, seqNumber)
                    }
                    Log.d(TAG, "SYN-ACK ← ${key.destIp}:${key.destPort}")
                    startRemoteReader(connection)
                } else {
                    Log.w(TAG, "Connection timeout: ${key.destIp}:${key.destPort}")
                    channel.close()
                    synchronized(connection.lock) {
                        packetWriter.writeRst(connection)
                    }
                    connections.remove(key)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connect failed: ${key.destIp}:${key.destPort} - ${e.message}")
                synchronized(connection.lock) {
                    packetWriter.writeRst(connection)
                }
                connections.remove(key)
            }
        }
    }

    private fun handleAck(key: ConnectionKey, payload: ByteArray, seqNumber: Long) {
        val connection = connections[key] ?: return

        synchronized(connection.lock) {
            if (connection.state == TcpConnection.State.SYN_RECEIVED) {
                connection.state = TcpConnection.State.ESTABLISHED
                connection.appSequenceNumber = seqNumber
                Log.d(TAG, "ESTABLISHED → ${key.destIp}:${key.destPort}")
            }

            if (payload.isNotEmpty() && connection.state == TcpConnection.State.ESTABLISHED) {
                val expectedSeq = connection.appSequenceNumber
                if (seqNumber >= expectedSeq) {
                    connection.appSequenceNumber = seqNumber + payload.size
                    packetWriter.writeAck(connection)

                    // Forward immediately in same thread, inside the lock
                    try {
                        connection.remoteChannel?.write(ByteBuffer.wrap(payload))
                        Log.d(TAG, "→ Forwarded ${payload.size} bytes to ${key.destIp}:${key.destPort}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Forward failed: ${e.message}")
                        packetWriter.writeRst(connection)
                        connection.close()
                        connections.remove(key)
                    }
                }
            }
        }
    }

    private fun handleFin(key: ConnectionKey) {
        val connection = connections[key] ?: return
        Log.d(TAG, "FIN → ${key.destIp}:${key.destPort}")

        synchronized(connection.lock) {
            connection.appSequenceNumber += 1
            connection.state = TcpConnection.State.CLOSING
            packetWriter.writeFinAck(connection)
        }

        connection.close()
        connections.remove(key)
    }

    private fun startRemoteReader(connection: TcpConnection) {
        scope.launch(Dispatchers.IO) {
            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val channel = connection.remoteChannel ?: return@launch

            try {
                while (connection.state != TcpConnection.State.CLOSED) {
                    buffer.clear()
                    val bytesRead = channel.read(buffer)

                    if (bytesRead > 0) {
                        buffer.flip()
                        val data = ByteArray(bytesRead)
                        buffer.get(data)

                        synchronized(connection.lock) {
                            packetWriter.writeData(connection, data)
                        }

                        Log.d(TAG, "← Received ${data.size} bytes from ${connection.key.destIp}:${connection.key.destPort}")
                    } else if (bytesRead == -1) {
                        Log.d(TAG, "Server closed: ${connection.key.destIp}:${connection.key.destPort}")
                        synchronized(connection.lock) {
                            packetWriter.writeFinAck(connection)
                        }
                        connection.close()
                        break
                    } else {
                        Thread.sleep(10)
                    }
                }
            } catch (e: Exception) {
                if (connection.state != TcpConnection.State.CLOSED) {
                    Log.e(TAG, "Remote read error: ${e.message}")
                    connection.close()
                }
            }
        }
    }

    private fun handleRst(key: ConnectionKey) {
        val connection = connections[key] ?: return
        Log.d(TAG, "RST → ${key.destIp}:${key.destPort}")
        connection.close()
        connections.remove(key)
    }


    fun closeAll() {
        connections.values.forEach { it.close() }
        connections.clear()
    }
}