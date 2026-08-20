package love.moonc.room.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import love.moonc.room.BuildConfig
import love.moonc.room.core.network.ApiException
import love.moonc.room.core.update.ApkDownloader
import love.moonc.room.data.api.RoomApi
import love.moonc.room.data.model.AppVersion

data class UpdateUiState(
    val checking: Boolean = true,
    val update: AppVersion? = null,
    val downloading: Boolean = false,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = -1,
    val apkPath: String? = null,
    val error: String? = null,
) {
    val canEnterApp: Boolean
        get() = !checking && update == null && error == null
}

class UpdateViewModel(
    private val api: RoomApi,
    private val downloader: ApkDownloader,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState

    init {
        checkForUpdate()
    }

    fun checkForUpdate() {
        if (_uiState.value.downloading) return
        _uiState.value = UpdateUiState(checking = true)
        viewModelScope.launch {
            runCatching {
                val response = api.latestAppVersion()
                if (response.code != 200) throw ApiException(response.code, response.message)
                response.data
            }.onSuccess { version ->
                _uiState.value = UpdateUiState(
                    update = version?.takeIf { it.versionCode > BuildConfig.VERSION_CODE },
                )
            }.onFailure { error ->
                _uiState.value = UpdateUiState(error = error.message ?: "检查更新失败")
            }
        }
    }

    fun download() {
        val version = _uiState.value.update ?: return
        if (_uiState.value.downloading) return

        _uiState.value = _uiState.value.copy(
            downloading = true,
            downloadedBytes = 0,
            totalBytes = -1,
            apkPath = null,
            error = null,
        )
        viewModelScope.launch {
            runCatching {
                downloader.download(version) { downloaded, total ->
                    _uiState.value = _uiState.value.copy(
                        downloadedBytes = downloaded,
                        totalBytes = total,
                    )
                }
            }.onSuccess { apkFile ->
                _uiState.value = _uiState.value.copy(
                    downloading = false,
                    apkPath = apkFile.absolutePath,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    downloading = false,
                    error = error.message ?: "下载更新失败",
                )
            }
        }
    }
}