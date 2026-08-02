package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Query("SELECT * FROM telegram_config WHERE id = 1 LIMIT 1")
    fun getTelegramConfig(): Flow<TelegramConfigEntity?>

    @Query("SELECT * FROM telegram_config WHERE id = 1 LIMIT 1")
    suspend fun getTelegramConfigSync(): TelegramConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTelegramConfig(config: TelegramConfigEntity)
}
