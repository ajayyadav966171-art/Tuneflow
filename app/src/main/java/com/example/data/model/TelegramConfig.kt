package com.example.data.model

data class TelegramConfig(
    val botToken: String = "",
    val channelId: String = "",
    val lastSyncedTimestamp: Long = 0L,
    val autoSyncEnabled: Boolean = true
)
