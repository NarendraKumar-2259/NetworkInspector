    package io.github.narendrakumar2259.networkinspector.vpn

    import java.nio.channels.SocketChannel

    data class ConnectionKey(
        val sourceIp: String,
        val sourcePort: Int,
        val destIp: String,
        val destPort: Int
    )

    class TcpConnection(
        val key: ConnectionKey
    ) {
        enum class State {
            SYN_RECEIVED,
            ESTABLISHED,
            CLOSING,
            CLOSED
        }

        val lock = Object()

        var state: State = State.SYN_RECEIVED

        // Our sequence number (what we send to the app)
        var ourSequenceNumber: Long = 1000L

        // App's sequence number (what the app sends us)
        var appSequenceNumber: Long = 0L

        // The real connection to the internet
        var remoteChannel: SocketChannel? = null

        fun close() {
            state = State.CLOSED
            try {
                remoteChannel?.close()
            } catch (_: Exception) {}
            remoteChannel = null
        }
    }