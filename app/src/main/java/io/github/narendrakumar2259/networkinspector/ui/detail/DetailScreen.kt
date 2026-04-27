package io.github.narendrakumar2259.networkinspector.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.narendrakumar2259.networkinspector.data.model.NetworkRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    request: NetworkRequest,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
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
                .verticalScroll(rememberScrollState())
        ) {
            // Connection info
            DetailSection("Connection") {
                DetailRow("Source", "${request.sourceIp}:${request.sourcePort}")
                DetailRow("Destination", "${request.destIp}:${request.destPort}")
                DetailRow("Encrypted", if (request.isEncrypted) "Yes (HTTPS)" else "No (HTTP)")
                DetailRow("Time", formatTimestamp(request.timestamp))
                DetailRow("State", request.tcpState)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // HTTP info (if available)
            if (request.method != null) {
                DetailSection("HTTP Request") {
                    DetailRow("Method", request.method)
                    request.path?.let { DetailRow("Path", it) }
                    request.host?.let { DetailRow("Host", it) }
                    DetailRow("Request Size", "${request.requestSize} bytes")
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Headers (if available)
            if (!request.headers.isNullOrEmpty()) {
                DetailSection("Headers") {
                    Text(
                        text = request.headers,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Body (if available)
            if (!request.requestBody.isNullOrEmpty()) {
                DetailSection("Request Body") {
                    Text(
                        text = request.requestBody,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Response info
            DetailSection("Response") {
                request.responseCode?.let { DetailRow("Status Code", it.toString()) }
                DetailRow("Response Size", "${request.responseSize} bytes")
            }
        }
    }
}

@Composable
fun DetailSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    return sdf.format(Date(timestamp))
}