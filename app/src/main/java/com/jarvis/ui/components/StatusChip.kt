package com.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jarvis.ui.theme.JarvisSuccess
import com.jarvis.ui.theme.JarvisWarning

/**
 * Small status indicator chip (offline/online, listening, etc.).
 */
@Composable
fun StatusChip(
    label: String,
    icon: ImageVector,
    online: Boolean,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = {},
        modifier = modifier,
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        trailingIcon = {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (online) JarvisSuccess else JarvisWarning)
            )
        },
        label = { Text(label, color = MaterialTheme.colorScheme.onSurface) }
    )
}
