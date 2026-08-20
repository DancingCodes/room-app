package love.moonc.room.core.voice

import android.content.Context
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import love.moonc.room.core.network.NetworkConfig

class AgoraVoiceClient(context: Context) {
    private val engine = RtcEngine.create(
        context.applicationContext,
        NetworkConfig.AGORA_APP_ID,
        object : IRtcEngineEventHandler() {},
    )
    private var joinedRoomId: Long? = null

    fun join(roomId: Long, userId: Long) {
        if (joinedRoomId == roomId) return
        leave()
        require(userId in 1..Int.MAX_VALUE.toLong()) { "用户 ID 不支持语音" }

        engine.enableAudio()
        engine.setEnableSpeakerphone(true)
        engine.enableLocalAudio(false)
        engine.muteLocalAudioStream(true)
        val result = engine.joinChannel(
            null,
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
        engine.leaveChannel()
        joinedRoomId = null
    }

    private fun channelName(roomId: Long): String = "room-$roomId"
}