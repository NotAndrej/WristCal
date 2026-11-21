package com.notandrej.wristcal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.notandrej.wristcal.ui.AgendaScreen
import com.notandrej.wristcal.ui.theme.WristCalTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {

    // Bumped on every resume so the agenda re-reads the provider after the user
    // has been on the watch face or in system settings.
    private val resumeTick = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WristCalTheme {
                WristCalApp(resumeTick)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.value += 1
    }
}

@Composable
private fun WristCalApp(resumeTick: StateFlow<Int>) {
    val context = LocalContext.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result -> granted = result }

    LaunchedEffect(granted) {
        if (!granted) launcher.launch(Manifest.permission.READ_CALENDAR)
    }

    if (granted) {
        AgendaScreen(resumeTick)
    } else {
        PermissionScreen(onRequest = { launcher.launch(Manifest.permission.READ_CALENDAR) })
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colors.primary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "WristCal needs access to your calendar",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.title3,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRequest) {
            Text("Grant access")
        }
    }
}
