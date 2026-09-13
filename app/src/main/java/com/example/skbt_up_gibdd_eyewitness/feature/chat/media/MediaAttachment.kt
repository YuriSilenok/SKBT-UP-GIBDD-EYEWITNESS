package com.example.skbt_up_gibdd_eyewitness.feature.chat.media

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

private const val MAX_PHOTO_BYTES = 10L * 1024 * 1024
private const val MAX_VIDEO_OR_GIF_BYTES = 100L * 1024 * 1024

enum class MediaKind { PHOTO, VIDEO, GIF }

data class MediaAttachment(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val mimeType: String,
    val sizeBytes: Long,
    val kind: MediaKind,
)

data class MediaSelectionResult(
    val accepted: List<MediaAttachment>,
    val rejectionMessages: List<String>,
)

fun resolveMediaSelection(context: Context, uris: List<Uri>): MediaSelectionResult {
    val resolver = context.contentResolver
    val accepted = mutableListOf<MediaAttachment>()
    val rejected = mutableListOf<String>()

    uris.forEach { uri ->
        val mimeType = resolver.getType(uri).orEmpty().lowercase()
        val kind = when {
            mimeType == "image/gif" -> MediaKind.GIF
            mimeType.startsWith("image/") -> MediaKind.PHOTO
            mimeType.startsWith("video/") -> MediaKind.VIDEO
            else -> null
        }
        if (kind == null) {
            rejected += "Формат файла не поддерживается"
            return@forEach
        }

        val size = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else -1L
        } ?: -1L
        val limit = if (kind == MediaKind.PHOTO) MAX_PHOTO_BYTES else MAX_VIDEO_OR_GIF_BYTES
        if (size < 0L) {
            rejected += "Не удалось определить размер файла"
            return@forEach
        }
        if (size > limit) {
            rejected += if (kind == MediaKind.PHOTO) "Фото больше 10 МБ" else "Видео или GIF больше 100 МБ"
            return@forEach
        }

        runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        accepted += MediaAttachment(uri = uri, mimeType = mimeType, sizeBytes = size, kind = kind)
    }
    return MediaSelectionResult(accepted, rejected.distinct())
}

fun MediaAttachment.displaySize(): String = when {
    sizeBytes >= 1024 * 1024 -> "%.1f МБ".format(sizeBytes / (1024.0 * 1024.0))
    else -> "${(sizeBytes / 1024).coerceAtLeast(1)} КБ"
}

fun createCaptureUri(context: Context, kind: MediaKind): Uri {
    require(kind == MediaKind.PHOTO || kind == MediaKind.VIDEO)
    val directory = File(context.cacheDir, "captured_media").apply { mkdirs() }
    val extension = if (kind == MediaKind.PHOTO) ".jpg" else ".mp4"
    val file = File.createTempFile(if (kind == MediaKind.PHOTO) "photo_" else "video_", extension, directory)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun galleryPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        android.Manifest.permission.READ_MEDIA_IMAGES,
        android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
}

fun hasGalleryPermission(context: Context): Boolean = galleryPermissions().any { permission ->
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

fun loadRecentPhotoUris(context: Context, limit: Int = 5): List<Uri> {
    if (!hasGalleryPermission(context)) return emptyList()
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    return context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        buildList {
            while (cursor.moveToNext() && size < limit) {
                add(Uri.withAppendedPath(collection, cursor.getLong(idColumn).toString()))
            }
        }
    }.orEmpty()
}
