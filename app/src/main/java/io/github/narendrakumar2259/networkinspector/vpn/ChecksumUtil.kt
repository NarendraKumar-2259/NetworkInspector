package io.github.narendrakumar2259.networkinspector.vpn

import java.nio.ByteBuffer

object ChecksumUtil {

    fun calculateIPChecksum(header: ByteArray): Short {
        var sum = 0L

        // Sum all 16-bit words
        var i = 0
        while (i < header.size) {
            if (i == 10) {
                // Skip checksum field itself (bytes 10-11)
                i += 2
                continue
            }
            val word = ((header[i].toInt() and 0xFF) shl 8) or
                    (header.getOrElse(i + 1) { 0 }.toInt() and 0xFF)
            sum += word
            i += 2
        }

        // Add carry bits
        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        return (sum.inv() and 0xFFFF).toShort()
    }

    fun calculateTCPChecksum(
        sourceIp: ByteArray,
        destIp: ByteArray,
        tcpSegment: ByteArray
    ): Short {
        var sum = 0L

        // Pseudo header
        for (i in 0..1) {
            val word = ((sourceIp[i * 2].toInt() and 0xFF) shl 8) or
                    (sourceIp[i * 2 + 1].toInt() and 0xFF)
            sum += word
        }
        for (i in 0..1) {
            val word = ((destIp[i * 2].toInt() and 0xFF) shl 8) or
                    (destIp[i * 2 + 1].toInt() and 0xFF)
            sum += word
        }
        sum += 6 // Protocol TCP
        sum += tcpSegment.size // TCP length

        // TCP segment (with checksum field zeroed)
        var i = 0
        while (i < tcpSegment.size) {
            if (i == 16) {
                // Skip checksum field (bytes 16-17 of TCP header)
                i += 2
                continue
            }
            val word = ((tcpSegment[i].toInt() and 0xFF) shl 8) or
                    (tcpSegment.getOrElse(i + 1) { 0 }.toInt() and 0xFF)
            sum += word
            i += 2
        }

        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        return (sum.inv() and 0xFFFF).toShort()
    }
}