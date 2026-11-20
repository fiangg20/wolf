package com.bases.wol

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.compose.foundation.background

val DeviceIcons: List<ImageVector> = listOf(
    Icons.Rounded.Computer,
    Icons.Rounded.Laptop,
    Icons.Rounded.Dns,
    Icons.Rounded.Storage,
    Icons.Rounded.Tv,
    Icons.Rounded.SportsEsports,
    Icons.Rounded.Router,
    Icons.Rounded.Memory,
)

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 8.dp, top = 12.dp, bottom = 2.dp),
    )
}

@Composable
fun StatusPill(st: DeviceStatus) {
    val cs = MaterialTheme.colorScheme
    val container = when (st.state) {
        DevState.ONLINE -> cs.primaryContainer
        DevState.WAKING -> cs.tertiaryContainer
        else -> cs.surfaceContainerHighest
    }
    val content = when (st.state) {
        DevState.ONLINE -> cs.onPrimaryContainer
        DevState.WAKING -> cs.onTertiaryContainer
        else -> cs.onSurfaceVariant
    }
    val dot = when (st.state) {
        DevState.ONLINE -> cs.primary
        DevState.WAKING -> cs.tertiary
        DevState.OFFLINE -> cs.error
        else -> cs.outline
    }
    val label = when (st.state) {
        DevState.ONLINE -> if (st.latency >= 0) "Online · ${st.latency} ms" else "Online"
        DevState.OFFLINE -> "Offline"
        DevState.WAKING -> "Waking · ${st.waited}s"
        DevState.CHECKING -> "Checking…"
        DevState.UNKNOWN -> "Not checked"
    }
    Surface(shape = CircleShape, color = container) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}

fun copyText(ctx: Context, label: String, text: String) {
    val cm = ctx.getSystemService(ClipboardManager::class.java) ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun readClipboard(ctx: Context): String {
    val cm = ctx.getSystemService(ClipboardManager::class.java) ?: return ""
    val clip = cm.primaryClip ?: return ""
    if (clip.itemCount == 0) return ""
    return clip.getItemAt(0).coerceToText(ctx)?.toString() ?: ""
}

fun shareText(ctx: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    val chooser = Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        ctx.startActivity(chooser)
    } catch (e: Exception) {
        // ignore
    }
}

fun openUri(ctx: Context, uri: String): Boolean {
    return try {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        true
    } catch (e: Exception) {
        false
    }
}

fun pinShortcut(ctx: Context, d: Device): Boolean {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)) return false
    val intent = Intent(ctx, WakeActivity::class.java)
        .setAction(Intent.ACTION_VIEW)
        .putExtra("id", d.id)
    val label = d.name.ifBlank { "Wake" }
    val info = ShortcutInfoCompat.Builder(ctx, "wake_" + d.id)
        .setShortLabel(label)
        .setLongLabel("Wake $label")
        .setIcon(IconCompat.createWithResource(ctx, R.mipmap.ic_launcher))
        .setIntent(intent)
        .build()
    return ShortcutManagerCompat.requestPinShortcut(ctx, info, null)
}

fun deviceSummary(d: Device): String {
    val sb = StringBuilder()
    sb.append(d.name.ifBlank { "Device" }).append('\n')
    sb.append("MAC: ").append(Wol.formatMac(d.mac))
    if (d.host.isNotBlank()) sb.append("\nHost: ").append(d.host)
    if (d.broadcast.isNotBlank()) sb.append("\nBroadcast: ").append(d.broadcast)
    sb.append("\nPort: ").append(d.port)
    return sb.toString()
}
