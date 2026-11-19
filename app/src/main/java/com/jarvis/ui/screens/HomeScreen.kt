package com.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.core.network.ConnectivityMonitor
import com.jarvis.core.network.ConnectivityState
import com.jarvis.data.datastore.SettingsRepository
import com.jarvis.ui.components.CoreState
import com.jarvis.ui.components.JarvisCore
import com.jarvis.ui.components.StatusChip
import com.jarvis.ui.theme.JarvisGradientEnd
import com.jarvis.ui.theme.JarvisGradientStart
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
        // Gradient title.
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(JarvisGradientStart, JarvisGradientEnd)
                        )
                    )
                ) {
                    append("JARVIS")
                }
            },
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Your private, offline-first assistant.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        // 3D core orb — drag to tilt.
        JarvisCore(state = CoreState.IDLE)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Drag the core to tilt",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        StatusChip(
            label = if (isOnline) "Online" else "Offline",
            icon = if (isOnline) Icons.Filled.Cloud else Icons.Filled.CloudOff,
            online = isOnline
        )
        Spacer(Modifier.height(24.dp))
        // Primary action: gradient button with depth shadow.
        Button(
            onClick = onOpenVoice,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(JarvisGradientStart, JarvisGradientEnd)
                        )
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Mic, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Talk to JARVIS")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenChat,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(JarvisGradientStart, JarvisGradientEnd)
                    ),
                    RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Type a message")
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
