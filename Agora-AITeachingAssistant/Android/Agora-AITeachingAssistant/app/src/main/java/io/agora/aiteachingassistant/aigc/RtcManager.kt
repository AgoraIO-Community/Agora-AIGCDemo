package io.agora.aiteachingassistant.aigc

import android.content.Context
import io.agora.aigc.sdk.utils.RingBuffer
import io.agora.aiteachingassistant.utils.KeyCenter
import io.agora.aiteachingassistant.utils.LogUtils
import io.agora.aiteachingassistant.utils.Utils
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

/**
 *The provided code is written in Kotlin and is part of an Android application. It defines a singleton object `RtcManager` that manages the lifecycle and interactions with the Agora RTC (Real-Time Communication) engine, which is used for real-time audio communication.
 *
 * The `RtcManager` object implements the `IAudioFrameObserver` interface, which means it provides implementations for methods that handle various audio frame events from the RTC engine.
 *
 * The `initRtcEngine(context: Context)` function is used to initialize the RTC engine. It sets up the engine configuration, including the context, app ID, channel profile, event handler, and audio scenario. It also sets various parameters for the engine and enables audio.
 *
 * ```kotlin
 * fun initRtcEngine(context: Context) {
 *     // ...
 *     mRtcEngine = RtcEngine.create(rtcEngineConfig)
 *     // ...
 *     mRtcEngine?.enableAudio()
 *     // ...
 * }
 * ```
 *
 * The `onRecordAudioFrame(...)` function is called when an audio frame is recorded. It gets the audio data from the buffer, converts it to a byte array, and sends it to the AI service for speech-to-text processing.
 *
 * ```kotlin
 * override fun onRecordAudioFrame(
 *     // ...
 *     buffer: ByteBuffer?,
 *     // ...
 * ): Boolean {
 *     // ...
 *     AIGCServiceManager.pushVoicesToStt(origin)
 *     return true
 * }
 * ```
 *
 * The `onPlaybackAudioFrame(...)` function is called when an audio frame is played back. It gets the speech buffer and puts it into the buffer for playback.
 *
 * ```kotlin
 * override fun onPlaybackAudioFrame(
 *     // ...
 *     buffer: ByteBuffer?,
 *     // ...
 * ): Boolean {
 *     // ...
 *     buffer.put(bytes, 0, buffer.capacity())
 *     return true
 * }
 * ```
 *
 * The `mute(enable: Boolean)` function is used to mute or unmute the local audio.
 *
 * ```kotlin
 * fun mute(enable: Boolean) {
 *     // ...
 *     mRtcEngine?.enableLocalAudio(!enable)
 *     // ...
 * }
 * ```
 *
 * The `leaveChannel()` function is used to leave the RTC channel, clear the buffer, and unregister the audio frame observer.
 *
 * ```kotlin
 * fun leaveChannel() {
 *     // ...
 *     mRtcEngine?.leaveChannel()
 * }
 * ```
 *
 * Finally, the `destroy()` function is used to destroy the RTC engine.
 *
 * ```kotlin
 * fun destroy() {
 *     RtcEngine.destroy()
 * }
 * ```
 *
 * This code is part of a larger system that likely involves real-time audio communication and AI services for speech recognition and text-to-speech functionalities.
 */
object RtcManager : IAudioFrameObserver {
    private var mRtcEngine: RtcEngine? = null
    private var mCallback: RtcCallback? = null
    private var mRingBuffer: RingBuffer = RingBuffer(1024 * 4)
    private var mChannelId: String = ""
    private var mTime: String = ""
    private val SAVE_AUDIO_RECORD_PCM = false

    fun setCallback(callback: RtcCallback) {
        mCallback = callback
    }

    fun initRtcEngine(context: Context) {
        try {
            val rtcEngineConfig = RtcEngineConfig()
            rtcEngineConfig.mContext = context
            rtcEngineConfig.mAppId = KeyCenter.APP_ID
            rtcEngineConfig.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            rtcEngineConfig.mEventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    LogUtils.d("onJoinChannelSuccess channel:$channel uid:$uid elapsed:$elapsed")
                    mRtcEngine?.muteLocalAudioStream(true)
                    mute(true)
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
                        AIGCServiceManager.pushVoicesToStt(null, true)
                    }
                }

                override fun onAudioVolumeIndication(
                    speakers: Array<out AudioVolumeInfo>?,
                    totalVolume: Int
                ) {
                    super.onAudioVolumeIndication(speakers, totalVolume)
                    mCallback?.onAudioVolumeIndication(speakers, totalVolume)
                }
            }
            rtcEngineConfig.mAudioScenario = Constants.AUDIO_SCENARIO_GAME_STREAMING
            rtcEngineConfig.addExtension("agora_ai_noise_suppression_extension")
            mRtcEngine = RtcEngine.create(rtcEngineConfig)

            mRtcEngine?.setParameters("{\"rtc.enable_debug_log\":true}")

            mRtcEngine?.setParameters(
                """{"che.audio.enable.nsng":true,
                                "che.audio.ains_mode":2,
                                "che.audio.ns.mode":2,
                                "che.audio.nsng.lowerBound":80,
                                "che.audio.nsng.lowerMask":50,
                                "che.audio.nsng.statisticalbound":5,
                                "che.audio.nsng.finallowermask":30
                                }
                                """.trimIndent()
            )

            mRtcEngine?.setParameters("{\"che.audio.adm_android_mode\":9}")

            mRtcEngine?.enableAudio()
            mRtcEngine?.setAudioProfile(
                Constants.AUDIO_PROFILE_DEFAULT, Constants.AUDIO_SCENARIO_GAME_STREAMING
            )

            mRtcEngine?.setDefaultAudioRoutetoSpeakerphone(true)

            mRtcEngine?.setPlaybackAudioFrameParameters(
                16000,
                1,
                Constants.RAW_AUDIO_FRAME_OP_MODE_READ_WRITE,
                640
            )

            mRtcEngine?.setRecordingAudioFrameParameters(
                16000,
                1,
                Constants.RAW_AUDIO_FRAME_OP_MODE_READ_WRITE,
                640
            )

            //min 50ms
            mRtcEngine?.enableAudioVolumeIndication(
                50,
                3,
                true
            )

            mRtcEngine?.registerAudioFrameObserver(this@RtcManager)

            mChannelId = Utils.getCurrentDateStr("yyyyMMddHHmmss") + Utils.getRandomString(2)
            val ret = mRtcEngine?.joinChannel(
                KeyCenter.getRtcToken(
                    mChannelId,
                    KeyCenter.getUid()
                ),
                mChannelId,
                KeyCenter.getUid(),
                object : ChannelMediaOptions() {
                    init {
                        publishMicrophoneTrack = true
                        autoSubscribeAudio = true
                        clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    }
                })
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
        AIGCServiceManager.pushVoicesToStt(origin, false)
        if (SAVE_AUDIO_RECORD_PCM) {
            try {
                val fos = FileOutputStream(
                    "/sdcard/Android/Data/io.agora.aiteachingassistant/cache/audio_" + mTime + ".pcm",
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
        val bytes: ByteArray? = getSpeechBuffer(buffer!!.capacity())
        if (null != bytes) {
            buffer.put(bytes, 0, buffer.capacity())
        }

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
        TODO("Not yet implemented")
    }

    override fun getObservedAudioFramePosition(): Int {
        TODO("Not yet implemented")
    }

    override fun getRecordAudioParams(): AudioParams {
        TODO("Not yet implemented")
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
        clearBuffer()
        mRtcEngine?.registerAudioFrameObserver(null)
        mRtcEngine?.leaveChannel()
    }

    fun destroy() {
        LogUtils.d("RtcManager destroy")
        RtcEngine.destroy()
    }

    fun clearBuffer() {
        LogUtils.d("RtcManager clearBuffer")
        mRingBuffer.clear()
    }

    private fun getSpeechBuffer(length: Int): ByteArray? {
        if (mRingBuffer.size() < length) {
            return null
        }
        var o: Any
        val bytes = ByteArray(length)
        for (i in 0 until length) {
            o = mRingBuffer.take()
            bytes[i] = o as Byte
        }
        return bytes
    }

    fun pushVoices(voices: ByteArray) {
        for (ib in voices) {
            mRingBuffer.put(ib)
        }
    }

    fun getChannelId(): String {
        return mChannelId
    }
}
