package io.github.narendrakumar2259.networkinspector.vpn

import java.io.FileOutputStream
import java.nio.ByteBuffer

class PacketWriter(
    private val outputStream: FileOutputStream
) {

    fun writeSynAck(connection: TcpConnection, appSeqNumber: Long) {
        connection.appSequenceNumber = appSeqNumber + 1
        val flags = 0x12 // SYN + ACK

        val packet = buildPacket(
            connection = connection,
            flags = flags,
            payload = ByteArray(0),
            isSyn = true
        )
        outputStream.write(packet)
        connection.ourSequenceNumber += 1 // SYN counts as 1 byte
    }

    fun writeAck(connection: TcpConnection) {
        val flags = 0x10 // ACK only

        val packet = buildPacket(
            connection = connection,
            flags = flags,
            payload = ByteArray(0)
        )
        outputStream.write(packet)
    }

    fun writeData(connection: TcpConnection, data: ByteArray) {
        val maxPayload = 1400 // Safe size under typical 1500 MTU

        var offset = 0
        while (offset < data.size) {
            val chunkSize = minOf(maxPayload, data.size - offset)
            val chunk = data.copyOfRange(offset, offset + chunkSize)

            val flags = 0x18 // PSH + ACK
            val packet = buildPacket(
                connection = connection,
                flags = flags,
                payload = chunk
            )
            outputStream.write(packet)
            connection.ourSequenceNumber += chunkSize
            offset += chunkSize
        }
    }

    fun writeFinAck(connection: TcpConnection) {
        val flags = 0x11 // FIN + ACK

        val packet = buildPacket(
            connection = connection,
            flags = flags,
            payload = ByteArray(0)
        )
        outputStream.write(packet)
        connection.ourSequenceNumber += 1 // FIN counts as 1 byte
    }

    fun writeRst(connection: TcpConnection) {
        val flags = 0x14 // RST + ACK

        val packet = buildPacket(
            connection = connection,
            flags = flags,
            payload = ByteArray(0)
        )
        outputStream.write(packet)
    }

    private fun buildPacket(
        connection: TcpConnection,
        flags: Int,
        payload: ByteArray,
        isSyn: Boolean = false
    ): ByteArray {
        val ipHeaderLen = 20
        val tcpHeaderLen = if (isSyn) 24 else 20 // SYN has options
        val totalLen = ipHeaderLen + tcpHeaderLen + payload.size

        val packet = ByteArray(totalLen)
        val buffer = ByteBuffer.wrap(packet)

        // === IP HEADER (20 bytes) ===
        // IMPORTANT: source and dest are SWAPPED
        // because we're responding TO the app

        val srcIpBytes = ipToBytes(connection.key.destIp)  // server → source
        val dstIpBytes = ipToBytes(connection.key.sourceIp) // app → destination

        buffer.put((0x45).toByte())              // Version 4, Header length 5
        buffer.put(0.toByte())                    // Type of service
        buffer.putShort(totalLen.toShort())        // Total length
        buffer.putShort(0.toShort())               // Identification
        buffer.putShort(0x4000.toShort())           // Flags: Don't fragment
        buffer.put(64.toByte())                    // TTL
        buffer.put(6.toByte())                     // Protocol: TCP
        buffer.putShort(0.toShort())               // Checksum (placeholder)
        buffer.put(srcIpBytes)                     // Source IP (server)
        buffer.put(dstIpBytes)                     // Destination IP (app)

        // Calculate and set IP checksum
        val ipChecksum = ChecksumUtil.calculateIPChecksum(packet.copyOf(ipHeaderLen))
        buffer.putShort(10, ipChecksum)

        // === TCP HEADER (20 or 24 bytes) ===
        // Ports are also SWAPPED

        buffer.position(ipHeaderLen)
        buffer.putShort(connection.key.destPort.toShort())    // Source port (server)
        buffer.putShort(connection.key.sourcePort.toShort())   // Dest port (app)
        buffer.putInt(connection.ourSequenceNumber.toInt())     // Sequence number
        buffer.putInt(connection.appSequenceNumber.toInt())     // ACK number
        if (isSyn) {
            buffer.put((0x60).toByte())    // Data offset: 6 (24 bytes)
        } else {
            buffer.put((0x50).toByte())    // Data offset: 5 (20 bytes)
        }
        buffer.put(flags.toByte())                             // Flags
        buffer.putShort(65535.toShort())                        // Window size
        buffer.putShort(0.toShort())                           // Checksum (placeholder)
        buffer.putShort(0.toShort())                           // Urgent pointer

        if (isSyn) {
            // TCP option: MSS = 1460
            buffer.put(0x02.toByte())  // Kind: MSS
            buffer.put(0x04.toByte())  // Length: 4
            buffer.putShort(1460.toShort()) // MSS value
        }

        // Add payload
        if (payload.isNotEmpty()) {
            buffer.put(payload)
        }

        // Calculate and set TCP checksum
        val tcpSegment = packet.copyOfRange(ipHeaderLen, totalLen)
        val tcpChecksum = ChecksumUtil.calculateTCPChecksum(srcIpBytes, dstIpBytes, tcpSegment)
        buffer.putShort(ipHeaderLen + 16, tcpChecksum)

        return packet
    }

    private fun ipToBytes(ip: String): ByteArray {
        return ip.split(".").map { it.toInt().toByte() }.toByteArray()
    }
}