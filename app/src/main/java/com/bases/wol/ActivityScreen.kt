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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(vm: AppViewModel, pad: PaddingValues) {
    val cs = MaterialTheme.colorScheme
    val log by vm.log.collectAsStateWithLifecycle()
    val schedules by vm.schedules.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val fmt = remember { SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()) }
    val fmtLog = remember { SimpleDateFormat("d MMM · HH:mm:ss", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding())) {
        TopAppBar(
            title = { Text("Activity", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            actions = {
                if (log.isNotEmpty()) {
                    IconButton(onClick = { vm.clearLog() }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear history")
                    }
                }
            },
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (schedules.isNotEmpty()) {
                item { SectionTitle("Scheduled") }
                items(schedules.sortedBy { it.triggerAt }, key = { it.id }) { s ->
                    val dev = devices.firstOrNull { it.id == s.deviceId }
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Alarm, null, tint = cs.onTertiaryContainer)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    dev?.name?.ifBlank { "Device" } ?: "Deleted device",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = cs.onTertiaryContainer,
                                )
                                Text(
                                    fmt.format(Date(s.triggerAt)) + if (s.repeatDaily) " · every day" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onTertiaryContainer,
                                )
                            }
                            IconButton(onClick = { vm.cancelSchedule(s.id) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Cancel schedule", tint = cs.onTertiaryContainer)
                            }
                        }
                    }
                }
            }

            item { SectionTitle("History") }
            if (log.isEmpty()) {
                item {
                    Text(
                        "Wake attempts will show up here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
            items(log) { e ->
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(if (e.ok) cs.primaryContainer else cs.errorContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (e.ok) Icons.Rounded.Check else Icons.Rounded.Close,
                                null,
                                tint = if (e.ok) cs.onPrimaryContainer else cs.onErrorContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                e.action + if (e.device.isNotBlank()) " · ${e.device}" else "",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                e.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                            Text(
                                fmtLog.format(Date(e.time)),
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.outline,
                            )
                        }
                    }
                }
            }
        }
    }
}
