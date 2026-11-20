package com.bases.wol

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel, pad: PaddingValues) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val s by vm.settings.collectAsStateWithLifecycle()
    val canDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding())) {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ---------- Look ----------
            item { SectionTitle("Look") }
            item {
                Group {
                    ChoiceRow(
                        title = "Theme",
                        options = listOf("System", "Light", "Dark"),
                        selected = s.themeMode,
                        onSelect = { i -> vm.updateSettings { it.copy(themeMode = i) } },
                    )
                    if (canDynamic) {
                        SwitchRow(
                            title = "Dynamic color",
                            subtitle = "Use your wallpaper colors",
                            checked = s.dynamicColor,
                            onChange = { v -> vm.updateSettings { it.copy(dynamicColor = v) } },
                        )
                    }
                    if (!(canDynamic && s.dynamicColor)) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text("Accent", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Palettes.forEachIndexed { i, p ->
                                    val sel = s.palette == i
                                    Box(
                                        Modifier
                                            .size(44.dp)
                                            .background(p.seed, CircleShape)
                                            .border(if (sel) 3.dp else 0.dp, cs.onSurface, CircleShape)
                                            .clickable { vm.updateSettings { it.copy(palette = i) } },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (sel) Icon(Icons.Rounded.Check, null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    SwitchRow(
                        title = "Pure black in dark mode",
                        subtitle = "Saves battery on OLED screens",
                        checked = s.pureBlack,
                        onChange = { v -> vm.updateSettings { it.copy(pureBlack = v) } },
                    )
                    SwitchRow(
                        title = "Compact cards",
                        subtitle = "Smaller device cards with a quick wake button",
                        checked = s.compactCards,
                        onChange = { v -> vm.updateSettings { it.copy(compactCards = v) } },
                    )
                    ChoiceRow(
                        title = "Sort devices",
                        options = listOf("Manual", "Name", "Favorites", "Recent"),
                        selected = s.sort,
                        onSelect = { i -> vm.updateSettings { it.copy(sort = i) } },
                    )
                }
            }

            // ---------- Magic packet ----------
            item { SectionTitle("Magic packet") }
            item {
                Group {
                    SliderRow(
                        title = "Packets per wake",
                        value = s.packetCount,
                        range = 1..10,
                        unit = "",
                        onDone = { v -> vm.updateSettings { it.copy(packetCount = v) } },
                    )
                    SliderRow(
                        title = "Delay between packets",
                        value = s.packetDelayMs,
                        range = 0..1000,
                        unit = " ms",
                        onDone = { v -> vm.updateSettings { it.copy(packetDelayMs = v) } },
                    )
                    SliderRow(
                        title = "Burst size",
                        value = s.burstCount,
                        range = 5..50,
                        unit = " packets",
                        onDone = { v -> vm.updateSettings { it.copy(burstCount = v) } },
                    )
                    SwitchRow(
                        title = "Also send to port 7",
                        subtitle = "Some network cards listen on 7 instead of 9",
                        checked = s.alsoPort7,
                        onChange = { v -> vm.updateSettings { it.copy(alsoPort7 = v) } },
                    )
                    SwitchRow(
                        title = "Automatic broadcast address",
                        subtitle = "Use your Wi-Fi subnet instead of 255.255.255.255",
                        checked = s.autoBroadcast,
                        onChange = { v -> vm.updateSettings { it.copy(autoBroadcast = v) } },
                    )
                    TextRow(
                        title = "Default broadcast address",
                        hint = "blank = automatic",
                        initial = s.defaultBroadcast,
                        number = false,
                        onChange = { t -> vm.updateSettings { it.copy(defaultBroadcast = t.trim()) } },
                    )
                    TextRow(
                        title = "Default port for new devices",
                        hint = "9",
                        initial = s.defaultPort.toString(),
                        number = true,
                        onChange = { t ->
                            val p = t.toIntOrNull()
                            if (p != null && p in 1..65535) vm.updateSettings { it.copy(defaultPort = p) }
                        },
                    )
                }
            }

            // ---------- Status ----------
            item { SectionTitle("Status checks") }
            item {
                Group {
                    SwitchRow(
                        title = "Check status when opening the app",
                        subtitle = null,
                        checked = s.refreshOnOpen,
                        onChange = { v -> vm.updateSettings { it.copy(refreshOnOpen = v) } },
                    )
                    ChoiceRow(
                        title = "Auto-refresh while open",
                        options = listOf("Off", "15 s", "30 s", "1 min", "5 min"),
                        selected = when (s.autoRefreshSec) {
                            15 -> 1
                            30 -> 2
                            60 -> 3
                            300 -> 4
                            else -> 0
                        },
                        onSelect = { i ->
                            val sec = intArrayOf(0, 15, 30, 60, 300)[i]
                            vm.updateSettings { it.copy(autoRefreshSec = sec) }
                        },
                    )
                    SliderRow(
                        title = "Ping timeout",
                        value = s.pingTimeoutMs,
                        range = 300..5000,
                        unit = " ms",
                        onDone = { v -> vm.updateSettings { it.copy(pingTimeoutMs = v) } },
                    )
                    SliderRow(
                        title = "Wake & wait timeout",
                        value = s.waitTimeoutSec,
                        range = 30..600,
                        unit = " s",
                        onDone = { v -> vm.updateSettings { it.copy(waitTimeoutSec = v) } },
                    )
                }
            }

            // ---------- Behavior ----------
            item { SectionTitle("Behavior") }
            item {
                Group {
                    SwitchRow(
                        title = "Haptic feedback",
                        subtitle = "Buzz when waking a device",
                        checked = s.haptics,
                        onChange = { v -> vm.updateSettings { it.copy(haptics = v) } },
                    )
                    SwitchRow(
                        title = "Confirm before waking many",
                        subtitle = "Ask first on the big Wake all button",
                        checked = s.confirmWakeAll,
                        onChange = { v -> vm.updateSettings { it.copy(confirmWakeAll = v) } },
                    )
                    SliderRow(
                        title = "History size",
                        value = s.maxLog,
                        range = 20..500,
                        unit = " entries",
                        onDone = { v -> vm.updateSettings { it.copy(maxLog = v) } },
                    )
                }
            }

            // ---------- Backup ----------
            item { SectionTitle("Backup") }
            item {
                Group {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Your devices, as text you can keep anywhere.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(onClick = {
                                copyText(ctx, "Wolf backup", vm.exportJson())
                                vm.message("Backup copied to clipboard")
                            }) {
                                Icon(Icons.Rounded.FileUpload, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Copy")
                            }
                            FilledTonalButton(onClick = { shareText(ctx, vm.exportJson()) }) {
                                Icon(Icons.Rounded.Share, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Share")
                            }
                            OutlinedButton(onClick = { vm.importJson(readClipboard(ctx), false) }) {
                                Icon(Icons.Rounded.FileDownload, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Import (merge)")
                            }
                            OutlinedButton(onClick = { vm.importJson(readClipboard(ctx), true) }) {
                                Text("Import (replace)")
                            }
                        }
                    }
                }
            }

            // ---------- About ----------
            item { SectionTitle("About") }
            item {
                Group {
                    Column(Modifier.padding(16.dp)) {
                        Text("Wolf by Bases", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Version 1.0 · Wake-on-LAN controller",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) { content() }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceRow(
    title: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { i, label ->
                FilterChip(selected = selected == i, onClick = { onSelect(i) }, label = { Text(label) })
            }
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Int,
    range: IntRange,
    unit: String,
    onDone: (Int) -> Unit,
) {
    var v by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                "${v.roundToInt()}$unit",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = v,
            onValueChange = { v = it },
            onValueChangeFinished = { onDone(v.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
}

@Composable
private fun TextRow(
    title: String,
    hint: String,
    initial: String,
    number: Boolean,
    onChange: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onChange(it)
            },
            label = { Text(title) },
            placeholder = { Text(hint) },
            singleLine = true,
            keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
