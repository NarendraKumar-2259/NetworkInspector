package io.github.narendrakumar2259.networkinspector.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_requests")
data class NetworkRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceIp: String,
    val sourcePort: Int,
    val destIp: String,
    val destPort: Int,
    val method: String? = null,        // GET, POST etc. (null if HTTPS/encrypted)
    val path: String? = null,          // /api/orders (null if encrypted)
    val host: String? = null,          // from Host header
    val headers: String? = null,       // JSON string of headers
    val requestBody: String? = null,   // request body if present
    val responseCode: Int? = null,     // HTTP status code
    val requestSize: Int = 0,          // bytes sent
    val responseSize: Int = 0,         // bytes received
    val isEncrypted: Boolean = false,  // true if port 443
    val tcpState: String = "SYN"       // SYN, ESTABLISHED, CLOSED
)