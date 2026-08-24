package love.moonc.room.core.file

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

fun Uri.toImagePart(context: Context): MultipartBody.Part {
    val mimeType = context.contentResolver.getType(this) ?: "application/octet-stream"
    val bytes = context.contentResolver.openInputStream(this)?.use { it.readBytes() }
        ?: throw IllegalArgumentException("无法读取头像文件")
    val body = bytes.toRequestBody(mimeType.toMediaType())
    return MultipartBody.Part.createFormData("file", "image", body)
}
