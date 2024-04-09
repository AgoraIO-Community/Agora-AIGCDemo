package io.agora.aiteachingassistant.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import io.agora.aigc.sdk.constants.HandleResult
import io.agora.aigc.sdk.constants.ServiceCode
import io.agora.aigc.sdk.constants.ServiceEvent
import io.agora.aigc.sdk.model.Data
import io.agora.aiteachingassistant.R
import io.agora.aiteachingassistant.aigc.AIGCCallback
import io.agora.aiteachingassistant.aigc.AIManager
import io.agora.aiteachingassistant.aigc.AIGCServiceManager
import io.agora.aiteachingassistant.aigc.RtcCallback
import io.agora.aiteachingassistant.aigc.RtcManager
import io.agora.aiteachingassistant.constants.Constants
import io.agora.aiteachingassistant.constants.FunctionType
import io.agora.aiteachingassistant.databinding.ActivityTeachingAssistantBinding
import io.agora.aiteachingassistant.model.ChatMessage
import io.agora.aiteachingassistant.ui.adapter.ChatMessageAdapter
import io.agora.aiteachingassistant.ui.adapter.PreloadMessagesAdapter
import io.agora.aiteachingassistant.utils.LogUtils
import io.agora.aiteachingassistant.utils.ToastUtils
import io.agora.aiteachingassistant.utils.Utils
import io.agora.rtc2.IRtcEngineEventHandler
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class AITeachingAssistantActivity : AppCompatActivity(), ChatMessageAdapter.OnItemClickListener,
    AIGCCallback, RtcCallback {
    private val TAG: String = Constants.TAG + "-AITeachingAssistantActivity"
    private lateinit var binding: ActivityTeachingAssistantBinding
    private var mSpeakVoice: Boolean = true
    private var mIsSpeaking: Boolean = false
    private val mChatMessageList: MutableList<ChatMessage> = mutableListOf()
    private var mChatMessageAdapter: ChatMessageAdapter? = null
    private var mChatLayoutManager: LinearLayoutManager? = null

    private var mExecutorCacheService: ExecutorService? = null
    private var mVoiceSttRoundId: String = ""
    private var mSttPending: StringBuilder = StringBuilder()
    private var mSttTranslatePending: StringBuilder = StringBuilder()
    private val mRequestRoundIdMap: MutableMap<String, FunctionType> = mutableMapOf()

    private val mHandler = Handler(Looper.getMainLooper(), Handler.Callback {
        true
    });


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeachingAssistantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        initView()
        initClick()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        initData()
    }

    private fun initData() {
        mExecutorCacheService = ThreadPoolExecutor(
            Int.MAX_VALUE,
            Int.MAX_VALUE,
            0,
            TimeUnit.SECONDS,
            LinkedBlockingDeque(),
            Executors.defaultThreadFactory(),
            ThreadPoolExecutor.AbortPolicy()
        )

        mRequestRoundIdMap.clear()

        AIGCServiceManager.setCallback(this)
        RtcManager.setCallback(this)

        AIGCServiceManager.initAIGCService(this)
    }

    private fun initView() {
        handleOnBackPressed()
        setupChatMessageView()
        updateSpeakTypeUI()
        updateTokensView()

        binding.toolbarTitle.text = String.format(
            getString(R.string.level),
            AIManager.getLevelName() + "(" + AIManager.getModelName() + ")" + "(" + AIManager.getTopic() + ")"
        )

    }

    private fun handleOnBackPressed() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                gotoMainActivity()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }


    private fun setupChatMessageView() {
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)

        mChatMessageAdapter = ChatMessageAdapter(mChatMessageList)
        mChatMessageAdapter?.setOnItemClickListener(this@AITeachingAssistantActivity)
        binding.chatRecyclerView.adapter = mChatMessageAdapter
        binding.chatRecyclerView.addItemDecoration(
            ChatMessageAdapter.ItemDecoration(
                resources.getDimensionPixelSize(
                    R.dimen.item_space
                )
            )
        )
        mChatLayoutManager = binding.chatRecyclerView.layoutManager as LinearLayoutManager

        val preloadMessageList = AIManager.getPreloadTipMessages()
        LogUtils.d(TAG, "preloadTipMessageList : $preloadMessageList")

        val preloadMessageArray: Array<String> =
            preloadMessageList.toTypedArray()

        binding.preloadMessageRv.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        val preloadMessageAdapter = PreloadMessagesAdapter(preloadMessageArray)
        preloadMessageAdapter.setOnItemClickListener(object :
            PreloadMessagesAdapter.OnItemClickListener {
            override fun onItemClick(message: String) {
                LogUtils.d("select preload message : $message")
                AIGCServiceManager.interrupt(io.agora.aigc.sdk.constants.Constants.SERVICE_LLM or io.agora.aigc.sdk.constants.Constants.SERVICE_TTS)
                val roundId = Utils.getUuidId();
                if (Utils.onlyChinese(message)) {
                    sendChatMessage(roundId, message);
                } else {
                    translateUserMessage(roundId, message)
                }
            }
        })
        binding.preloadMessageRv.adapter = preloadMessageAdapter
        binding.preloadMessageRv.addItemDecoration(
            PreloadMessagesAdapter.MyItemDecoration(
                resources.getDimensionPixelSize(
                    R.dimen.item_space
                )
            )
        )

    }

    private fun updateSpeakTypeUI() {
        if (mSpeakVoice) {
            binding.speakInputIv.setBackgroundResource(R.drawable.keyboard)
            binding.btnSpeak.visibility = View.VISIBLE
            binding.textInputEt.visibility = View.GONE
            binding.btnSend.visibility = View.GONE
            Utils.hideKeyboard(this, binding.btnSpeak)
        } else {
            binding.speakInputIv.setBackgroundResource(R.drawable.voice)
            binding.textInputEt.visibility = View.VISIBLE
            binding.btnSpeak.visibility = View.GONE
            binding.btnSend.visibility = View.VISIBLE
        }
        binding.btnPolish.visibility = View.GONE
        binding.textInputEt.text.clear()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initClick() {
        binding.toolbarBack.setOnClickListener {
            gotoMainActivity()
        }
        binding.toolbarBackTitle.setOnClickListener {
            gotoMainActivity()
        }


        binding.btnSpeak.setOnLongClickListener {
//            mIsSpeaking = true;
//            mSttPending.clear()
//            mVoiceSttRoundId = Utils.getUuidId();
//            RtcManager.mute(false)
            true
        }

        binding.btnSpeak.setOnTouchListener(View.OnTouchListener { v, event ->
            val action: Int = event.action
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!mIsSpeaking) {
                        LogUtils.d("start speak")
                        mIsSpeaking = true
                        RtcManager.mute(false)
                        mSttPending.clear()
                        mVoiceSttRoundId = Utils.getUuidId();
                    }
                    false
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    runOnUiThread {
                        binding.btnSpeak.setText(R.string.start_speak)
                        binding.btnSpeak.setBackgroundResource(R.drawable.button_background_not_pressed)
                    }

                    if (mIsSpeaking) {
                        mHandler.postDelayed({
                            LogUtils.d("stop speak")
                            mIsSpeaking = false
                            RtcManager.mute(true)

                            if (mSttPending.isNotEmpty()) {
                                LogUtils.d(
                                    "sttPending is all chinese: ${
                                        Utils.onlyChinese(
                                            mSttPending.toString()
                                        )
                                    }"
                                )

                                if (Utils.onlyChinese(mSttPending.toString())) {
                                    sendChatMessage(
                                        mVoiceSttRoundId + "stt",
                                        mSttPending.toString(),
                                        false
                                    )
                                } else {
                                    translateUserMessage(
                                        mVoiceSttRoundId,
                                        mSttPending.toString(),
                                        false
                                    )
                                }
                                mSttPending.clear()
                            }
                        }, 400)
                    }
                    false
                }

                else -> false
            }
        })

        binding.speakInputIv.setOnClickListener {
            mSpeakVoice = !mSpeakVoice
            updateSpeakTypeUI()
        }

//        binding.textInputEt.setOnEditorActionListener() { v, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_NULL) {
//                val message = binding.textInputEt.text.toString()
//                sendChatMessage(Utils.getUuidId(), message);
//            }
//            true
//        }

        binding.textInputEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty()) {
                    binding.btnPolish.visibility = View.VISIBLE
                    binding.btnSend.isEnabled = true
                } else {
                    binding.btnPolish.visibility = View.GONE
                    binding.btnSend.isEnabled = false
                }
            }
        })

        binding.btnPolish.setOnClickListener {
            val roundId = Utils.getUuidId()
            val polishMessages = AIManager.getPolishMessages(
                getRequestChatMessageList(),
                ChatMessage(
                    Constants.LLM_ROLE_USER,
                    AIManager.getUserName(),
                    binding.textInputEt.text.toString(),
                    roundId,
                    ""
                )
            )
            if (polishMessages.isNotEmpty()) {
                LogUtils.d("btnPolish polishMessages : $polishMessages")
                mRequestRoundIdMap[roundId] = FunctionType.POLISH
                binding.textInputEt.setText("")
                sendMessagesToLlm(roundId, polishMessages)
            }
        }

        binding.btnSend.setOnClickListener {
            val message = binding.textInputEt.text.toString()
            if (message.isNotEmpty() && message.isNotBlank()) {
                AIGCServiceManager.interrupt(io.agora.aigc.sdk.constants.Constants.SERVICE_LLM or io.agora.aigc.sdk.constants.Constants.SERVICE_TTS)
                val roundId = Utils.getUuidId();
                if (Utils.onlyChinese(message)) {
                    sendChatMessage(roundId, message);
                } else {
                    translateUserMessage(roundId, message)
                }
            } else {
                binding.textInputEt.setText("")
                ToastUtils.showLongToast(this, "发送内容不能为空")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
    }

    private fun gotoMainActivity() {
        AIGCServiceManager.stopChat()
        RtcManager.leaveChannel()
        reset()

        startActivity(
            Intent(
                this@AITeachingAssistantActivity,
                MainActivity::class.java
            ).apply {
                //putExtra(Constants.EXTRA_KEY_TEACHER_LEVEL_PROMPT, teacherLevelPrompt)
            })
    }

    private fun reset() {
        mChatMessageList.clear()
        mChatMessageAdapter?.notifyDataSetChanged()
    }

    private fun handleLlmResult(
        roundId: String, answer: Data<String>?, isRoundEnd: Boolean,
        estimatedResponseTokens: Int, functionType: FunctionType
    ): HandleResult {
        val llmResponseContent = answer?.data ?: ""
        when (functionType) {
            FunctionType.CHAT_INIT -> {
                LogUtils.d("handleLlmResult chat init : $llmResponseContent,roundId: $roundId, isRoundEnd: $isRoundEnd")
                runOnUiThread {
                    insertChatMessage(roundId + "chat_init", llmResponseContent, "", true, true)
                }
                answer?.data?.let {
                    if (it.isNotEmpty()) {
                        answer.data = Utils.removeChineseQuotes(llmResponseContent)
                    }
                }
                return HandleResult.CONTINUE
            }

            FunctionType.CHAT -> {
                LogUtils.d("handleLlmResult chat : $llmResponseContent,roundId: $roundId, isRoundEnd: $isRoundEnd")
                runOnUiThread {
                    insertChatMessage(roundId, llmResponseContent, "", true, true)
                    if (isRoundEnd) {
                        if (AIManager.hasExceededMaxTokens()) {
                            val chatMessages =
                                AIManager.getChatMessages(getRequestChatMessageList(), true)
                            val requestRoundId = Utils.getUuidId()
                            mRequestRoundIdMap[requestRoundId] =
                                FunctionType.CHAT_EXCEEDED_MAX_TOKEN
                            sendMessagesToLlm(requestRoundId, chatMessages)
                        }
                    }
                }
                answer?.data?.let {
                    if (it.isNotEmpty()) {
                        answer.data = Utils.removeChineseQuotes(llmResponseContent)
                    }
                }
                return HandleResult.CONTINUE
            }

            FunctionType.CHAT_EXCEEDED_MAX_TOKEN -> {
                LogUtils.d("handleLlmResult chat exceeded max token : $llmResponseContent,roundId: $roundId, isRoundEnd: $isRoundEnd")
                runOnUiThread {
                    insertChatMessage(roundId, llmResponseContent, "", true, true)
                }
                answer?.data?.let {
                    if (it.isNotEmpty()) {
                        answer.data = Utils.removeChineseQuotes(llmResponseContent)
                    }
                }
                return HandleResult.CONTINUE
            }

            FunctionType.POLISH -> {
                LogUtils.d("handleLlmResult polish : $llmResponseContent")
                runOnUiThread {
                    binding.textInputEt.setText(Utils.removeChineseQuotes(binding.textInputEt.text.toString() + llmResponseContent))
                }
                return HandleResult.DISCARD
            }

            FunctionType.TIPS -> {
                LogUtils.d("handleLlmResult tips : $llmResponseContent")
                runOnUiThread {
                    if (mSpeakVoice) {
                        mSpeakVoice = false
                        updateSpeakTypeUI()
                    }
                    binding.textInputEt.setText(Utils.removeChineseQuotes(binding.textInputEt.text.toString() + llmResponseContent))
                }

                return HandleResult.DISCARD
            }

            FunctionType.TRANSLATE -> {
                LogUtils.d("handleLlmResult translate : $llmResponseContent")
                if (llmResponseContent.isNotEmpty()) {
                    runOnUiThread {
                        insertChatMessage(
                            roundId,
                            "",
                            llmResponseContent,
                            true, false, true
                        )
                    }
                }
                return HandleResult.DISCARD
            }

            FunctionType.TRANSLATE_USER_MESSAGE -> {
                LogUtils.d("handleLlmResult stt translate : $llmResponseContent,roundId: $roundId, isRoundEnd: $isRoundEnd")
                if (llmResponseContent.isNotEmpty()) {
                    var content = llmResponseContent
                    if (mSttTranslatePending.isEmpty()) {
                        content = "你想说的应该是:$llmResponseContent"
                    }
                    mSttTranslatePending.append(llmResponseContent)

                    runOnUiThread {
                        insertChatMessage(
                            roundId + "_translate_user_message",
                            content,
                            "",
                            true,
                            true,
                            insertRequestChatMessageList = false
                        )
                    }
                    answer?.data?.let {
                        if (it.isNotEmpty()) {
                            answer.data = Utils.removeChineseQuotes(llmResponseContent)
                        }
                    }
                }
                if (isRoundEnd) {
                    LogUtils.d("handleLlmResult stt translate complete : $mSttTranslatePending")
                    if (mChatMessageList.size >= 2) {
                        val userChatMessage = mChatMessageList[mChatMessageList.size - 2];
                        if (userChatMessage.role == Constants.LLM_ROLE_USER) {
                            userChatMessage.translateContent = mSttTranslatePending.toString()
                        }
                    }
                    sendChatMessage(roundId, mSttTranslatePending.toString(), false)
                }
                return HandleResult.DISCARD
            }

            FunctionType.END -> {
                LogUtils.d("handleLlmResult end : $llmResponseContent")
                return HandleResult.CONTINUE
            }

            else -> {
                LogUtils.d("handleLlmResult default : $llmResponseContent")
                return HandleResult.DISCARD
            }
        }
    }

    private fun updateLlmResultTokens(
        estimatedResponseTokens: Int, functionType: FunctionType
    ) {
        when (functionType) {
            FunctionType.CHAT -> {
                AIManager.addChatListToken(estimatedResponseTokens * 2)
                updateTokensView()
            }

            FunctionType.POLISH, FunctionType.TIPS, FunctionType.END -> {
//                AIGCManager.addChatListToken(estimatedResponseTokens * 2)
//                updateTokensView()
            }

            FunctionType.TRANSLATE -> {
//                AIGCManager.addChatListToken(estimatedResponseTokens * 2, false)
            }

            else -> {
            }
        }
    }

    override fun onTipClick(chatMessage: ChatMessage) {
        LogUtils.d("onTipClick : ${chatMessage.toString()}")
        val tipMessages = AIManager.getTipMessages(getRequestChatMessageList(), chatMessage)
        if (tipMessages.isNotEmpty()) {
            runOnUiThread { binding.textInputEt.setText("") }
            LogUtils.d("onTipClick tipMessages : $tipMessages")
            val roundId = Utils.getUuidId()
            mRequestRoundIdMap[roundId] = FunctionType.TIPS
            sendMessagesToLlm(roundId, tipMessages)
        }
    }

    override fun onTranslateClick(chatMessage: ChatMessage) {
        LogUtils.d("onTranslateClick : ${chatMessage.toString()}")
        chatMessage.translateContent = ""
        val translateMessages =
            AIManager.getTranslateMessages(getRequestChatMessageList(), chatMessage.content)
        if (translateMessages.isNotEmpty()) {
            LogUtils.d("onTranslateClick translateMessages : $translateMessages")
            val roundId = chatMessage.id
            mRequestRoundIdMap[roundId] = FunctionType.TRANSLATE
            sendMessagesToLlm(roundId, translateMessages)
        }
    }

    private fun insertChatMessage(
        id: String,
        message: String,
        translateContent: String,
        isAI: Boolean,
        isMessageAppend: Boolean,
        isTranslateAppend: Boolean = false,
        insertRequestChatMessageList: Boolean = true
    ) {
        LogUtils.d("insertChatMessage id : $id, message : $message, translateContent : $translateContent, isAI : $isAI, isMessageAppend : $isMessageAppend")
        var position = 0

        var existChatMessage: ChatMessage? = null
        for (chatMessage in mChatMessageList) {
            if (id == chatMessage.id) {
                existChatMessage = chatMessage
                break
            }
            position++
        }
        if (existChatMessage == null) {
            val chatMessage = ChatMessage(
                if (isAI) Constants.LLM_ROLE_ASSISTANT else Constants.LLM_ROLE_USER,
                if (isAI) AIManager.getTeacherName() else AIManager.getUserName(),
                if (isAI) message else Utils.addChineseQuotes(message),
                id,
                translateContent, insertRequestChatMessageList
            )
            mChatMessageList.add(
                chatMessage
            )
            position = mChatMessageList.size - 1
            mChatMessageAdapter?.notifyItemInserted(position)

        } else {
            if (translateContent.isNotEmpty()) {
                if (isTranslateAppend) {
                    existChatMessage.translateContent += translateContent
                } else {
                    existChatMessage.translateContent = translateContent
                }
            }
            if (message.isNotEmpty()) {
                if (isMessageAppend) {
                    if (isAI) {
                        existChatMessage.content += message
                    } else {
                        existChatMessage.content =
                            Utils.addChineseQuotes(Utils.removeChineseQuotes(existChatMessage.content) + message)
                    }
                } else {
                    if (isAI) {
                        existChatMessage.content = message
                    } else {
                        existChatMessage.content = Utils.addChineseQuotes(message)
                    }
                }
            }
            mChatMessageAdapter?.notifyItemChanged(position);
        }
        //mChatMessageAdapter?.notifyDataSetChanged()
        mChatLayoutManager?.scrollToPosition(position)
    }

    override fun onEventResult(event: ServiceEvent, code: ServiceCode, msg: String?) {
        super.onEventResult(event, code, msg)
        while (true) {
            when (event) {
                ServiceEvent.INITIALIZE -> {
                    if (ServiceCode.SUCCESS == code) {
                        LogUtils.d("AITeachingAssistant onEventResult INITIALIZE SUCCESS")
                        AIGCServiceManager.startChat()
                    }
                }

                ServiceEvent.START -> {
                    if (ServiceCode.SUCCESS == code) {
                        LogUtils.d("AITeachingAssistant onEventResult START SUCCESS")
                        runOnUiThread {
//                            insertChatMessage(
//                                Utils.getUuidId(),
//                                AIGCManager.getChatStartMessage(),
//                                "",
//                                true,
//                                false,
//                                insertRequestChatMessageList = false
//                            )
                            RtcManager.initRtcEngine(this@AITeachingAssistantActivity.applicationContext)
                        }
                    }
                }

                else -> {
                    LogUtils.d("AITeachingAssistant onEventResult event: $event, code: $code, msg: $msg")
                }
            }
            break
        }
    }

    override fun onSpeech2TextResult(
        roundId: String?,
        result: Data<String>?,
        isRecognizedSpeech: Boolean,
        code: ServiceCode
    ): HandleResult {
        if (ServiceCode.SUCCESS == code) {
            if (roundId != null) {
                val sttRoundId = mVoiceSttRoundId + "stt"
                runOnUiThread {
                    insertChatMessage(
                        sttRoundId + "user_message",
                        (mSttPending.toString() + result?.data) ?: "", "", false, false
                    )

                    if (isRecognizedSpeech) {
                        mSttPending.append(result?.data ?: "")
                        if (!mIsSpeaking) {
                            if (Utils.onlyChinese(mSttPending.toString())) {
                                sendChatMessage(sttRoundId, mSttPending.toString(), false)
                            } else {
                                translateUserMessage(
                                    mVoiceSttRoundId,
                                    mSttPending.toString(),
                                    false
                                )
                            }

                            mSttPending.clear()
                        }
                    }

                }
            }
        } else {
            LogUtils.d("onSpeech2TextResult code : $code")
            runOnUiThread {
                ToastUtils.showLongToast(
                    this,
                    "STT语音识别错误: ${code.code}-${code.message} 请重试"
                )
            }
        }
        return HandleResult.DISCARD
    }

    override fun onLLMResult(
        roundId: String?,
        answer: Data<String>?,
        isRoundEnd: Boolean,
        estimatedResponseTokens: Int,
        code: ServiceCode
    ): HandleResult {
        if (ServiceCode.SUCCESS == code) {
            if (roundId != null) {
                if (isRoundEnd) {
                    updateLlmResultTokens(
                        estimatedResponseTokens, mRequestRoundIdMap[roundId] ?: FunctionType.CHAT
                    )
                }
                val handleLlmResult = handleLlmResult(
                    roundId,
                    answer,
                    isRoundEnd,
                    estimatedResponseTokens, mRequestRoundIdMap[roundId] ?: FunctionType.CHAT
                )
                if (isRoundEnd) {
                    mRequestRoundIdMap.remove(roundId)
                }
                return handleLlmResult
            }
        } else {
            LogUtils.d("onLLMResult code : ${code.code},message: ${code.message}")
            mChatMessageList[mChatMessageList.size - 1].requestMessage = false
            runOnUiThread {
                ToastUtils.showLongToast(
                    this,
                    "AI错误: ${code.code}-${code.message} 请重试"
                )
            }
        }
        return HandleResult.DISCARD
    }

    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        runOnUiThread {
            binding.channelIdTv.text = String.format(
                getString(R.string.channel_id),
                RtcManager.getChannelId()
            )
        }
        //AIGCServiceManager.pushTextToTts(Utils.removeChineseQuotes(AIGCManager.getChatStartMessage()))
        val chatMessages = AIManager.getInitChatMessages()
        val roundId = Utils.getUuidId()
        mRequestRoundIdMap[roundId] = FunctionType.CHAT_INIT
        sendMessagesToLlm(roundId, chatMessages)
    }

    override fun onLeaveChannel(stats: IRtcEngineEventHandler.RtcStats) {
        runOnUiThread { RtcManager.destroy() }
    }

    private fun sendMessagesToLlm(roundId: String, messages: String) {
        AIGCServiceManager.pushMessagesToLlm(
            messages,
            AIManager.getLlmExtraInfoJson(), roundId
        )
    }

    private fun sendChatMessage(
        roundId: String,
        message: String,
        updateChatMessage: Boolean = true
    ) {
        if (message.isNotEmpty()) {
            mRequestRoundIdMap[roundId] = FunctionType.CHAT
            if (updateChatMessage) {
                runOnUiThread {
                    insertChatMessage(
                        roundId + "user_message",
                        message,
                        "",
                        false,
                        false
                    )
                }
            }
            val chatMessages = AIManager.getChatMessages(getRequestChatMessageList())
            sendMessagesToLlm(roundId, chatMessages)

            binding.textInputEt.setText("")
        }
    }

    private fun updateTokensView() {
        runOnUiThread {
            binding.tokensTv.text =
                String.format(getString(R.string.tokens), AIManager.getTotalTokens())
        }
    }

    override fun onUnMuteSuccess() {
        LogUtils.d("btnSpeak onUnMuteSuccess")
        if (mIsSpeaking) {
            //只是为了中断对话
            AIGCServiceManager.interrupt(io.agora.aigc.sdk.constants.Constants.SERVICE_TTS)
            mHandler.postDelayed({
                runOnUiThread {
                    if (mIsSpeaking) {
                        binding.btnSpeak.setText(R.string.stop_speak)
                        //binding.btnSpeak.setBackgroundResource(R.drawable.button_background_pressed)

                        val animation = ObjectAnimator.ofInt(
                            binding.btnSpeak,
                            "backgroundResource",
                            R.drawable.button_background_not_pressed,
                            R.drawable.button_background_pressed
                        )
                        animation.duration = 50
                        animation.start()
                    }
                }
            }, 200)
        }
    }

    override fun onMuteSuccess() {
        LogUtils.d("btnSpeak onMuteSuccess")
        runOnUiThread {
            binding.btnSpeak.setText(R.string.start_speak)
            binding.btnSpeak.setBackgroundResource(R.drawable.button_background_not_pressed)
        }
    }

    private fun translateUserMessage(
        roundId: String,
        message: String,
        updateChatMessage: Boolean = true
    ) {
        LogUtils.d("translateUserMessage message : $message, roundId: $roundId,updateChatMessage: $updateChatMessage")
        if (updateChatMessage) {
            runOnUiThread { insertChatMessage(roundId + "user_message", message, "", false, false) }
        }

        val userMessageTranslateMessages = AIManager.getUserTranslateMessage(
            message
        )
        if (userMessageTranslateMessages.isNotEmpty()) {
            mSttTranslatePending.clear()
            LogUtils.d("stt translateMessages : $userMessageTranslateMessages")
            mRequestRoundIdMap[roundId] = FunctionType.TRANSLATE_USER_MESSAGE
            sendMessagesToLlm(roundId, userMessageTranslateMessages)
        }
    }

    private fun getRequestChatMessageList(): MutableList<ChatMessage> {
        val requestChatMessageList: MutableList<ChatMessage> =
            mChatMessageList.map { it.copy() }.toMutableList()
        val iterator = requestChatMessageList.listIterator()

        // remove the chat message which is not request message
        while (iterator.hasNext()) {
            val chatMessage = iterator.next()
            if (chatMessage.requestMessage) {
                if (chatMessage.translateContent.isNotEmpty() && chatMessage.translateContent.isNotBlank() && chatMessage.role == Constants.LLM_ROLE_USER) {
                    chatMessage.content = Utils.addChineseQuotes(chatMessage.translateContent)
                }
            } else {
                iterator.remove()
            }
        }

        return requestChatMessageList
    }

    override fun onAudioVolumeIndication(
        speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
        totalVolume: Int
    ) {
        for (speaker in speakers!!) {
            if (speaker.uid == 0 && speaker.volume > 0) {
                LogUtils.d("onAudioVolumeIndication speaker : ${speaker.uid},volume: ${speaker.volume}")
            }
        }
    }
}