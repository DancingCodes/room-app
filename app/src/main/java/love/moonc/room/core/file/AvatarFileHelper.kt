package love.moonc.room.core.file

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream

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

    fun readAvatarBytes(context: Context, uri: Uri): ByteArray {
        validateAvatar(context, uri)
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("无法读取头像文件")

        input.use {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var totalBytes = 0
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                totalBytes += count
                if (totalBytes > maxBytes) {
                    throw IllegalArgumentException("头像不能超过 2MB")
                }
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }
    }
}
