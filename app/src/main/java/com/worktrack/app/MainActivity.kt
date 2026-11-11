package com.worktrack.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifier.ensureChannels(this)
        Alarms.restoreAll(this)
        setContent { WorkTrackTheme { App() } }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var tick by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) }

    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            Store.checkCompletion(ctx, now)
            delay(1000)
        }
    }

    val s = remember(now, tick) { Store.snapshot(ctx, now) }

    val toggle = {
        val t = System.currentTimeMillis()
        if (s.running) {
            Store.logout(ctx, t)
            Alarms.cancelCompletion(ctx)
        } else {
            Store.login(ctx, t)
            Alarms.scheduleCompletion(ctx)
        }
        tick++
    }

    Scaffold(
        containerColor = Navy,
        bottomBar = {
            NavigationBar(containerColor = Card) {
                val colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Navy, selectedTextColor = Mint, indicatorColor = Mint,
                    unselectedIconColor = Muted, unselectedTextColor = Muted
                )
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, colors = colors,
                    icon = { Icon(Icons.Default.Home, null) }, label = { Text("Today") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, colors = colors,
                    icon = { Icon(Icons.Default.List, null) }, label = { Text("History") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, colors = colors,
                    icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        }
    ) { inner ->
        val m = Modifier.padding(inner)
        when (tab) {
            0 -> Dashboard(ctx, s, now, m, toggle)
            1 -> HistoryScreen(ctx, s, now, m)
            else -> SettingsScreen(ctx, m, tick) { tick++ }
        }
    }
}
