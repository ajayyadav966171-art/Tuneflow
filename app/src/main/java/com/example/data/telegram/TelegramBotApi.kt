package com.example.data.telegram

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class TelegramResponse<T>(
    val ok: Boolean,
    val result: T?,
    val description: String?
)

data class TelegramUpdate(
    val update_id: Long,
    val message: TelegramMessage?,
    val channel_post: TelegramMessage?
)

data class TelegramMessage(
    val message_id: Long,
    val date: Long,
    val text: String?,
    val caption: String?,
    val audio: TelegramAudio?,
    val voice: TelegramAudio?,
    val document: TelegramDocument?
)

data class TelegramAudio(
    val file_id: String,
    val file_unique_id: String,
    val duration: Int?,
    val performer: String?,
    val title: String?,
    val file_name: String?,
    val mime_type: String?,
    val file_size: Long?,
    val thumbnail: TelegramPhotoSize?
)

data class TelegramDocument(
    val file_id: String,
    val file_name: String?,
    val mime_type: String?,
    val file_size: Long?
)

data class TelegramPhotoSize(
    val file_id: String,
    val width: Int,
    val height: Int,
    val file_size: Int?
)

data class TelegramFileInfo(
    val file_id: String,
    val file_unique_id: String?,
    val file_size: Long?,
    val file_path: String?
)

interface TelegramBotService {
    @GET("bot{token}/getUpdates")
    suspend fun getUpdates(
        @Path("token") token: String,
        @Query("offset") offset: Long? = null,
        @Query("limit") limit: Int = 100,
        @Query("allowed_updates") allowedUpdates: String = "[\"channel_post\",\"message\"]"
    ): Response<TelegramResponse<List<TelegramUpdate>>>

    @GET("bot{token}/getFile")
    suspend fun getFile(
        @Path("token") token: String,
        @Query("file_id") fileId: String
    ): Response<TelegramResponse<TelegramFileInfo>>
}

object TelegramApiClient {
    private const val BASE_URL = "https://api.telegram.org/"

    val service: TelegramBotService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(TelegramBotService::class.java)
    }

    fun getFileDirectUrl(botToken: String, filePath: String): String {
        return "https://api.telegram.org/file/bot$botToken/$filePath"
    }
}
