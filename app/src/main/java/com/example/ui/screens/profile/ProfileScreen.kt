package com.example.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockedUser
import com.example.data.model.PrivacySettings
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatErrorRed
import com.example.ui.theme.SatTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    privacySettings: PrivacySettings,
    blockedUsers: List<BlockedUser>,
    onBack: () -> Unit,
    onUpdateProfile: (name: String, status: String) -> Unit,
    onUpdatePrivacy: (readReceipts: Boolean, lastSeenVisible: Boolean) -> Unit,
    onUnblockUser: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var displayName by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: "") }
    var statusMessage by remember(userProfile) { mutableStateOf(userProfile?.statusMessage ?: "") }
    var readReceipts by remember(privacySettings) { mutableStateOf(privacySettings.readReceipts) }
    var lastSeenVisible by remember(privacySettings) { mutableStateOf(privacySettings.lastSeenVisible) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(
                        text = "My Profile & Security",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Avatar and Identity Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserAvatar(
                        name = displayName.ifBlank { "User" },
                        avatarUrl = userProfile?.avatarUrl,
                        isOnline = true,
                        size = 84.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = displayName.ifBlank { "SAT Colleague" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = userProfile?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Profile Details Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = SatElectricBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Profile Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("display_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = statusMessage,
                            onValueChange = { statusMessage = it },
                            label = { Text("Corporate Status / Title") },
                            placeholder = { Text("e.g. Senior Network Specialist") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("status_message_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onUpdateProfile(displayName, statusMessage) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_profile_button")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Profile")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Privacy Settings Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SatTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Privacy Controls",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Read Receipts",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Allow others to see when you have read their messages.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = readReceipts,
                                onCheckedChange = {
                                    readReceipts = it
                                    onUpdatePrivacy(it, lastSeenVisible)
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Online & Last Seen Presence",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Display real-time active status to colleagues in chat.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = lastSeenVisible,
                                onCheckedChange = {
                                    lastSeenVisible = it
                                    onUpdatePrivacy(readReceipts, it)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Blocked Users Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Blocked Contacts (${blockedUsers.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (blockedUsers.isEmpty()) {
                            Text(
                                text = "No blocked contacts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            blockedUsers.forEach { blocked ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = blocked.blockedUserName.ifBlank { blocked.blockedUserId },
                                        fontWeight = FontWeight.Medium
                                    )
                                    OutlinedButton(
                                        onClick = { onUnblockUser(blocked.blockedUserId) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Unblock")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Account Actions
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out from SAT Messenger")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SatErrorRed.copy(alpha = 0.15f), contentColor = SatErrorRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_account_button")
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account & Clear Profile")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete SAT Messenger Account?") },
                text = {
                    Text("This will permanently remove your profile, presence, and personal privacy settings from SAT Limited Cloud Firestore. This operation cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteAccount()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SatErrorRed)
                    ) {
                        Text("Delete Account")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
