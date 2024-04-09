package io.agora.aiteachingassistant.aigc

import android.content.Context
import io.agora.aigc.sdk.AIGCService
import io.agora.aigc.sdk.AIGCServiceCallback
import io.agora.aigc.sdk.AIGCServiceConfig
import io.agora.aigc.sdk.constants.HandleResult
import io.agora.aigc.sdk.constants.Language
import io.agora.aigc.sdk.constants.NoiseEnvironment
import io.agora.aigc.sdk.constants.STTMode
import io.agora.aigc.sdk.constants.ServiceCode
import io.agora.aigc.sdk.constants.ServiceEvent
import io.agora.aigc.sdk.constants.SpeechRecognitionCompletenessLevel
import io.agora.aigc.sdk.constants.SpeechState
import io.agora.aigc.sdk.constants.Vad
import io.agora.aigc.sdk.model.Data
import io.agora.aigc.sdk.model.SceneMode
import io.agora.aigc.sdk.model.ServiceVendor
import io.agora.aiteachingassistant.utils.KeyCenter
import io.agora.aiteachingassistant.utils.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The provided code is written in Kotlin and is part of an Android application. It defines a singleton object `AIGCServiceManager` that manages the lifecycle and interactions with an AI service, likely used for speech recognition and text-to-speech functionalities.
 *
 * The `AIGCServiceManager` object implements the `AIGCServiceCallback` interface, which means it provides implementations for methods that handle various events and results from the AI service.
 *
 * The `initAIGCService(context: Context)` function is used to initialize the AI service. It first checks if the service is already initialized, and if so, it logs a message and returns. If the service is not initialized, it creates an instance of `AIGCService` and initializes it with a configuration object. The configuration object sets various properties such as the context, callback, log settings, user information, and input/output settings.
 *
 * ```kotlin
 * fun initAIGCService(context: Context) {
 *     // ...
 *     mAIGCService = AIGCService.create()
 *     mAIGCService.initialize(object : AIGCServiceConfig() {
 *         // ...
 *     })
 * }
 * ```
 *
 * The `onEventResult(event: ServiceEvent, code: ServiceCode, msg: String?)` function is called when an event occurs in the AI service. It logs the event and handles specific events like `ServiceEvent.INITIALIZE` and `ServiceEvent.DESTROY`. For the `INITIALIZE` event, it sets the role, selects vendors for speech-to-text, language model, and text-to-speech services, and sets the service vendor. For the `DESTROY` event, it sets a flag indicating that the service is not initialized.
 *
 * ```kotlin
 * override fun onEventResult(event: ServiceEvent, code: ServiceCode, msg: String?) {
 *     // ...
 *     when (event) {
 *         ServiceEvent.INITIALIZE -> {
 *             // ...
 *         }
 *         ServiceEvent.DESTROY -> {
 *             // ...
 *         }
 *         // ...
 *     }
 * }
 * ```
 *
 * The `startChat()` and `stopChat()` functions are used to start and stop the AI service, respectively. They check if the service is initialized before calling the corresponding function on the service.
 *
 * ```kotlin
 * fun startChat() {
 *     if (mAIGCInitialized) {
 *         mAIGCService.start()
 *     }
 * }
 * ```
 *
 * The `pushVoicesToStt(voices: ByteArray)`, `pushTextToTts(text: String)`, and `pushMessagesToLlm(messagesJsonArray: String, extraInfoJson: String, roundId: String, interruptDialogue: Boolean)` functions are used to send data to the AI service. They also check if the service is initialized before sending the data.
 *
 * ```kotlin
 * fun pushVoicesToStt(voices: ByteArray) {
 *     if (mAIGCInitialized) {
 *         mAIGCService.pushSpeechDialogue(voices, Vad.UNKNOWN)
 *     }
 * }
 * ```
 *
 * Finally, the `destroy()` function is used to destroy the AI service.
 *
 * ```kotlin
 * fun destroy() {
 *     AIGCService.destroy()
 * }
 * ```
 *
 * This code is part of a larger system that likely involves real-time audio communication and AI services for speech recognition and text-to-speech functionalities.
 */
object AIGCServiceManager : AIGCServiceCallback {
    private lateinit var mAIGCService: AIGCService
    private var mAIGCInitialized = false
    private var mCallback: AIGCCallback? = null
    private var mPreTtsRoundId: String? = ""

    fun setCallback(callback: AIGCCallback) {
        mCallback = callback
    }

    fun initAIGCService(context: Context) {
        if (mAIGCInitialized) {
            val scope = CoroutineScope(Dispatchers.Main)
            scope.launch {

                withContext(Dispatchers.IO) {
                    onEventResult(
                        ServiceEvent.INITIALIZE,
                        ServiceCode.SUCCESS,
                        "AIGCService has been initialized"
                    )
                }
            }

            return
        }

        mAIGCService = AIGCService.create()
        mAIGCService.initialize(object : AIGCServiceConfig() {
            init {
                this.context = context
                callback = this@AIGCServiceManager
                enableLog = true
                enableSaveLogToFile = true
                userName = "AI"
                appId = KeyCenter.APP_ID
                rtmToken = KeyCenter.getRtmToken2(KeyCenter.randomUserUid)
                userId = KeyCenter.getUid().toString()
                enableMultiTurnShortTermMemory = false
                speechRecognitionFiltersLength = 0
                input = object : SceneMode() {
                    init {
                        language = Language.ZH_CN
                        speechFrameSampleRates = 16000
                        speechFrameChannels = 1
                        speechFrameBits = 16
                    }
                }
                output = object : SceneMode() {
                    init {
                        language = Language.ZH_CN
                        speechFrameSampleRates = 16000
                        speechFrameChannels = 1
                        speechFrameBits = 16
                    }
                }
                noiseEnvironment = NoiseEnvironment.NOISE
                speechRecognitionCompletenessLevel = SpeechRecognitionCompletenessLevel.NORMAL
            }
        })
        LogUtils.d("AIGCServiceSDK version:" + AIGCService.getVersion())
    }

    override fun onEventResult(event: ServiceEvent, code: ServiceCode, msg: String?) {
        super.onEventResult(event, code, msg)
        LogUtils.d("AIGCManager onEventResult event: $event, code: $code, msg: $msg")
        when (event) {
            ServiceEvent.INITIALIZE -> {
                if (ServiceCode.SUCCESS == code) {
                    mAIGCInitialized = true

                    mAIGCService.setRole("ai_teaching_assistant-zh-CN")
                    val serviceVendor = ServiceVendor()
                    for (sttVendor in mAIGCService.serviceVendors.sttList) {
                        if (sttVendor.id == "microsoft") {
                            serviceVendor.sttVendor = sttVendor
                            break
                        }
                    }

                    for (llmVendor in mAIGCService.serviceVendors.llmList) {
                        if (AIManager.getModelName() == io.agora.aiteachingassistant.constants.Constants.LLM_MODEL_GPT_35) {
                            if (llmVendor.id == "azureOpenai-gpt-35-turbo-16k") {
                                serviceVendor.llmVendor = llmVendor
                                break
                            }
                        } else if (AIManager.getModelName() == io.agora.aiteachingassistant.constants.Constants.LLM_MODEL_GPT_4) {
                            if (llmVendor.id == "azureOpenai-gpt-4") {
                                serviceVendor.llmVendor = llmVendor
                                break
                            }
                        }
                    }

                    for (ttsVendor in mAIGCService.serviceVendors.ttsList) {
                        if (ttsVendor.id == "microsoft-zh-CN-xiaoxiao-cheerful-female") {
                            serviceVendor.ttsVendor = ttsVendor
                            break
                        }
                    }

                    mAIGCService.setServiceVendor(serviceVendor)

                    mAIGCService.setSTTMode(STTMode.CONTROLLED)
                }
            }

            ServiceEvent.DESTROY -> {
                if (ServiceCode.SUCCESS == code) {
                    mAIGCInitialized = false
                }
            }

            else -> {
                // do nothing
            }
        }

        mCallback?.onEventResult(event, code, msg)
    }


    override fun onSpeech2TextResult(
        roundId: String?,
        result: Data<String>?,
        isRecognizedSpeech: Boolean,
        code: ServiceCode
    ): HandleResult {
        LogUtils.d("AIGCManager onSpeech2TextResult roundId: $roundId, result: $result, isRecognizedSpeech: $isRecognizedSpeech, code: $code")
        mCallback?.onSpeech2TextResult(roundId, result, isRecognizedSpeech, code)
        return HandleResult.DISCARD
    }

    override fun onLLMResult(
        roundId: String?,
        answer: Data<String>?,
        isRoundEnd: Boolean,
        estimatedResponseTokens: Int,
        code: ServiceCode
    ): HandleResult? {
        LogUtils.d("AIGCManager onLLMResult roundId: $roundId, answer: $answer, isRoundEnd: $isRoundEnd, estimatedResponseTokens: $estimatedResponseTokens, code: $code")
        return mCallback?.onLLMResult(roundId, answer, isRoundEnd, estimatedResponseTokens, code)
    }

    override fun onText2SpeechResult(
        roundId: String?,
        voice: Data<ByteArray>?,
        sampleRates: Int,
        channels: Int,
        bits: Int,
        code: ServiceCode
    ): HandleResult {
        LogUtils.d("AIGCManager onText2SpeechResult roundId: $roundId, voice: $voice, sampleRates: $sampleRates, channels: $channels, bits: $bits, code: $code")
        if (mPreTtsRoundId != null && mPreTtsRoundId != roundId) {
            RtcManager.clearBuffer()
        }
        mPreTtsRoundId = roundId

        val voices = voice!!.data
        RtcManager.pushVoices(voices)

        return HandleResult.CONTINUE
    }

    override fun onSpeechStateChange(state: SpeechState?) {
        super.onSpeechStateChange(state)
//        if (SpeechState.START == state) {
//            interrupt()
//        }
    }

    fun startChat() {
        LogUtils.d("AIGCManager startChat")
        if (mAIGCInitialized) {
            mAIGCService.start()
        }
    }

    fun stopChat() {
        LogUtils.d("AIGCManager stopChat")
        if (mAIGCInitialized) {
            mAIGCService.stop()
        }
    }

    fun pushVoicesToStt(voices: ByteArray?, isLastFrame: Boolean) {
        if (mAIGCInitialized) {
            mAIGCService.pushSpeechDialogue(voices, Vad.UNKNOWN, isLastFrame)
        }
    }

    fun pushTextToTts(text: String, roundId: String) {
        LogUtils.d("AIGCManager pushTextToTts text: $text")
        if (mAIGCInitialized) {
            mAIGCService.pushTxtToTTS(text, roundId)
        }
    }

    fun pushMessagesToLlm(
        messagesJsonArray: String,
        extraInfoJson: String,
        roundId: String
    ) {
        if (mAIGCInitialized) {
            mAIGCService.pushMessagesToLLM(
                messagesJsonArray,
                extraInfoJson,
                roundId
            )
        }
    }

    fun pushTxtDialogue(text: String, roundId: String) {
        if (mAIGCInitialized) {
            mAIGCService.pushTxtDialogue(text, roundId)
        }
    }

    fun interrupt(services: Int) {
        if (mAIGCInitialized) {
            mAIGCService.interrupt(services)
        }
    }


    fun destroy() {
        AIGCService.destroy()
    }
}