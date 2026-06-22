package love.moonc.room.core.file

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

fun Uri.toAvatarPart(context: Context): MultipartBody.Part {
    AvatarFileHelper.validateAvatar(context, this)
    val mimeType = context.contentResolver.getType(this) ?: "image/jpeg"
    val bytes = context.contentResolver.openInputStream(this)?.use { it.readBytes() }
        ?: throw IllegalArgumentException("无法读取头像文件")
    val body = bytes.toRequestBody(mimeType.toMediaType())
    return MultipartBody.Part.createFormData("file", "avatar", body)
}
