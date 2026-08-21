package love.moonc.room.core.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import love.moonc.room.core.network.NetworkConfig

class AgoraVoiceClient(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var tokenRefreshListener: (() -> Unit)? = null

    private var engine: RtcEngine? = null
    private var joinedRoomId: Long? = null

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

    fun renewToken(token: String) = runOnMain {
        if (joinedRoomId != null && token.isNotBlank()) {
            engine?.renewToken(token)
        }
    }

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

    fun leave() = runOnMain(::leaveInternal)

    private fun leaveInternal() {
        if (joinedRoomId == null) return
        tokenRefreshListener = null
        engine?.leaveChannel()
        joinedRoomId = null
    }

    private fun getOrCreateEngine(): RtcEngine = engine ?: RtcEngine.create(
        appContext,
        NetworkConfig.AGORA_APP_ID,
        object : IRtcEngineEventHandler() {
            override fun onTokenPrivilegeWillExpire(token: String?) = notifyTokenRefreshRequired()

            override fun onRequestToken() = notifyTokenRefreshRequired()
        },
    ).also { engine = it }

    private fun notifyTokenRefreshRequired() {
        runOnMain { tokenRefreshListener?.invoke() }
    }

    private fun runOnMain(action: () -> Unit) {
        mainHandler.post(action)
    }

    private fun channelName(roomId: Long): String = "room-$roomId"
}
