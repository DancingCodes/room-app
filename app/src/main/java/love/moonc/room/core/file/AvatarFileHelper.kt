package love.moonc.room.core.file

import android.content.Context
import android.net.Uri

object AvatarFileHelper {
    private const val maxBytes = 2 * 1024 * 1024

    fun validateAvatar(context: Context, uri: Uri) {
        val mime = context.contentResolver.getType(uri).orEmpty()
        if (mime !in setOf("image/jpeg", "image/png", "image/webp")) {
            throw IllegalArgumentException("头像只支持 JPEG、PNG、WebP")
        }
        val size = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        if (size > maxBytes) {
            throw IllegalArgumentException("头像不能超过 2MB")
        }
    }
}
