package com.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.context.ConversationContext
import com.jarvis.ai.orchestrator.AssistantOrchestrator
import com.jarvis.tools.ToolExecutor
import com.jarvis.ui.components.ConfirmationDialog
import com.jarvis.ui.theme.JarvisGradientEnd
import com.jarvis.ui.theme.JarvisGradientStart
import com.jarvis.ui.theme.JarvisGlass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(val role: String, val text: String)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val orchestrator: AssistantOrchestrator,
    private val toolExecutor: ToolExecutor,
    private val context: ConversationContext
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("assistant", "Hello. I'm JARVIS. Ask me to set a reminder, take a note, open an app, or say 'help'."))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<String?>(null)
    val pendingConfirmation: StateFlow<String?> = _pendingConfirmation.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    init {
        // Route tool confirmations through a dialog. The handler suspends the
        // caller until the UI resolves it (or the ViewModel is cleared).
        toolExecutor.confirmationHandler = { description ->
            _pendingConfirmation.value = description
            suspendCancellableCoroutine { continuation ->
                pendingResolution = continuation
                continuation.invokeOnCancellation { pendingResolution = null }
            }
        }
    }

    private var pendingResolution: CancellableContinuation<Boolean>? = null

    override fun onCleared() {
        // Never leave a tool execution suspended on a dead confirmation handler.
        toolExecutor.confirmationHandler = null
        pendingResolution?.cancel()
        pendingResolution = null
        super.onCleared()
    }

    fun resolveConfirmation(approved: Boolean) {
        _pendingConfirmation.value = null
        val continuation = pendingResolution
        pendingResolution = null
        if (continuation?.isActive == true) continuation.resumeWith(Result.success(approved))
    }

    fun send(text: String) {
        if (text.isBlank() || _isSending.value) return
        _messages.value = _messages.value + ChatMessage("user", text)
        _isSending.value = true
        viewModelScope.launch {
            val response = orchestrator.handleUserText(text)
            _messages.value = _messages.value + ChatMessage("assistant", response.spoken)
            _isSending.value = false
        }
    }
}

@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    var input by remember { mutableStateOf("") }

    pendingConfirmation?.let { description ->
        ConfirmationDialog(
            description = description,
            onConfirm = { viewModel.resolveConfirmation(true) },
            onDismiss = { viewModel.resolveConfirmation(false) }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
            if (isSending) {
                item { Text("Thinking…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                label = { Text("Message JARVIS") },
                singleLine = true
            )
            IconButton(
                onClick = {
                    viewModel.send(input)
                    input = ""
                }
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isUser) 18.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 18.dp
    )
    val bubbleModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = if (isUser) 48.dp else 0.dp)
        .shadow(6.dp, shape)
        .clip(shape)
    if (isUser) {
        Box(
            modifier = bubbleModifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(JarvisGradientStart, JarvisGradientEnd)
                    )
                )
                .padding(12.dp)
        ) {
            Text(text = message.text, color = Color.White)
        }
    } else {
        Box(
            modifier = bubbleModifier
                .background(JarvisGlass)
                .border(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            JarvisGradientStart.copy(alpha = 0.4f),
                            JarvisGradientEnd.copy(alpha = 0.3f)
                        )
                    ),
                    shape
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
