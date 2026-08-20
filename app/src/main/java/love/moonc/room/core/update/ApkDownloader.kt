package love.moonc.room.core.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import love.moonc.room.data.model.AppVersion
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

class ApkDownloader(
    private val okHttpClient: OkHttpClient,
    private val cacheDir: File,
) {
    suspend fun download(version: AppVersion, onProgress: (Long, Long) -> Unit): File = withContext(Dispatchers.IO) {
        validate(version)

        val updateDir = File(cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updateDir, "room-${version.versionCode}.apk")
        val temporaryFile = File(updateDir, "room-${version.versionCode}.apk.part")
        apkFile.delete()
        temporaryFile.delete()

        try {
            okHttpClient.newCall(Request.Builder().url(version.apkUrl).build()).execute().use { response ->
                if (!response.isSuccessful || !response.request.url.isHttps) {
                    throw IOException("下载更新失败")
                }
                val body = response.body
                val totalBytes = body.contentLength()
                val digest = MessageDigest.getInstance("SHA-256")
                var downloadedBytes = 0L

                body.byteStream().use { input ->
                    FileOutputStream(temporaryFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            downloadedBytes += count
                            onProgress(downloadedBytes, totalBytes)
                        }
                        output.fd.sync()
                    }
                }

                val actualHash = digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
                if (!MessageDigest.isEqual(actualHash.toByteArray(), version.apkSha256.lowercase().toByteArray())) {
                    throw IOException("安装包校验失败")
                }
                if (!temporaryFile.renameTo(apkFile)) {
                    throw IOException("保存安装包失败")
                }
                apkFile
            }
        } catch (error: Throwable) {
            temporaryFile.delete()
            apkFile.delete()
            throw error
        }
    }

    private fun validate(version: AppVersion) {
        require(version.apkUrl.startsWith("https://")) { "安装包地址错误" }
        require(sha256Pattern.matches(version.apkSha256)) { "安装包校验值错误" }
    }

    private companion object {
        val sha256Pattern = Regex("[0-9a-fA-F]{64}")
    }
}