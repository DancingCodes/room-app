package love.moonc.room.core.voice

import android.content.Context
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import love.moonc.room.core.network.NetworkConfig

class AgoraVoiceClient(context: Context) {
    private var tokenRefreshListener: (() -> Unit)? = null
    private val engine = RtcEngine.create(
        context.applicationContext,
        NetworkConfig.AGORA_APP_ID,
        object : IRtcEngineEventHandler() {
            override fun onTokenPrivilegeWillExpire(token: String?) {
                tokenRefreshListener?.invoke()
            }

            override fun onRequestToken() {
                tokenRefreshListener?.invoke()
            }
        },
    )
    private var joinedRoomId: Long? = null

    fun join(roomId: Long, userId: Long, token: String, onTokenRefreshRequired: () -> Unit) {
        if (joinedRoomId == roomId) return
        leave()
        require(userId in 1..Int.MAX_VALUE.toLong()) { "用户 ID 不支持语音" }
        require(token.isNotBlank()) { "语音 Token 为空" }

        tokenRefreshListener = onTokenRefreshRequired
        engine.enableAudio()
        engine.setEnableSpeakerphone(true)
        engine.enableLocalAudio(false)
        engine.muteLocalAudioStream(true)
        val result = engine.joinChannel(
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
    }

    fun renewToken(token: String) {
        if (joinedRoomId != null && token.isNotBlank()) {
            engine.renewToken(token)
        }
    }

    fun setMicrophoneEnabled(enabled: Boolean) {
        if (enabled) {
            engine.enableLocalAudio(true)
        }
        engine.muteLocalAudioStream(!enabled)
        engine.updateChannelMediaOptions(
            ChannelMediaOptions().apply {
                publishMicrophoneTrack = enabled
                autoSubscribeAudio = true
            },
        )
        if (!enabled) {
            engine.enableLocalAudio(false)
        }
    }

    fun leave() {
        if (joinedRoomId == null) return
        tokenRefreshListener = null
        engine.leaveChannel()
        joinedRoomId = null
    }

    private fun channelName(roomId: Long): String = "room-$roomId"
}