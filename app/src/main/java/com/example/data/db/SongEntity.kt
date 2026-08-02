package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val audioUrl: String,
    val coverUrl: String,
    val lyrics: String,
    val isFavorite: Boolean,
    val playCount: Int,
    val lastPlayedTimestamp: Long,
    val telegramMessageId: Long?,
    val telegramFileId: String?,
    val genre: String
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationSeconds = durationSeconds,
        audioUrl = audioUrl,
        coverUrl = coverUrl,
        lyrics = lyrics,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedTimestamp = lastPlayedTimestamp,
        telegramMessageId = telegramMessageId,
        telegramFileId = telegramFileId,
        genre = genre
    )

    companion object {
        fun fromSong(song: Song): SongEntity = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationSeconds = song.durationSeconds,
            audioUrl = song.audioUrl,
            coverUrl = song.coverUrl,
            lyrics = song.lyrics,
            isFavorite = song.isFavorite,
            playCount = song.playCount,
            lastPlayedTimestamp = song.lastPlayedTimestamp,
            telegramMessageId = song.telegramMessageId,
            telegramFileId = song.telegramFileId,
            genre = song.genre
        )
    }
}
