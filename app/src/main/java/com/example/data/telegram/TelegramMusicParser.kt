package com.example.data.telegram

import com.example.data.model.Song

object TelegramMusicParser {

    fun parseMessageToSong(
        message: TelegramMessage,
        botToken: String,
        channelId: String
    ): Song? {
        val audio = message.audio
        if (audio == null && message.document?.mime_type?.startsWith("audio") != true) {
            return null
        }

        val fileId = audio?.file_id ?: message.document?.file_id ?: return null
        val originalTitle = audio?.title
            ?: audio?.file_name?.removeSuffix(".mp3")?.removeSuffix(".m4a")
            ?: message.document?.file_name?.removeSuffix(".mp3")
            ?: extractTitleFromCaption(message.caption)
            ?: "Track #${message.message_id}"

        val performer = audio?.performer
            ?: extractArtistFromCaption(message.caption)
            ?: "Unknown Artist"

        val duration = audio?.duration ?: 210
        val lyrics = message.caption ?: ""

        // Generate consistent cover artwork color / image based on performer & title
        val coverUrl = generateAlbumArtUrl(performer, originalTitle)

        // For direct playback before getFile resolution, or fallback audio URL
        val audioUrl = "https://api.telegram.org/bot$botToken/getFile?file_id=$fileId"

        return Song(
            id = "tg_${message.message_id}_${fileId.takeLast(8)}",
            title = originalTitle,
            artist = performer,
            album = "Telegram Channel Stream",
            durationSeconds = duration,
            audioUrl = audioUrl,
            coverUrl = coverUrl,
            lyrics = lyrics,
            telegramMessageId = message.message_id,
            telegramFileId = fileId,
            genre = detectGenre(originalTitle, performer, lyrics)
        )
    }

    private fun extractTitleFromCaption(caption: String?): String? {
        if (caption.isNullOrBlank()) return null
        val lines = caption.trim().split("\n")
        val firstLine = lines.firstOrNull() ?: return null
        if (firstLine.contains("-")) {
            val parts = firstLine.split("-")
            if (parts.size >= 2) return parts[1].trim()
        }
        return firstLine.take(40)
    }

    private fun extractArtistFromCaption(caption: String?): String? {
        if (caption.isNullOrBlank()) return null
        val lines = caption.trim().split("\n")
        val firstLine = lines.firstOrNull() ?: return null
        if (firstLine.contains("-")) {
            val parts = firstLine.split("-")
            return parts[0].trim()
        }
        return null
    }

    private fun detectGenre(title: String, artist: String, caption: String): String {
        val combined = "$title $artist $caption".lowercase()
        return when {
            combined.contains("lofi") || combined.contains("chill") -> "Lofi / Chill"
            combined.contains("electro") || combined.contains("dance") || combined.contains("synth") -> "Electronic"
            combined.contains("rock") || combined.contains("indie") -> "Indie Rock"
            combined.contains("pop") -> "Pop"
            combined.contains("hip hop") || combined.contains("rap") -> "Hip-Hop"
            else -> "Pop / Modern"
        }
    }

    fun generateAlbumArtUrl(artist: String, title: String): String {
        val hash = (artist + title).hashCode() and 0x7FFFFFFF
        val colorHex = "%06x".format(hash % 0xFFFFFF)
        return "https://dummyimage.com/600x600/$colorHex/ffffff.png&text=${artist.take(1)}+${title.take(1)}"
    }
}

private fun String?.isNull_orBlank(): Boolean = this == null || this.trim().isEmpty()
