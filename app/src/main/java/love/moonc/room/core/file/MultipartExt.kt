package love.moonc.room.core.file

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

fun Uri.toImagePart(context: Context): MultipartBody.Part {
    val mimeType = context.contentResolver.getType(this) ?: "image/jpeg"
    val bytes = AvatarFileHelper.readAvatarBytes(context, this)
    val body = bytes.toRequestBody(mimeType.toMediaType())
    return MultipartBody.Part.createFormData("file", "image", body)
}
