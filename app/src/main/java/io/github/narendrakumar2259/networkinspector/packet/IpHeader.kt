package io.github.narendrakumar2259.networkinspector.packet

import java.nio.ByteBuffer

data class IpHeader(
    val version: Int,
    val headerLength: Int,
    val totalLength: Int,
    val protocol: Int,
    val sourceAddress: String,
    val destinationAddress: String
) {
    companion object {
        const val PROTOCOL_TCP = 6
        const val PROTOCOL_UDP = 17

        fun parse(buffer: ByteBuffer): IpHeader {
            val versionAndLength = buffer.get(0).toInt() and 0xFF
            val version = versionAndLength shr 4
            val headerLength = (versionAndLength and 0x0F) * 4

            val totalLength = buffer.getShort(2).toInt() and 0xFFFF
            val protocol = buffer.get(9).toInt() and 0xFF

            val sourceAddress = formatIpAddress(buffer, 12)
            val destinationAddress = formatIpAddress(buffer, 16)

            return IpHeader(
                version = version,
                headerLength = headerLength,
                totalLength = totalLength,
                protocol = protocol,
                sourceAddress = sourceAddress,
                destinationAddress = destinationAddress
            )
        }

        private fun formatIpAddress(buffer: ByteBuffer, offset: Int): String {
            return "${buffer.get(offset).toInt() and 0xFF}" +
                    ".${buffer.get(offset + 1).toInt() and 0xFF}" +
                    ".${buffer.get(offset + 2).toInt() and 0xFF}" +
                    ".${buffer.get(offset + 3).toInt() and 0xFF}"
        }
    }
}