package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val coverUrl: String,
    val songIdsRaw: String, // Comma separated song IDs
    val createdAt: Long
) {
    fun toPlaylist(): Playlist {
        val ids = if (songIdsRaw.isBlank()) emptyList() else songIdsRaw.split(",")
        return Playlist(
            id = id,
            name = name,
            description = description,
            coverUrl = coverUrl,
            songIds = ids,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromPlaylist(playlist: Playlist): PlaylistEntity = PlaylistEntity(
            id = playlist.id,
            name = playlist.name,
            description = playlist.description,
            coverUrl = playlist.coverUrl,
            songIdsRaw = playlist.songIds.joinToString(","),
            createdAt = playlist.createdAt
        )
    }
}
