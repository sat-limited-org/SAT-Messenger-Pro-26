package com.example.ui.screens.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar

@Composable
fun NewChatDialog(
    users: List<UserProfile>,
    onDismiss: () -> Unit,
    onSelectUser: (UserProfile) -> Unit,
    onCreateGroup: (String, List<String>) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    val selectedGroupMembers = remember { mutableStateListOf<String>() }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true) ||
                    (it.statusMessage?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 0) "New Conversation" else "Create Group Chat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Direct Message") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("New Group") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 1) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        placeholder = { Text("e.g. SAT Operations, DevOps Team") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_name_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search SAT colleagues...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("colleague_search_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    if (filteredUsers.isEmpty()) {
                        item {
                            Text(
                                text = "No colleagues found in directory.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(filteredUsers, key = { it.userId }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedTab == 0) {
                                            onSelectUser(user)
                                        } else {
                                            if (selectedGroupMembers.contains(user.userId)) {
                                                selectedGroupMembers.remove(user.userId)
                                            } else {
                                                selectedGroupMembers.add(user.userId)
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    name = user.displayName,
                                    avatarUrl = user.avatarUrl,
                                    isOnline = user.isOnline,
                                    size = 40.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = user.statusMessage ?: user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selectedTab == 1) {
                                    Checkbox(
                                        checked = selectedGroupMembers.contains(user.userId),
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                selectedGroupMembers.add(user.userId)
                                            } else {
                                                selectedGroupMembers.remove(user.userId)
                                            }
                                        }
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }

                if (selectedTab == 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onCreateGroup(groupName, selectedGroupMembers.toList())
                        },
                        enabled = groupName.isNotBlank() && selectedGroupMembers.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_group_button")
                    ) {
                        Text("Create Group (${selectedGroupMembers.size} selected)")
                    }
                }
            }
        }
    }
}
