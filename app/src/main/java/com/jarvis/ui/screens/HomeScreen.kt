package com.jarvis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.core.network.ConnectivityMonitor
import com.jarvis.core.network.ConnectivityState
import com.jarvis.data.datastore.SettingsRepository
import com.jarvis.ui.components.AudioVisualizer
import com.jarvis.ui.components.StatusChip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val connectivity: ConnectivityMonitor
) : ViewModel() {

    data class UiState(
        val offlineMode: Boolean = true,
        val onlineModeEnabled: Boolean = false,
        val connectivity: ConnectivityState = ConnectivityState.Offline
    )

    val uiState: StateFlow<UiState> = combine(
        settings.offlineMode,
        settings.onlineModeEnabled,
        connectivity.state
    ) { offline, onlineEnabled, conn ->
        UiState(offline, onlineEnabled, conn)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun setOfflineMode(value: Boolean) {
        viewModelScope.launch { settings.setOfflineMode(value) }
    }
}

@Composable
fun HomeScreen(
    onOpenChat: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isOnline = !state.offlineMode && state.onlineModeEnabled &&
        state.connectivity == ConnectivityState.Online

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "JARVIS",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your private, offline-first assistant.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        AudioVisualizer(active = false)
        Spacer(Modifier.height(24.dp))
        StatusChip(
            label = if (isOnline) "Online" else "Offline",
            icon = if (isOnline) Icons.Filled.Cloud else Icons.Filled.CloudOff,
            online = isOnline
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onOpenVoice, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Mic, contentDescription = null)
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text("Talk to JARVIS")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenChat, modifier = Modifier.fillMaxWidth()) {
            Text("Type a message")
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (state.offlineMode) "Offline mode: ON (default)" else "Offline mode: OFF",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Text("Settings")
            }
        }
    }
}
