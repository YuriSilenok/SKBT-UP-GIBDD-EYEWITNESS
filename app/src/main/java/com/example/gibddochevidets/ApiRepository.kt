package com.example.gibddochevidets.network

import com.example.gibddochevidets.ApiService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.gibddochevidets.PrivacyLog as Log
import com.example.gibddochevidets.BanActivity
import com.example.gibddochevidets.data.DeviceFingerprint
import com.example.gibddochevidets.data.SessionManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiRepository(context: Context) {
    suspend fun receiptNotifications(): List<Map<String, String>> = api.receipts(getRequiredDeviceId(), "Bearer ${getRequiredToken()}")["receipts"].orEmpty()
    suspend fun grantPrivacyConsent() {
        val prefs = appContext.getSharedPreferences("privacy", Context.MODE_PRIVATE)
        val accepted = prefs.getLong("accepted_at", 0)
        if (prefs.getLong("submitted_at", -1) == accepted) return
        api.consent("Bearer ${getRequiredToken()}", mapOf("version" to com.example.gibddochevidets.PrivacyActivity.VERSION,
            "clicked_at" to java.time.Instant.ofEpochMilli(accepted).toString()))
        prefs.edit().putLong("submitted_at", accepted).apply()
    }
    suspend fun withdrawPrivacyConsent() = api.withdrawConsent("Bearer ${getRequiredToken()}")

    private val appContext =
        context.applicationContext

    private val session =
        SessionManager(appContext)

    private val clientApp =
        "eyewitness"

    private val gson =
        Gson()

    private val api: ApiService =
        Retrofit.Builder()
            .baseUrl(
                com.example.gibddochevidets.BuildConfig.API_BASE_URL
            )
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(
                        30,
                        TimeUnit.SECONDS
                    )
                    .readTimeout(
                        5,
                        TimeUnit.MINUTES
                    )
                    .writeTimeout(
                        5,
                        TimeUnit.MINUTES
                    )
                    .callTimeout(
                        10,
                        TimeUnit.MINUTES
                    )
                    .build()
            )
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiService::class.java)


    // ============================================================
    // РЕГИСТРАЦИЯ УСТРОЙСТВА
    // ============================================================

    suspend fun registerDevice(): RegisterResponse {

        val fingerprint =
            DeviceFingerprint.get(appContext)

        val pushToken: String? = null

        return try {

            val result =
                api.registerDevice(
                    authorization = session.accessToken?.let { "Bearer $it" },
                    clientApp = clientApp,
                    request =
                        RegisterDeviceRequest(
                            fingerprint_hash =
                                fingerprint,
                            push_token =
                                pushToken
                        )
                )

            session.deviceId =
                result.device_id

            session.accessToken =
                result.access_token

            session.fingerprintHash =
                fingerprint

            result

        } catch (e: HttpException) {

            Log.e(
                "REGISTER_DEBUG",
                "HTTP ${e.code()} ${e.message()}"
            )

            Log.e(
                "REGISTER_DEBUG",
                "SERVER BODY = ${
                    e.response()
                        ?.errorBody()
                        ?.string()
                }"
            )

            throw e
        }
    }

    // ============================================================
    // ПРОВЕРКА ЗДОРОВЬЯ СЕРВЕРА
    // ============================================================

    suspend fun checkHealth(): Boolean {
        return try {
            val response: Response<Unit> = api.health()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    // ============================================================
    // ПОЛУЧЕНИЕ АКТИВНОГО БАНА
    // ============================================================

    suspend fun getActiveBan(): BanStatusResponse? {
        val token = getRequiredToken()
        return try {
            val response: Response<BanStatusResponse> = api.getActiveBan(
                authorization = "Bearer $token",
                clientApp = clientApp
            )
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: HttpException) {
            if (e.code() == 404 || e.code() == 403) {
                null
            } else {
                throw e
            }
        }
    }

    // ============================================================
    // ТЕКСТОВОЕ СООБЩЕНИЕ
    // ============================================================

    suspend fun sendMessage(
        text: String
    ): MessageResponse {

        val token =
            getRequiredToken()

        requireRegistered()

        val cleanText =
            text.trim()

        require(
            cleanText.isNotEmpty()
        ) {
            "Сообщение не может быть пустым"
        }

        return try {

            api.sendMessage(
                authorization =
                    "Bearer $token",

                clientApp =
                    clientApp,

                request =
                    SendMessageRequest(
                        message_type =
                            "TEXT",

                        observer_device_id =
                            null,

                        text =
                            cleanText
                    )
            )

        } catch (e: HttpException) {

            handleBanException(e)

            throw e
        }
    }


    // ============================================================
    // ПОЛУЧЕНИЕ СООБЩЕНИЙ
    // ============================================================

    suspend fun getMessages(
        afterMessageId: String? = null
    ): List<MessageResponse> {

        val token =
            getRequiredToken()

        val deviceId =
            getRequiredDeviceId()

        val all = mutableListOf<MessageResponse>()
        var cursor = afterMessageId
        while (true) {
            val page = api.getMessages(authorization = "Bearer $token", clientApp = clientApp,
                observerDeviceId = deviceId, afterMessageId = cursor, limit = 100).messages
            all.addAll(page)
            if (page.size < 100 || page.lastOrNull()?.message_id == cursor) break
            cursor = page.last().message_id
        }
        return all.distinctBy { it.message_id }
    }


    // ============================================================
    // СКАЧИВАНИЕ MEDIA
    // ============================================================

    suspend fun downloadMedia(
        messageId: String
    ): ByteArray {

        require(
            messageId.isNotBlank()
        ) {
            "messageId пустой"
        }

        val token =
            getRequiredToken()

        requireRegistered()

        return api.downloadMedia(
            authorization =
                "Bearer $token",

            clientApp =
                clientApp,

            messageId =
                messageId
        ).use {
            it.bytes()
        }
    }


    // ============================================================
    // СКАЧИВАНИЕ MEDIA В ФАЙЛ
    // ============================================================

    suspend fun downloadMediaToFile(
        messageId: String
    ): File? {

        require(
            messageId.isNotBlank()
        ) {
            "messageId пустой"
        }

        val token =
            getRequiredToken()

        requireRegistered()

        val dir =
            File(
                appContext.cacheDir,
                "media_cache"
            ).apply {
                mkdirs()
            }

        val target =
            File(
                dir,
                "media_$messageId"
            )

        if (
            target.exists() &&
            target.length() > 0L
        ) {
            return target
        }

        val temp =
            File(
                dir,
                "media_${messageId}.tmp"
            )

        if (temp.exists()) {
            temp.delete()
        }

        try {

            api.downloadMedia(
                authorization =
                    "Bearer $token",

                clientApp =
                    clientApp,

                messageId =
                    messageId
            ).use { body ->

                body.byteStream().use { input ->

                    temp.outputStream().use { output ->

                        val buffer =
                            ByteArray(64 * 1024)

                        while (true) {

                            val count =
                                input.read(buffer)

                            if (count == -1) {
                                break
                            }

                            output.write(
                                buffer,
                                0,
                                count
                            )
                        }

                        output.flush()
                    }
                }
            }

            if (
                !temp.exists() ||
                temp.length() == 0L
            ) {

                temp.delete()

                return null
            }

            if (target.exists()) {
                target.delete()
            }

            check(
                temp.renameTo(target)
            ) {
                "Не удалось сохранить медиафайл"
            }

            return target

        } catch (e: Exception) {

            temp.delete()

            throw e
        }
    }


    // ============================================================
    // ДОСТАВКА СООБЩЕНИЯ
    // ============================================================

    suspend fun markDelivered(
        messageId: String
    ): MessageResponse {

        val token =
            getRequiredToken()

        require(
            messageId.isNotBlank()
        ) {
            "messageId пустой"
        }

        return api.markDelivered(
            authorization =
                "Bearer $token",

            clientApp =
                clientApp,

            messageId =
                messageId
        )
    }


    // ============================================================
    // СТАТИЧЕСКАЯ ТОЧКА
    // ============================================================

    suspend fun sendStaticLocation(
        latitude: Double,
        longitude: Double
    ): MessageResponse {

        val token =
            getRequiredToken()

        requireRegistered()

        require(
            latitude in -90.0..90.0
        ) {
            "Некорректная широта"
        }

        require(
            longitude in -180.0..180.0
        ) {
            "Некорректная долгота"
        }

        return try {

            api.sendStaticLocation(
                authorization =
                    "Bearer $token",

                clientApp =
                    clientApp,

                request =
                    StaticLocationRequest(
                        latitude,
                        longitude
                    )
            )

        } catch (e: HttpException) {

            handleBanException(e)

            throw e
        }
    }


    // ============================================================
    // MEDIA ПО STORAGE KEY
    // ============================================================

    suspend fun uploadMedia(
        uri: Uri
    ): MessageResponse {

        val token =
            getRequiredToken()

        requireRegistered()

        val resolver =
            appContext.contentResolver

        val mimeType =
            resolver
                .getType(uri)
                ?.lowercase()
                ?: "application/octet-stream"

        val fileName =
            getFileName(uri)

        val size =
            querySize(uri)

        require(
            size != 0L
        ) {
            "Выбранный файл пустой"
        }

        val body =
            ContentUriRequestBody(
                resolver =
                    resolver,

                uri =
                    uri,

                mimeType =
                    mimeType,

                sizeBytes =
                    size
            )

        val part =
            MultipartBody.Part.createFormData(
                "file",
                fileName,
                body
            )

        return try {

            api.uploadMedia(
                authorization =
                    "Bearer $token",

                clientApp =
                    clientApp,

                file =
                    part
            )

        } catch (e: HttpException) {

            handleBanException(e)

            throw e
        }
    }


    // ============================================================
    // START LIVE
    // ============================================================

    suspend fun startLiveLocation():
            MessageResponse {

        val token =
            getRequiredToken()

        requireRegistered()

        return try {

            api.startLiveLocation(
                authorization =
                    "Bearer $token",

                clientApp =
                    clientApp,

                request =
                    LiveLocationStartRequest(
                        observer_device_id =
                            null
                    )
            )

        } catch (e: HttpException) {

            handleBanException(e)

            throw e
        }
    }


    // ============================================================
    // LIVE — ОТПРАВКА ТОЧКИ
    // ============================================================

    suspend fun sendLiveLocationPoint(
        messageId: String,
        latitude: Double,
        longitude: Double
    ): LiveLocationPointResponse {

        val token =
            getRequiredToken()

        requireRegistered()

        require(
            messageId.isNotBlank()
        ) {
            "messageId пустой"
        }

        require(
            latitude in -90.0..90.0
        ) {
            "Некорректная широта"
        }

        require(
            longitude in -180.0..180.0
        ) {
            "Некорректная долгота"
        }

        return try {

            api.sendLiveLocationPoint(
                authorization =
                    "Bearer $token",

                clientApp =
                    clientApp,

                messageId =
                    messageId,

                request =
                    LiveLocationPointRequest(
                        latitude,
                        longitude
                    )
            )

        } catch (e: HttpException) {

            handleBanException(e)

            throw e
        }
    }


    // ============================================================
    // LIVE — ОСТАНОВКА
    // ============================================================

    suspend fun stopLiveLocation(
        messageId: String
    ): MessageResponse {

        val token =
            getRequiredToken()

        requireRegistered()

        require(
            messageId.isNotBlank()
        ) {
            "messageId пустой"
        }

        return api.stopLiveLocation(
            authorization =
                "Bearer $token",

            clientApp =
                clientApp,

            messageId =
                messageId
        )
    }


    // ============================================================
    // LIVE — ПОЛУЧЕНИЕ ТОЧЕК
    // ============================================================

    private fun handleBanException(
        exception: HttpException
    ) {

        if (exception.code() != 403) {
            return
        }

        val errorBody =
            try {
                exception
                    .response()
                    ?.errorBody()
                    ?.string()
            } catch (_: Exception) {
                null
            }

        Log.e(
            "BAN_DEBUG",
            "HTTP 403"
        )

        Log.e(
            "BAN_DEBUG",
            "SERVER BODY = $errorBody"
        )

        if (isObserverBanned(errorBody)) {

            openBanActivity(
                errorBody
            )
        }
    }


    private fun isObserverBanned(
        errorBody: String?
    ): Boolean {

        if (
            errorBody.isNullOrBlank()
        ) {
            return false
        }

        return try {

            val json =
                gson.fromJson(
                    errorBody,
                    JsonObject::class.java
                )

            val detail =
                json
                    ?.get("detail")
                    ?.asString
                    ?.trim()

            detail ==
                    "Observer device is banned"

        } catch (_: Exception) {

            // Запасной вариант,
            // если backend вернул не совсем
            // стандартный JSON.

            errorBody.contains(
                "Observer device is banned",
                ignoreCase = true
            )
        }
    }


    private fun openBanActivity(
        errorBody: String?
    ) {

        try {

            val intent =
                Intent(
                    appContext,
                    BanActivity::class.java
                )

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )

            /*
             * Сейчас backend в документации
             * возвращает только:
             *
             * "Observer device is banned"
             *
             * Поэтому дату сюда пока передать
             * невозможно.
             *
             * Когда backend начнет отдавать
             * ends_at, здесь добавим:
             *
             * intent.putExtra(
             *     BanActivity.EXTRA_ENDS_AT,
             *     endsAt
             * )
             */

            appContext.startActivity(
                intent
            )

        } catch (e: Exception) {

            Log.e(
                "BAN_DEBUG",
                "Не удалось открыть BanActivity",
                e
            )
        }
    }


    // ============================================================
    // FILE
    // ============================================================

    private fun querySize(
        uri: Uri
    ): Long {

        appContext.contentResolver.query(
            uri,
            arrayOf(
                OpenableColumns.SIZE
            ),
            null,
            null,
            null
        )?.use { cursor ->

            if (
                cursor.moveToFirst() &&
                !cursor.isNull(0)
            ) {

                return cursor.getLong(0)
            }
        }

        return -1L
    }


    private fun getFileName(
        uri: Uri
    ): String {

        appContext.contentResolver.query(
            uri,
            arrayOf(
                OpenableColumns.DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->

            if (
                cursor.moveToFirst()
            ) {

                val index =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (index >= 0) {

                    val name =
                        cursor.getString(index)

                    if (
                        !name.isNullOrBlank()
                    ) {
                        return name
                    }
                }
            }
        }

        return "media_${System.currentTimeMillis()}"
    }


    // ============================================================
    // SESSION
    // ============================================================

    fun getDeviceId(): String? =
        session.deviceId

    fun getAccessToken(): String? =
        session.accessToken

private fun getRequiredToken(): String =
        session.accessToken
            ?: throw IllegalStateException(
                "Access token is missing"
            )


    private fun getRequiredDeviceId(): String =
        session.deviceId
            ?: throw IllegalStateException(
                "Device is not registered"
            )


    private fun requireRegistered() {

        if (
            session.deviceId == null
        ) {

            throw IllegalStateException(
                "Device is not registered"
            )
        }

        if (
            session.accessToken == null
        ) {

            throw IllegalStateException(
                "Access token is missing"
            )
        }
    }
}


// ==================================================================
// STREAMING REQUEST BODY
// ==================================================================

private class ContentUriRequestBody(
    private val resolver:
    android.content.ContentResolver,

    private val uri:
    Uri,

    private val mimeType:
    String,

    private val sizeBytes:
    Long

) : RequestBody() {

    override fun contentType() =
        mimeType.toMediaTypeOrNull()

    override fun contentLength(): Long =
        sizeBytes

    override fun writeTo(
        sink: BufferedSink
    ) {

        val input =
            resolver.openInputStream(uri)
                ?: throw IOException(
                    "Не удалось открыть выбранный файл"
                )

        input.use { source ->

            val buffer =
                ByteArray(64 * 1024)

            while (true) {

                val count =
                    source.read(buffer)

                if (count == -1) {
                    break
                }

                sink.write(
                    buffer,
                    0,
                    count
                )
            }
        }
    }
}
