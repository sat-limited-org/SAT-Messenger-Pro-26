package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal

@Composable
fun UserAvatar(
    name: String,
    avatarUrl: String? = null,
    isOnline: Boolean = false,
    isGroup: Boolean = false,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            val initials = if (isGroup) "" else {
                name.trim().split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase()
                    .ifEmpty { "U" }
            }
            val bgColor = if (isGroup) SatTeal else SatElectricBlue

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                if (isGroup) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Group",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.55f)
                    )
                } else {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.4f).sp
                    )
                }
            }
        }

        if (isOnline && !isGroup) {
            val badgeSize = (size * 0.28f).coerceAtLeast(10.dp)
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .align(Alignment.BottomEnd)
                    .background(SatOnlineGreen, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun MessageStatusIndicator(
    status: String,
    isOutgoing: Boolean,
    isRead: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!isOutgoing) return

    val effectivelyRead = isRead || status == "read"
    val icon = when {
        effectivelyRead -> Icons.Default.DoneAll
        status == "delivered" -> Icons.Default.DoneAll
        else -> Icons.Default.Done
    }
    val tint = when {
        effectivelyRead -> SatTeal
        status == "delivered" -> Color.White.copy(alpha = 0.85f)
        else -> Color.White.copy(alpha = 0.6f)
    }

    Icon(
        imageVector = icon,
        contentDescription = if (effectivelyRead) "Read" else "Status $status",
        tint = tint,
        modifier = modifier.size(14.dp)
    )
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("empty_state_action_button")
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Notice",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
