package com.bases.wol

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import java.util.Calendar

class CardActions(
    val wake: (Device) -> Unit,
    val burst: (Device) -> Unit,
    val waitWake: (Device) -> Unit,
    val stop: (Device) -> Unit,
    val check: (Device) -> Unit,
    val openLink: (Device, String) -> Unit,
    val schedule: (Device) -> Unit,
    val edit: (Device) -> Unit,
    val duplicate: (Device) -> Unit,
    val copyMac: (Device) -> Unit,
    val share: (Device) -> Unit,
    val pin: (Device) -> Unit,
    val delete: (Device) -> Unit,
    val favorite: (Device) -> Unit,
    val move: (Device, Int) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(vm: AppViewModel, pad: PaddingValues) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val devices by vm.devices.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val statuses by vm.statuses.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()

    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmAll by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Device?>(null) }
    var scheduleTarget by remember { mutableStateOf<Device?>(null) }
    var sortMenu by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val groups = devices.map { it.group }.filter { it.isNotBlank() }.distinct().sorted()
    val hasFav = devices.any { it.favorite }
    val effFilter = when {
        filter == FILTER_ALL -> FILTER_ALL
        filter == FILTER_FAV && hasFav -> FILTER_FAV
        groups.contains(filter) -> filter
        else -> FILTER_ALL
    }
    val filtered = when (effFilter) {
        FILTER_ALL -> devices
        FILTER_FAV -> devices.filter { it.favorite }
        else -> devices.filter { it.group == effFilter }
    }
    val visible = when (settings.sort) {
        1 -> filtered.sortedBy { it.name.lowercase() }
        2 -> filtered.sortedWith(compareByDescending<Device> { it.favorite }.thenBy { it.name.lowercase() })
        3 -> filtered.sortedByDescending { it.lastWoken }
        else -> filtered
    }
    val onlineCount = visible.count { statuses[it.id]?.state == DevState.ONLINE }
    val wakeLabel = when (effFilter) {
        FILTER_ALL -> "Wake all"
        FILTER_FAV -> "Wake favorites"
        else -> "Wake $effFilter"
    }

    LaunchedEffect(Unit) {
        if (settings.refreshOnOpen) vm.refreshAll()
    }
    LaunchedEffect(settings.autoRefreshSec) {
        if (settings.autoRefreshSec > 0) {
            while (true) {
                delay(settings.autoRefreshSec * 1000L)
                vm.refreshAll()
            }
        }
    }

    fun buzz() {
        if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val actions = CardActions(
        wake = { buzz(); vm.wake(it) },
        burst = { buzz(); vm.wake(it, settings.burstCount) },
        waitWake = { buzz(); vm.wakeAndWait(it) },
        stop = { vm.stopWait(it) },
        check = { vm.checkStatus(it) },
        openLink = { d, scheme ->
            if (d.host.isBlank()) {
                vm.message("Add a host/IP to this device first")
            } else {
                val ok = openUri(ctx, scheme + d.host.trim())
                if (!ok) vm.message("No app can open $scheme links")
            }
        },
        schedule = { scheduleTarget = it },
        edit = { vm.startEdit(it, false) },
        duplicate = { vm.duplicateDevice(it) },
        copyMac = {
            copyText(ctx, "MAC", Wol.formatMac(it.mac))
            vm.message("MAC copied")
        },
        share = { shareText(ctx, deviceSummary(it)) },
        pin = {
            if (pinShortcut(ctx, it)) vm.message("Shortcut requested, check your home screen")
            else vm.message("This launcher doesn't support pinned shortcuts")
        },
        delete = { deleteTarget = it },
        favorite = { vm.toggleFavorite(it) },
        move = { d, delta -> vm.move(d, delta) },
    )

    Column(Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding())) {
        TopAppBar(
            title = { Text("Devices", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            actions = {
                IconButton(onClick = { vm.refreshAll() }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Check all")
                }
                Box {
                    IconButton(onClick = { sortMenu = true }) {
                        Icon(Icons.Rounded.Tune, contentDescription = "Sort")
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        listOf("Manual order", "Name", "Favorites first", "Recently woken").forEachIndexed { i, label ->
                            DropdownMenuItem(
                                text = { Text(if (settings.sort == i) "✓  $label" else label) },
                                onClick = {
                                    sortMenu = false
                                    vm.updateSettings { it.copy(sort = i) }
                                },
                            )
                        }
                    }
                }
            },
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Hero(
                    online = onlineCount,
                    total = visible.size,
                    wakeLabel = wakeLabel,
                    onWakeAll = {
                        if (settings.confirmWakeAll) confirmAll = true
                        else {
                            buzz()
                            vm.wakeMany(visible)
                        }
                    },
                    onRefresh = { vm.refreshAll() },
                )
            }
            if (groups.isNotEmpty() || hasFav) {
                item {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = effFilter == FILTER_ALL,
                            onClick = { vm.setFilter(FILTER_ALL) },
                            label = { Text("All") },
                        )
                        if (hasFav) {
                            FilterChip(
                                selected = effFilter == FILTER_FAV,
                                onClick = { vm.setFilter(FILTER_FAV) },
                                label = { Text("Favorites") },
                                leadingIcon = { Icon(Icons.Rounded.Star, null, Modifier.size(18.dp)) },
                            )
                        }
                        groups.forEach { g ->
                            FilterChip(
                                selected = effFilter == g,
                                onClick = { vm.setFilter(g) },
                                label = { Text(g) },
                            )
                        }
                    }
                }
            }
            if (visible.isEmpty()) {
                item { EmptyState(onAdd = { vm.newDevice() }) }
            }
            items(visible, key = { it.id }) { d ->
                DeviceCard(
                    d = d,
                    st = statuses[d.id] ?: DeviceStatus(),
                    compact = settings.compactCards,
                    burst = settings.burstCount,
                    manual = settings.sort == 0 && effFilter == FILTER_ALL,
                    expanded = expandedId == d.id,
                    a = actions,
                    onToggle = { expandedId = if (expandedId == d.id) null else d.id },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text("$wakeLabel?") },
            text = { Text("Send a magic packet to ${visible.size} device(s).") },
            confirmButton = {
                TextButton(onClick = {
                    confirmAll = false
                    buzz()
                    vm.wakeMany(visible)
                }) { Text("Wake") }
            },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Cancel") } },
        )
    }

    deleteTarget?.let { d ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${d.name.ifBlank { "device" }}?") },
            text = { Text("Its schedules will be removed too.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    vm.deleteDevice(d)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }

    scheduleTarget?.let { d ->
        ScheduleDialog(
            device = d,
            onDismiss = { scheduleTarget = null },
            onConfirm = { at, daily ->
                scheduleTarget = null
                if (Build.VERSION.SDK_INT >= 33 &&
                    ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                vm.addSchedule(d, at, daily)
            },
        )
    }
}

@Composable
private fun Hero(
    online: Int,
    total: Int,
    wakeLabel: String,
    onWakeAll: () -> Unit,
    onRefresh: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(36.dp), color = cs.primaryContainer) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "$online / $total",
                        style = MaterialTheme.typography.displaySmall,
                        color = cs.onPrimaryContainer,
                    )
                    Text(
                        "devices online",
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onPrimaryContainer,
                    )
                }
                Box(
                    Modifier.size(72.dp).clip(ScallopShape(10, 0.06f)).background(cs.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Bolt, null, tint = cs.onPrimary, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onWakeAll,
                    enabled = total > 0,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(Icons.Rounded.Bolt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(wakeLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(onClick = onRefresh, shape = RoundedCornerShape(20.dp)) {
                    Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Check")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(96.dp).clip(ScallopShape(8, 0.08f)).background(cs.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Bolt, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Nothing to wake yet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Add a device with its MAC address, or scan your network.",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onAdd) {
            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add device")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceCard(
    d: Device,
    st: DeviceStatus,
    compact: Boolean,
    burst: Int,
    manual: Boolean,
    expanded: Boolean,
    a: CardActions,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val waking = st.state == DevState.WAKING
    val blob by animateColorAsState(
        when (st.state) {
            DevState.ONLINE -> cs.primary
            DevState.WAKING -> cs.tertiary
            else -> cs.surfaceContainerHighest
        },
        label = "blob",
    )
    val iconTint = when (st.state) {
        DevState.ONLINE -> cs.onPrimary
        DevState.WAKING -> cs.onTertiary
        else -> cs.onSurfaceVariant
    }
    val inf = rememberInfiniteTransition(label = "spin")
    val rot by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)),
        label = "rot",
    )
    val subtitle = listOf(
        if (d.mac.isBlank()) "" else Wol.formatMac(d.mac),
        d.host,
        d.group,
    ).filter { it.isNotBlank() }.joinToString(" · ")

    Card(
        onClick = onToggle,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .graphicsLayer { rotationZ = if (waking) rot else 0f }
                            .clip(ScallopShape(8, 0.07f))
                            .background(blob)
                    )
                    Icon(DeviceIcons[d.icon.coerceIn(0, DeviceIcons.lastIndex)], null, tint = iconTint)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        d.name.ifBlank { "Unnamed" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (compact) {
                        Spacer(Modifier.height(6.dp))
                        StatusPill(st)
                    }
                }
                IconButton(onClick = { a.favorite(d) }) {
                    Icon(
                        if (d.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (d.favorite) cs.tertiary else cs.onSurfaceVariant,
                    )
                }
                if (compact) {
                    if (waking) {
                        IconButton(onClick = { a.stop(d) }) { Text("Stop", style = MaterialTheme.typography.labelMedium) }
                    } else {
                        FilledIconButton(onClick = { a.wake(d) }) {
                            Icon(Icons.Rounded.Bolt, contentDescription = "Wake")
                        }
                    }
                }
            }
            if (!compact) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(st)
                    Spacer(Modifier.weight(1f))
                    if (waking) {
                        OutlinedButton(onClick = { a.stop(d) }, shape = RoundedCornerShape(20.dp)) { Text("Stop") }
                    } else {
                        Button(onClick = { a.wake(d) }, shape = RoundedCornerShape(20.dp)) {
                            Icon(Icons.Rounded.Bolt, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Wake")
                        }
                    }
                }
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ActionChip(Icons.Rounded.Timer, "Wake & wait") { a.waitWake(d) }
                    ActionChip(Icons.Rounded.Repeat, "Burst ×$burst") { a.burst(d) }
                    ActionChip(Icons.Rounded.Speed, "Check") { a.check(d) }
                    OpenChip(d, a)
                    ActionChip(Icons.Rounded.Schedule, "Schedule") { a.schedule(d) }
                    ActionChip(Icons.Rounded.AddToHomeScreen, "Home screen") { a.pin(d) }
                    ActionChip(Icons.Rounded.Edit, "Edit") { a.edit(d) }
                    ActionChip(Icons.Rounded.ContentCopy, "Copy MAC") { a.copyMac(d) }
                    ActionChip(Icons.Rounded.Share, "Share") { a.share(d) }
                    ActionChip(Icons.Rounded.Add, "Duplicate") { a.duplicate(d) }
                    if (manual) {
                        ActionChip(Icons.Rounded.ArrowUpward, "Move up") { a.move(d, -1) }
                        ActionChip(Icons.Rounded.ArrowDownward, "Move down") { a.move(d, 1) }
                    }
                    ActionChip(Icons.Rounded.Delete, "Delete", danger = true) { a.delete(d) }
                }
                if (d.notes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(d.notes, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
        colors = if (danger) {
            AssistChipDefaults.assistChipColors(
                labelColor = cs.error,
                leadingIconContentColor = cs.error,
            )
        } else {
            AssistChipDefaults.assistChipColors()
        },
    )
}

@Composable
private fun OpenChip(d: Device, a: CardActions) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        ActionChip(Icons.Rounded.Public, "Open") { menuOpen = true }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            listOf(
                "Web (http)" to "http://",
                "Web (https)" to "https://",
                "Remote Desktop" to "rdp://",
                "SSH" to "ssh://",
                "VNC" to "vnc://",
            ).forEach { (label, scheme) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        menuOpen = false
                        a.openLink(d, scheme)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleDialog(
    device: Device,
    onDismiss: () -> Unit,
    onConfirm: (Long, Boolean) -> Unit,
) {
    var mode by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(15) }
    var daily by remember { mutableStateOf(false) }
    val now = remember { Calendar.getInstance() }
    val tp = rememberTimePickerState(
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = now.get(Calendar.MINUTE),
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule wake") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    device.name.ifBlank { "Device" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("In a while") })
                    FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("At a time") })
                }
                if (mode == 0) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(1, 5, 15, 30, 60, 120).forEach { m ->
                            FilterChip(
                                selected = minutes == m,
                                onClick = { minutes = m },
                                label = { Text(if (m < 60) "$m min" else "${m / 60} h") },
                            )
                        }
                    }
                } else {
                    TimeInput(state = tp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = daily, onCheckedChange = { daily = it })
                        Spacer(Modifier.width(12.dp))
                        Text("Repeat every day")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (mode == 0) {
                    onConfirm(System.currentTimeMillis() + minutes * 60_000L, false)
                } else {
                    val c = Calendar.getInstance()
                    c.set(Calendar.HOUR_OF_DAY, tp.hour)
                    c.set(Calendar.MINUTE, tp.minute)
                    c.set(Calendar.SECOND, 0)
                    c.set(Calendar.MILLISECOND, 0)
                    if (c.timeInMillis <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1)
                    onConfirm(c.timeInMillis, daily)
                }
            }) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
