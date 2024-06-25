package io.agora.aigc_cloud_demo.agora

import android.content.Context
import io.agora.aigc_cloud_demo.utils.KeyCenter
import io.agora.aigc_cloud_demo.utils.LogUtils
import io.agora.aigc_cloud_demo.utils.Utils
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IAudioFrameObserver
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.audio.AudioParams
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale

object RtcManager : IAudioFrameObserver {
    private var mRtcEngine: RtcEngine? = null
    private var mCallback: RtcCallback? = null
    private var mChannelId: String = ""
    private var mTime: String = ""
    private val SAVE_AUDIO_RECORD_PCM = false


    fun initRtcEngine(context: Context, rtcCallback: RtcCallback, useCertificate: Boolean = true) {
        mCallback = rtcCallback
        try {
            LogUtils.d("RtcEngine version:" + RtcEngine.getSdkVersion())
            val rtcEngineConfig = RtcEngineConfig()
            rtcEngineConfig.mContext = context
            rtcEngineConfig.mAppId = KeyCenter.APP_ID
            rtcEngineConfig.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            rtcEngineConfig.mEventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    LogUtils.d("onJoinChannelSuccess channel:$channel uid:$uid elapsed:$elapsed")
                    mCallback?.onJoinChannelSuccess(channel, uid, elapsed)
                }

                override fun onLeaveChannel(stats: RtcStats) {
                    LogUtils.d("onLeaveChannel")
                    mCallback?.onLeaveChannel(stats)
                }

                override fun onLocalAudioStateChanged(state: Int, error: Int) {
                    super.onLocalAudioStateChanged(state, error)
                    LogUtils.d("onLocalAudioStateChanged state:$state error:$error")
                    if (Constants.LOCAL_AUDIO_STREAM_STATE_RECORDING == state) {
                        mCallback?.onUnMuteSuccess()
                    } else if (Constants.LOCAL_AUDIO_STREAM_STATE_STOPPED == state) {
                        mCallback?.onMuteSuccess()
                    }
                }

                override fun onAudioVolumeIndication(
                    speakers: Array<out AudioVolumeInfo>?,
                    totalVolume: Int
                ) {
                    super.onAudioVolumeIndication(speakers, totalVolume)
                    mCallback?.onAudioVolumeIndication(speakers, totalVolume)
                }

                override fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray?) {
                    super.onStreamMessage(uid, streamId, data)
                    LogUtils.d("onStreamMessage uid:$uid streamId:$streamId data:${data?.size}")
                    mCallback?.onStreamMessage(uid, streamId, data)
                }
            }
            rtcEngineConfig.mAudioScenario = Constants.AUDIO_SCENARIO_GAME_STREAMING
            mRtcEngine = RtcEngine.create(rtcEngineConfig)

            mRtcEngine?.setParameters("{\"rtc.enable_debug_log\":true}")
            mRtcEngine?.setParameters("{\"che.audio.adm_android_mode\":9}")

            mRtcEngine?.enableAudio()

            mRtcEngine?.setDefaultAudioRoutetoSpeakerphone(true)

            mRtcEngine?.setRecordingAudioFrameParameters(
                16000,
                1,
                Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY,
                640
            )

//            //min 50ms
//            mRtcEngine?.enableAudioVolumeIndication(
//                50,
//                3,
//                true
//            )

            mChannelId = getChannelId()
            val ret = mRtcEngine?.joinChannel(if (useCertificate)
                KeyCenter.getRtcToken(
                    mChannelId,
                    KeyCenter.getUid()
                ) else "",
                mChannelId,
                KeyCenter.getUid(),
                object : ChannelMediaOptions() {
                    init {
                        publishMicrophoneTrack = true
                        autoSubscribeAudio = true
                        clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    }
                })
            LogUtils.d("initRtcEngine ret:$ret")
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtils.e("initRtcEngine error:" + e.message)
        }
    }

    override fun onRecordAudioFrame(
        channelId: String?,
        type: Int,
        samplesPerChannel: Int,
        bytesPerSample: Int,
        channels: Int,
        samplesPerSec: Int,
        buffer: ByteBuffer?,
        renderTimeMs: Long,
        avsync_type: Int
    ): Boolean {
        val length = buffer!!.remaining()
        val origin = ByteArray(length)
        buffer[origin]
        buffer.flip()
        if (SAVE_AUDIO_RECORD_PCM) {
            try {
                val fos = FileOutputStream(
                    "/sdcard/Android/Data/io.agora.mccex_demo/cache/audio_" + mTime + ".pcm",
                    true
                )
                fos.write(origin)
                fos.close()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        return true
    }

    override fun onPlaybackAudioFrame(
        channelId: String?,
        type: Int,
        samplesPerChannel: Int,
        bytesPerSample: Int,
        channels: Int,
        samplesPerSec: Int,
        buffer: ByteBuffer?,
        renderTimeMs: Long,
        avsync_type: Int
    ): Boolean {

        return true
    }

    override fun onMixedAudioFrame(
        channelId: String?,
        type: Int,
        samplesPerChannel: Int,
        bytesPerSample: Int,
        channels: Int,
        samplesPerSec: Int,
        buffer: ByteBuffer?,
        renderTimeMs: Long,
        avsync_type: Int
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun onEarMonitoringAudioFrame(
        type: Int,
        samplesPerChannel: Int,
        bytesPerSample: Int,
        channels: Int,
        samplesPerSec: Int,
        buffer: ByteBuffer?,
        renderTimeMs: Long,
        avsync_type: Int
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun onPlaybackAudioFrameBeforeMixing(
        channelId: String?,
        userId: Int,
        type: Int,
        samplesPerChannel: Int,
        bytesPerSample: Int,
        channels: Int,
        samplesPerSec: Int,
        buffer: ByteBuffer?,
        renderTimeMs: Long,
        avsync_type: Int,
        rtpTimestamp: Int
    ): Boolean {
        return true
    }


    override fun getObservedAudioFramePosition(): Int {
        TODO("Not yet implemented")
    }

    override fun getRecordAudioParams(): AudioParams {
        LogUtils.d("getRecordAudioParams")
        return AudioParams(16000, 1, 0, 640)
    }

    override fun getPlaybackAudioParams(): AudioParams {
        TODO("Not yet implemented")
    }

    override fun getMixedAudioParams(): AudioParams {
        TODO("Not yet implemented")
    }

    override fun getEarMonitoringAudioParams(): AudioParams {
        TODO("Not yet implemented")
    }


    fun mute(enable: Boolean) {
        val ret = mRtcEngine?.enableLocalAudio(!enable)
        LogUtils.d("mute enable:$enable ret:$ret")
        if (SAVE_AUDIO_RECORD_PCM) {
            if (!enable) {
                val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                mTime = format.format(System.currentTimeMillis())
            }
        }
    }

    fun leaveChannel() {
        LogUtils.d("RtcManager leaveChannel")
        mRtcEngine?.registerAudioFrameObserver(null)
        mRtcEngine?.leaveChannel()
    }

    fun destroy() {
        LogUtils.d("RtcManager destroy")
        mChannelId = ""
        RtcEngine.destroy()
    }


    fun getChannelId(): String {
        if (mChannelId.isEmpty()) {
            mChannelId = Utils.getCurrentDateStr("yyyyMMddHHmmss") + Utils.getRandomString(2)
        }
        return mChannelId
    }

    fun getRtcEngine(): RtcEngine? {
        return mRtcEngine
    }

    interface RtcCallback {
        fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int)
        fun onLeaveChannel(stats: IRtcEngineEventHandler.RtcStats)
        fun onMuteSuccess() {

        }

        fun onUnMuteSuccess() {

        }

        fun onAudioVolumeIndication(
            speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
            totalVolume: Int
        ) {

        }

        fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray?) {

        }
    }
}
