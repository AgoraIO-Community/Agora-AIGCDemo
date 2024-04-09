package io.agora.aiteachingassistant.aigc

import io.agora.rtc2.IRtcEngineEventHandler

interface RtcCallback {
    fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int)
    fun onLeaveChannel(stats: IRtcEngineEventHandler.RtcStats)
    fun onMuteSuccess()
    fun onUnMuteSuccess()
    fun onAudioVolumeIndication(
        speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
        totalVolume: Int
    )
}