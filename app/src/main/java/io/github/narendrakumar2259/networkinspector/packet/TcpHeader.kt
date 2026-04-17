package io.github.narendrakumar2259.networkinspector.packet

import java.nio.ByteBuffer

data class TcpHeader(
    val sourcePort: Int,
    val destinationPort: Int,
    val sequenceNumber: Long,
    val acknowledgmentNumber: Long,
    val headerLength: Int,
    val flags: Int
) {
    val isSyn: Boolean get() = (flags and 0x02) != 0
    val isAck: Boolean get() = (flags and 0x10) != 0
    val isFin: Boolean get() = (flags and 0x01) != 0
    val isRst: Boolean get() = (flags and 0x04) != 0

    companion object {
        fun parse(buffer: ByteBuffer, ipHeaderLength: Int): TcpHeader {
            val offset = ipHeaderLength

            val sourcePort = buffer.getShort(offset).toInt() and 0xFFFF
            val destinationPort = buffer.getShort(offset + 2).toInt() and 0xFFFF
            val sequenceNumber = buffer.getInt(offset + 4).toLong() and 0xFFFFFFFFL
            val acknowledgmentNumber = buffer.getInt(offset + 8).toLong() and 0xFFFFFFFFL

            val dataOffsetByte = buffer.get(offset + 12).toInt() and 0xFF
            val headerLength = (dataOffsetByte shr 4) * 4

            val flags = buffer.get(offset + 13).toInt() and 0xFF

            return TcpHeader(
                sourcePort = sourcePort,
                destinationPort = destinationPort,
                sequenceNumber = sequenceNumber,
                acknowledgmentNumber = acknowledgmentNumber,
                headerLength = headerLength,
                flags = flags
            )
        }
    }
}