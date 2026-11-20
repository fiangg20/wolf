package com.bases.wol

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeviceEditor(
    initial: Device,
    isNew: Boolean,
    groups: List<String>,
    onDismiss: () -> Unit,
    onSave: (Device) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(initial.name) }
    var mac by remember { mutableStateOf(initial.mac) }
    var host by remember { mutableStateOf(initial.host) }
    var group by remember { mutableStateOf(initial.group) }
    var port by remember { mutableStateOf(initial.port.toString()) }
    var checkPort by remember { mutableStateOf(if (initial.checkPort > 0) initial.checkPort.toString() else "") }
    var broadcast by remember { mutableStateOf(initial.broadcast) }
    var secure by remember { mutableStateOf(initial.secureOn) }
    var notes by remember { mutableStateOf(initial.notes) }
    var icon by remember { mutableIntStateOf(initial.icon) }
    var fav by remember { mutableStateOf(initial.favorite) }
    var tried by remember { mutableStateOf(false) }

    val macOk = Wol.parseMac(mac) != null
    val secureOk = secure.isBlank() || Wol.parseMac(secure) != null

    fun save() {
        tried = true
        if (!macOk || !secureOk) return
        val p = port.toIntOrNull()?.coerceIn(1, 65535) ?: 9
        val cp = checkPort.toIntOrNull()?.coerceIn(0, 65535) ?: 0
        onSave(
            initial.copy(
                name = name.trim().ifBlank { Wol.formatMac(mac) },
                mac = Wol.formatMac(mac),
                broadcast = broadcast.trim(),
                port = p,
                host = host.trim(),
                checkPort = cp,
                group = group.trim(),
                secureOn = if (secure.isBlank()) "" else Wol.formatMac(secure),
                icon = icon,
                favorite = fav,
                notes = notes.trim(),
            )
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (isNew) "New device" else "Edit device",
                style = MaterialTheme.typography.headlineSmall,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DeviceIcons.forEachIndexed { i, vec ->
                    val sel = i == icon
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(if (sel) ScallopShape(8, 0.07f) else CircleShape)
                            .background(if (sel) cs.primary else cs.surfaceContainerHighest)
                            .clickable { icon = i },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(vec, null, tint = if (sel) cs.onPrimary else cs.onSurfaceVariant)
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = mac,
                onValueChange = { mac = it },
                label = { Text("MAC address") },
                placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                singleLine = true,
                isError = (tried || mac.isNotBlank()) && !macOk,
                supportingText = {
                    if ((tried || mac.isNotBlank()) && !macOk) Text("Needs 12 hex digits, like AA:BB:CC:DD:EE:FF")
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host / IP (for status and Open)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = group,
                onValueChange = { group = it },
                label = { Text("Group (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (groups.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groups.forEach { g ->
                        SuggestionChip(onClick = { group = g }, label = { Text(g) })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("WoL port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = checkPort,
                    onValueChange = { checkPort = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Status port") },
                    placeholder = { Text("ping") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = broadcast,
                onValueChange = { broadcast = it },
                label = { Text("Broadcast address (blank = automatic)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = secure,
                onValueChange = { secure = it },
                label = { Text("SecureOn password (optional)") },
                placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                singleLine = true,
                isError = secure.isNotBlank() && !secureOk,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = fav, onCheckedChange = { fav = it })
                Spacer(Modifier.width(12.dp))
                Text("Favorite")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = { save() }, modifier = Modifier.weight(1f)) { Text("Save") }
            }
        }
    }
}
