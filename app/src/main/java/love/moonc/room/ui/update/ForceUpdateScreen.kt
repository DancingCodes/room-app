package love.moonc.room.ui.update

// 显示强制更新状态并发起系统安装流程；删除后检测到新版本时用户无法完成更新。

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import love.moonc.room.ui.components.CenteredFormColumn
import love.moonc.room.ui.components.PrimaryButton
import java.io.File

@Composable
fun ForceUpdateScreen(
    state: UpdateUiState,
    onRetryCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: (File) -> Unit,
) {
    BackHandler(enabled = true) {}

    CenteredFormColumn(
        verticalArrangement = Arrangement.Center,
    ) {
        val update = state.update
        if (update == null) {
            Text(
                if (state.error == null) "正在检查更新" else "检查更新失败",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(state.error ?: "请稍候")
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = "重新检查",
                loading = state.checking,
                onClick = onRetryCheck,
            )
            return@CenteredFormColumn
        }

        Text("发现新版本 v${update.versionCode}", style = MaterialTheme.typography.titleMedium)
        if (update.releaseNotes.isNotBlank()) {
            Text(update.releaseNotes, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))

        val apkPath = state.apkPath
        when {
            state.downloading -> {
                CircularProgressIndicator()
                Text(downloadText(state.downloadedBytes, state.totalBytes))
            }
            apkPath != null -> {
                Text("安装包已下载完成")
                PrimaryButton(
                    text = "安装更新",
                    loading = false,
                    onClick = { onInstall(File(apkPath)) },
                )
            }
            else -> {
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                PrimaryButton(
                    text = "立即更新",
                    loading = false,
                    onClick = onDownload,
                )
            }
        }
    }
}

fun launchPackageInstaller(context: Context, apkFile: File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        return
    }

    val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun downloadText(downloadedBytes: Long, totalBytes: Long): String {
    if (totalBytes <= 0) return "正在下载 ${downloadedBytes / 1024} KB"
    val progress = downloadedBytes * 100 / totalBytes
    return "正在下载 $progress%"
}
