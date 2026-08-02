package com.example.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Single",
    val durationSeconds: Int = 180,
    val audioUrl: String,
    val coverUrl: String = "",
    val lyrics: String = "",
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val telegramMessageId: Long? = null,
    val telegramFileId: String? = null,
    val genre: String = "Pop"
) {
    val formattedDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
