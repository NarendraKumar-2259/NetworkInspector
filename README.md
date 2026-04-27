# Network Inspector

An Android network traffic analyzer that intercepts, parses, and displays device network traffic in real-time using Android's VpnService API.

## What It Does

Network Inspector captures all outgoing network traffic from your Android device and displays it in a clean, real-time UI. It works by creating a virtual network interface (TUN) that all device traffic flows through, parsing raw IP and TCP packet headers, and forwarding traffic to the internet while logging every request.

## Features

- **Traffic Interception** — Captures all device network traffic via VpnService and TUN interface
- **Packet Parsing** — Custom IP and TCP header parsers extract source/destination addresses, ports, and protocol flags from raw byte streams
- **HTTP Inspection** — Parses HTTP requests (port 80) to extract method, URL, headers, and body
- **TCP Connection Management** — Handles the full TCP lifecycle (SYN handshake, data forwarding, teardown) with protected sockets to prevent VPN loopback
- **Real-time UI** — Live dashboard showing captured requests as they happen
- **Request Details** — Tap any request to see full connection info, headers, and body
- **Persistent Storage** — Captured requests saved to local Room database
- **Auto-cleanup** — Automatically trims stored requests to prevent storage overflow

## Architecture

```
Device Traffic
      ↓
TUN Interface (VpnService)
      ↓
Packet Reader (Coroutine — IO Dispatcher)
      ↓
IP Header Parser → extract source/dest IP
      ↓
TCP Header Parser → extract port numbers
      ↓
Connection Tracker → manage TCP state
      ↓
HTTP Parser → extract method, URL, headers (port 80)
      ↓
Repository → Room DB (IO Dispatcher)
      ↓
ViewModel → StateFlow (Main Dispatcher)
      ↓
Compose UI → LazyColumn updates in real time
```

## Tech Stack

- **Kotlin**
- **Jetpack Compose** — UI
- **VpnService API** — Traffic interception
- **Room** — Local database
- **Coroutines + Flow** — Async operations and reactive data
- **MVVM + Clean Architecture**

## Project Structure

```
├── vpn/                    — VPN service and TCP forwarding
│   ├── InspectorVpnService.kt
│   ├── ConnectionTracker.kt
│   ├── TcpConnection.kt
│   ├── PacketWriter.kt
│   └── ChecksumUtil.kt
├── packet/                 — Raw packet parsers
│   ├── IpHeader.kt
│   ├── TcpHeader.kt
│   └── HttpParser.kt
├── crypto/                 — Certificate generation (for future HTTPS decryption)
│   └── CertificateAuthority.kt
├── data/
│   ├── model/              — Data classes
│   ├── local/              — Room DB and DAOs
│   └── repository/         — Repository layer
└── ui/
    ├── dashboard/          — Main screen with live request list
    └── detail/             — Request detail screen
```

## How To Use

1. Install the app on your Android device
2. Tap **Start** to begin capturing traffic
3. Grant VPN permission when prompted
4. Use other apps normally — requests appear in real-time
5. Tap any request to see full details
6. Tap **Stop** to end capture

## CA Certificate (Optional)

For future HTTPS decryption support:

1. Tap **Install CA** in the app
2. Find `network_inspector_ca.crt` in your Downloads folder
3. Go to Settings → Security → Install Certificate
4. Follow the installation steps

## Requirements

- Android 7.0 (API 24) or higher
- No root required
