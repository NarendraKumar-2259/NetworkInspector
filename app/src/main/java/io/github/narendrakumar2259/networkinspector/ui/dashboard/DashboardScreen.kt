package io.github.narendrakumar2259.networkinspector.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.narendrakumar2259.networkinspector.data.model.NetworkRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
    onInstallCert: () -> Unit,
    onRequestClick: (NetworkRequest) -> Unit
) {
    val requests by viewModel.requests.collectAsState()
    val isVpnRunning by viewModel.isVpnRunning.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Network Inspector",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Control buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { if (isVpnRunning) onStopVpn() else onStartVpn() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVpnRunning) Color(0xFFE53935) else Color(0xFF43A047)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isVpnRunning) "Stop" else "Start")
                }

                Button(
                    onClick = onInstallCert,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Install CA", fontSize = 13.sp)
                }

                Button(
                    onClick = { viewModel.clearRequests() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${requests.size} requests captured",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isVpnRunning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF43A047))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Live",
                            color = Color(0xFF43A047),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Request list
            if (requests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No requests captured yet.\nTap Start to begin.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(requests) { request ->
                        RequestItem(
                            request = request,
                            onClick = { onRequestClick(request) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestItem(
    request: NetworkRequest,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Method badge
            val method = request.method ?: if (request.isEncrypted) "TLS" else "TCP"
            val methodColor = when (method) {
                "GET" -> Color(0xFF43A047)
                "POST" -> Color(0xFFFB8C00)
                "PUT" -> Color(0xFF1E88E5)
                "DELETE" -> Color(0xFFE53935)
                "TLS" -> Color(0xFF8E24AA)
                else -> Color(0xFF757575)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(methodColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = method,
                    color = methodColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // URL and destination info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.path ?: request.host ?: "${request.destIp}:${request.destPort}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${request.destIp}:${request.destPort}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Timestamp
            Text(
                text = formatTime(request.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}