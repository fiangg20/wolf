package com.bases.wol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { WolfApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WolfApp() {
    val vm: AppViewModel = viewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val editing by vm.editing.collectAsStateWithLifecycle()
    val editingIsNew by vm.editingIsNew.collectAsStateWithLifecycle()

    val dark = when (settings.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    val activity = LocalContext.current as ComponentActivity
    DisposableEffect(dark) {
        val style = if (dark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        onDispose { }
    }

    WolfTheme(settings = settings, dark = dark) {
        var tab by rememberSaveable { mutableStateOf(0) }
        val snack = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            vm.events.collect { msg ->
                snack.currentSnackbarData?.dismiss()
                launch { snack.showSnackbar(msg) }
            }
        }

        BackHandler(enabled = tab != 0) { tab = 0 }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snack) },
            floatingActionButton = {
                if (tab == 0) {
                    ExtendedFloatingActionButton(
                        onClick = { vm.newDevice() },
                        shape = RoundedCornerShape(24.dp),
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text("Add device") },
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Rounded.Devices, contentDescription = null) },
                        label = { Text("Devices") },
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        label = { Text("Scan") },
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                        label = { Text("Activity") },
                    )
                    NavigationBarItem(
                        selected = tab == 3,
                        onClick = { tab = 3 },
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                    )
                }
            },
        ) { pad ->
            Crossfade(targetState = tab, label = "tabs") { t ->
                when (t) {
                    0 -> DevicesScreen(vm, pad)
                    1 -> ScanScreen(vm, pad)
                    2 -> ActivityScreen(vm, pad)
                    else -> SettingsScreen(vm, pad)
                }
            }
        }

        val e = editing
        if (e != null) {
            key(e.id) {
                DeviceEditor(
                    initial = e,
                    isNew = editingIsNew,
                    groups = devices.map { it.group }.filter { it.isNotBlank() }.distinct().sorted(),
                    onDismiss = { vm.closeEdit() },
                    onSave = { vm.saveDevice(it) },
                )
            }
        }
    }
}
