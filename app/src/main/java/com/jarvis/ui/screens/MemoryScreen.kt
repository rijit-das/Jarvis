package com.jarvis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.core.common.AppResult
import com.jarvis.data.local.MemoryEntity
import com.jarvis.memory.MemoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryManager: MemoryManager
) : ViewModel() {

    val memories: StateFlow<List<MemoryEntity>> = memoryManager.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = mutableStateOf<String?>(null)
    val message: androidx.compose.runtime.State<String?> = _message

    fun save(category: String, key: String, value: String) {
        viewModelScope.launch {
            val result = memoryManager.save(category, key, value)
            _message.value = when (result) {
                is AppResult.Success -> "Saved."
                is AppResult.Failure -> result.message
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            val result = memoryManager.delete(id)
            _message.value = when (result) {
                is AppResult.Success -> "Deleted."
                is AppResult.Failure -> result.message
            }
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            val result = memoryManager.deleteAll()
            _message.value = when (result) {
                is AppResult.Success -> "All memories cleared."
                is AppResult.Failure -> result.message
            }
        }
    }

    suspend fun exportJson(): String = memoryManager.exportJson()
}

@Composable
fun MemoryScreen(viewModel: MemoryViewModel = hiltViewModel()) {
    val memories by viewModel.memories.collectAsState()
    val message by viewModel.message
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var exportedJson by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add memory")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Memory", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "You control everything JARVIS remembers. Nothing is stored without your explicit action. " +
                    "Data is encrypted on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        exportedJson = viewModel.exportJson()
                    }
                }) { Text("Export JSON") }
                Button(onClick = { showClearDialog = true }) { Text("Clear all") }
            }
            exportedJson?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(memories) { memory ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(memory.key, style = MaterialTheme.typography.titleMedium)
                                Text(memory.value, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    memory.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.delete(memory.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var key by remember { mutableStateOf("") }
        var value by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add memory") },
            text = {
                Column {
                    OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Key") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Value") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.save("FACT", key, value)
                    showAddDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all memories?") },
            text = { Text("This permanently deletes every stored memory. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAll()
                    showClearDialog = false
                }) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}
