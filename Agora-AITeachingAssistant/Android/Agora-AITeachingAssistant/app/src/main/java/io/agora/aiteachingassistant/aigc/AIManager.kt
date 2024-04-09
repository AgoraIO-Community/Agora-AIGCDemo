package io.agora.aiteachingassistant.aigc

import com.alibaba.fastjson.JSON
import io.agora.aiteachingassistant.model.ChatMessage
import io.agora.aiteachingassistant.model.TeacherLevel
import io.agora.aiteachingassistant.utils.LogUtils
import io.agora.aiteachingassistant.utils.Utils

object AIManager {
    private lateinit var mUserName: String
    private lateinit var mTeacherName: String
    private lateinit var chatPrompt: String
    private lateinit var tipPrompt: String
    private lateinit var translatePrompt: String
    private lateinit var polishPrompt: String
    private lateinit var endPrompt: String
    private lateinit var mChatStartMessage: String
    private var mTotalTokens = 0
    private var mChatListTokens = 0
    private var mPromptTokens = 1024
    private var mMaxTokens = 10000
    private var mExtraInfoJson: String = ""
    private var mLevelName: String = ""
    private var mModelName: String = ""
    private var mPreloadTipMessages: MutableList<String> = mutableListOf()
    private var mTopic: String = ""

    fun initialize(
        teacherLevel: TeacherLevel,
        tipPrompt: String,
        translatePrompt: String,
        polishPrompt: String,
        endPrompt: String,
        level: Int,
        model: String,
    ) {
        this.chatPrompt = teacherLevel.prompt
        this.tipPrompt = tipPrompt
        this.translatePrompt = translatePrompt
        this.polishPrompt = polishPrompt
        this.endPrompt = endPrompt
        mTotalTokens = 0
        mChatListTokens = 0
        mModelName = model
        mPreloadTipMessages = teacherLevel.preloadTipMessage
        mUserName = teacherLevel.userName
        mTeacherName = teacherLevel.teacherName
        mPromptTokens = teacherLevel.promptToken
        mLevelName = teacherLevel.level
        mExtraInfoJson = teacherLevel.llmExtraInfoJson
        mTopic = teacherLevel.topic

        val getChatMessageList = JSON.parseArray(chatPrompt, ChatMessage::class.java)
        mChatStartMessage =
            getReplaceMessages(getChatMessageList[getChatMessageList.size - 2].content)
    }

    fun getUserName(): String {
        return mUserName
    }

    fun getTeacherName(): String {
        return mTeacherName
    }

    fun getInitChatMessages(): String {
        val getChatMessageList = JSON.parseArray(chatPrompt, ChatMessage::class.java)
        return getReplaceMessages(JSON.toJSONString(getChatMessageList))
    }

    fun getChatMessages(
        chatMessageList: MutableList<ChatMessage>,
        exceededMaxTokens: Boolean = false
    ): String {
        LogUtils.d("getChatMessages chatMessageList: $chatMessageList")
        val getChatMessageList = JSON.parseArray(chatPrompt, ChatMessage::class.java)
        getChatMessageList.removeAt(getChatMessageList.size - 1)
        getChatMessageList.addAll(getChatMessageList.size - 1, chatMessageList)

        if (exceededMaxTokens) {
            getChatMessageList.add(JSON.parseObject(endPrompt, ChatMessage::class.java))
        }

        return getReplaceMessages(JSON.toJSONString(getChatMessageList))
    }

    fun getTipMessages(
        chatMessageList: MutableList<ChatMessage>,
        chatMessage: ChatMessage
    ): String {
        val getChatMessageList = JSON.parseArray(chatPrompt, ChatMessage::class.java)
        getChatMessageList.removeAt(getChatMessageList.size - 1)
        getChatMessageList.addAll(getChatMessageList.size - 1, chatMessageList)

        getChatMessageList.add(JSON.parseObject(tipPrompt, ChatMessage::class.java))

        return getReplaceMessages(
            JSON.toJSONString(getChatMessageList),
            Utils.removeChineseQuotes(chatMessage.content)
        )
    }

    fun getPolishMessages(
        chatMessageList: MutableList<ChatMessage>,
        chatMessage: ChatMessage
    ): String {
        val getChatMessageList = JSON.parseArray(chatPrompt, ChatMessage::class.java)
        getChatMessageList.removeAt(getChatMessageList.size - 1)
        getChatMessageList.addAll(getChatMessageList.size - 1, chatMessageList)

        getChatMessageList.add(JSON.parseObject(polishPrompt, ChatMessage::class.java))

        return getReplaceMessages(
            JSON.toJSONString(getChatMessageList),
            Utils.removeChineseQuotes(chatMessage.content)
        )
    }

    fun getTranslateMessages(
        chatMessageList: MutableList<ChatMessage>,
        content: String
    ): String {
        val translate = JSON.parseArray(translatePrompt, ChatMessage::class.java)
        return getReplaceMessages(
            JSON.toJSONString(translate),
            Utils.removeChineseQuotes(content)
        )
    }

    fun getUserTranslateMessage(
        content: String
    ): String {
        val translate = JSON.parseArray(translatePrompt, ChatMessage::class.java)
        return getReplaceMessages(
            JSON.toJSONString(translate),
            Utils.removeChineseQuotes(content), "中文"
        )
    }

    private fun getReplaceMessages(
        messages: String,
        lastResponse: String = "",
        language: String = "英文"
    ): String {
        return if (messages.isNotEmpty()) {
            messages.replace("{{user}}", mUserName)
                .replace("{{teacher}}", mTeacherName)
                .replace("{{last_response}}", lastResponse)
                .replace("{{language}}", language)
        } else {
            messages
        }
    }

    fun addChatListToken(tokens: Int, withPrompt: Boolean = true) {
        if (withPrompt) {
            mTotalTokens += mPromptTokens + mChatListTokens + tokens;
        } else {
            mTotalTokens += tokens
        }
        mChatListTokens += tokens
    }

    fun getTotalTokens(): Int {
        return mTotalTokens
    }

    fun getLlmExtraInfoJson(): String {
        return mExtraInfoJson
    }

    fun getLevelName(): String {
        return mLevelName
    }

    fun getChatStartMessage(): String {
        return mChatStartMessage
    }

    fun getModelName(): String {
        return mModelName
    }

    fun getPreloadTipMessages(): MutableList<String> {
        return mPreloadTipMessages
    }

    fun getTopic(): String {
        return mTopic
    }

    fun hasExceededMaxTokens(): Boolean {
        return mTotalTokens >= mMaxTokens
    }
}