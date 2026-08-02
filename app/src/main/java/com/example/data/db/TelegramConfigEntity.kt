package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.TelegramConfig

@Entity(tableName = "telegram_config")
data class TelegramConfigEntity(
    @PrimaryKey val id: Int = 1,
    val botToken: String,
    val channelId: String,
    val lastSyncedTimestamp: Long,
    val autoSyncEnabled: Boolean
) {
    fun toConfig(): TelegramConfig = TelegramConfig(
        botToken = botToken,
        channelId = channelId,
        lastSyncedTimestamp = lastSyncedTimestamp,
        autoSyncEnabled = autoSyncEnabled
    )

    companion object {
        fun fromConfig(config: TelegramConfig): TelegramConfigEntity = TelegramConfigEntity(
            id = 1,
            botToken = config.botToken,
            channelId = config.channelId,
            lastSyncedTimestamp = config.lastSyncedTimestamp,
            autoSyncEnabled = config.autoSyncEnabled
        )
    }
}
