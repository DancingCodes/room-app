package love.moonc.room.core.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import love.moonc.room.core.network.NetworkConfig

// 管理声网引擎的加入、开麦、续签和退出；集中管理可避免房间页面重复创建多个 RTC 引擎。
class AgoraVoiceClient(context: Context) {
    private val appContext = context.applicationContext
    // 声网引擎操作必须串行在主线程执行；删除后并发调用可能导致 SDK 状态异常。
    private val mainHandler = Handler(Looper.getMainLooper())

    // Token 即将过期时回调 ViewModel 请求新 Token；删除后长时间停留房间会断开语音。
    @Volatile
    private var tokenRefreshListener: (() -> Unit)? = null

    private var engine: RtcEngine? = null
    private var joinedRoomId: Long? = null

    // 加入指定房间的语音频道，初始仅订阅远端音频、不采集本地麦克风。
    fun join(
        roomId: Long,
        userId: Long,
        token: String,
        onTokenRefreshRequired: () -> Unit,
        onFailure: () -> Unit,
    ) = runOnMain {
        if (joinedRoomId == roomId) return@runOnMain
        leaveInternal()
        require(userId in 1..Int.MAX_VALUE.toLong()) { "用户 ID 不支持语音" }
        require(token.isNotBlank()) { "语音 Token 为空" }

        runCatching {
            tokenRefreshListener = onTokenRefreshRequired
            val rtcEngine = getOrCreateEngine()
            rtcEngine.enableAudio()
            rtcEngine.setEnableSpeakerphone(true)
            rtcEngine.enableLocalAudio(false)
            rtcEngine.muteLocalAudioStream(true)
            val result = rtcEngine.joinChannel(
                token,
                channelName(roomId),
                userId.toInt(),
                ChannelMediaOptions().apply {
                    clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    autoSubscribeAudio = true
                    publishMicrophoneTrack = false
                },
            )
            check(result == 0) { "语音连接失败（错误码 $result）" }
            joinedRoomId = roomId
        }.onFailure {
            tokenRefreshListener = null
            onFailure()
        }
    }

    // 用后端签发的新 Token 延长当前语音会话；删除后 Token 过期无法恢复。
    fun renewToken(token: String) = runOnMain {
        if (joinedRoomId != null && token.isNotBlank()) {
            engine?.renewToken(token)
        }
    }

    // 根据后端确认的麦克风状态发布或静音本地音轨；删除后 UI 麦克风状态与实际语音状态会不一致。
    fun setMicrophoneEnabled(enabled: Boolean) = runOnMain {
        val rtcEngine = engine ?: return@runOnMain
        if (enabled) {
            rtcEngine.enableLocalAudio(true)
        }
        rtcEngine.muteLocalAudioStream(!enabled)
        rtcEngine.updateChannelMediaOptions(
            ChannelMediaOptions().apply {
                publishMicrophoneTrack = enabled
                autoSubscribeAudio = true
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            },
        )
        if (!enabled) {
            rtcEngine.enableLocalAudio(false)
        }
    }

    // 主动离开房间时退出语音频道；删除后离开后仍可能占用语音连接。
    fun leave() = runOnMain(::leaveInternal)

    private fun leaveInternal() {
        if (joinedRoomId == null) return
        tokenRefreshListener = null
        engine?.leaveChannel()
        joinedRoomId = null
    }

    // 仅在首次入房时创建声网引擎，并注册 Token 续签回调。
    private fun getOrCreateEngine(): RtcEngine = engine ?: RtcEngine.create(
        appContext,
        NetworkConfig.AGORA_APP_ID,
        object : IRtcEngineEventHandler() {
            override fun onTokenPrivilegeWillExpire(token: String?) = notifyTokenRefreshRequired()

            override fun onRequestToken() = notifyTokenRefreshRequired()
        },
    ).also { engine = it }

    // 将 SDK 回调切回主线程，再请求新的语音 Token。
    private fun notifyTokenRefreshRequired() {
        runOnMain { tokenRefreshListener?.invoke() }
    }

    private fun runOnMain(action: () -> Unit) {
        mainHandler.post(action)
    }

    private fun channelName(roomId: Long): String = "room-$roomId"
}
