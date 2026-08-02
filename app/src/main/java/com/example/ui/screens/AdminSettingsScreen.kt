package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TelegramConfig
import com.example.ui.SyncStatusState
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SurfaceGlassCard
import com.example.ui.theme.SurfaceGlassDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.glassmorphism

@Composable
fun AdminSettingsScreen(
    config: TelegramConfig,
    syncStatus: SyncStatusState,
    onSyncTelegram: (String, String) -> Unit,
    onClearSyncStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var botToken by remember(config.botToken) { mutableStateOf(config.botToken) }
    var channelId by remember(config.channelId) { mutableStateOf(config.channelId) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("admin_settings_screen")
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CloudSync,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Telegram Music Backend",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Configure your Telegram Channel storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card Configuration Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism(
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = SurfaceGlassCard.copy(alpha = 0.7f)
                )
                .padding(20.dp)
        ) {
            Text(
                text = "BOT & CHANNEL CREDENTIALS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bot Token Input
            OutlinedTextField(
                value = botToken,
                onValueChange = { botToken = it },
                label = { Text("Telegram Bot Token", color = TextMuted) },
                placeholder = { Text("e.g., 123456789:ABCdefGHIjklMNO...", color = TextMuted) },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Key, contentDescription = null, tint = EmeraldPrimary)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = SurfaceGlassCard,
                    focusedContainerColor = SurfaceGlassDark,
                    unfocusedContainerColor = SurfaceGlassDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bot_token_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Channel ID Input
            OutlinedTextField(
                value = channelId,
                onValueChange = { channelId = it },
                label = { Text("Telegram Channel ID or Username", color = TextMuted) },
                placeholder = { Text("e.g., @my_music_channel or -100123456789", color = TextMuted) },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Send, contentDescription = null, tint = EmeraldPrimary)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = SurfaceGlassCard,
                    focusedContainerColor = SurfaceGlassDark,
                    unfocusedContainerColor = SurfaceGlassDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("channel_id_input")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sync Button
            Button(
                onClick = { onSyncTelegram(botToken, channelId) },
                enabled = syncStatus !is SyncStatusState.Syncing && botToken.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sync_channel_button")
            ) {
                if (syncStatus is SyncStatusState.Syncing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetching Telegram Music...", color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(
                        imageVector = Icons.Filled.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Refresh Library from Telegram",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sync Status Feedback
        when (syncStatus) {
            is SyncStatusState.Success -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphism(shape = RoundedCornerShape(12.dp), backgroundColor = EmeraldPrimary.copy(alpha = 0.2f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Successfully loaded ${syncStatus.count} songs from Telegram channel!",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }
            is SyncStatusState.Error -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphism(shape = RoundedCornerShape(12.dp), backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sync error: ${syncStatus.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Telegram Setup Instructions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism(
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = SurfaceGlassCard.copy(alpha = 0.4f)
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.Info, contentDescription = null, tint = EmeraldLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HOW TELEGRAM BACKEND WORKS",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = EmeraldLight,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val instructions = listOf(
                "1. Create a Telegram Channel for storing your music library.",
                "2. Create a Telegram Bot via @BotFather and copy the API Token.",
                "3. Add your Bot as an Administrator to your Telegram channel.",
                "4. Send audio / MP3 files to your channel with Title, Artist, and Lyrics.",
                "5. Paste your Bot Token above and tap 'Refresh Library'."
            )

            instructions.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(140.dp))
    }
}
