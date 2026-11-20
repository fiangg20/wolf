package com.bases.wol

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(vm: AppViewModel, pad: PaddingValues) {
    val cs = MaterialTheme.colorScheme
    val st by vm.scan.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding())) {
        TopAppBar(
            title = { Text("Scan network", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(shape = RoundedCornerShape(36.dp), color = cs.secondaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Find devices",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = cs.onSecondaryContainer,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    st.network?.let { "Your network: $it" } ?: "Pings every address on your Wi-Fi subnet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cs.onSecondaryContainer,
                                )
                            }
                            Box(
                                Modifier.size(64.dp).clip(ScallopShape(9, 0.07f)).background(cs.secondary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Wifi, null, tint = cs.onSecondary, modifier = Modifier.size(30.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        if (st.running) {
                            val p = if (st.total > 0) st.done.toFloat() / st.total.toFloat() else 0f
                            LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Checked ${st.done} of ${st.total} · ${st.found.size} found",
                                style = MaterialTheme.typography.labelLarge,
                                color = cs.onSecondaryContainer,
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { vm.stopScan() }, shape = RoundedCornerShape(20.dp)) {
                                Icon(Icons.Rounded.Close, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Stop")
                            }
                        } else {
                            Button(onClick = { vm.startScan() }, shape = RoundedCornerShape(20.dp)) {
                                Icon(Icons.Rounded.Search, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (st.finished) "Scan again" else "Start scan")
                            }
                        }
                        if (st.error != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(st.error ?: "", color = cs.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (st.finished && st.found.isEmpty()) {
                item {
                    Text(
                        "Nothing answered. Some devices ignore pings while asleep, which is exactly when you need to wake them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
            if (st.found.isNotEmpty()) {
                item { SectionTitle("Found on your network") }
            }
            items(st.found, key = { it.ip }) { h ->
                val added = devices.any { it.host == h.ip }
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(48.dp).clip(ScallopShape(8, 0.07f)).background(cs.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Devices, null, tint = cs.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.ip, style = MaterialTheme.typography.titleMedium)
                            val sub = listOf(h.name, h.mac, "${h.latency} ms").filter { it.isNotBlank() }.joinToString(" · ")
                            Text(
                                sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                vm.startEdit(
                                    Device(
                                        name = h.name.substringBefore('.').ifBlank { "Device ${h.ip.substringAfterLast('.')}" },
                                        mac = h.mac,
                                        host = h.ip,
                                        port = settings.defaultPort,
                                    ),
                                    true,
                                )
                            },
                            enabled = !added,
                        ) { Text(if (added) "Added" else "Add") }
                    }
                }
            }
            if (st.finished && st.found.isNotEmpty()) {
                item {
                    Text(
                        "Android 10+ hides other devices' MAC addresses from apps, so you may need to type the MAC yourself. Find it in your router's client list or on the device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}
