package io.agora.aigc_cloud_demo.model

data class ConversationData(
    val conversationId: Int,
    var conversationStartTimestamp: Long = 0, var conversationEndTimestamp: Long = 0,
    var vadStartTimestamp: Long = 0, var vadEndTimestamp: Long = 0,
    var sttStartTimestamp: Long = 0, var sttEndTimestamp: Long = 0,
    var llmStartTimestamp: Long = 0, var llmEndTimestamp: Long = 0,
    var ttsStartTimestamp: Long = 0, var ttsEndTimestamp: Long = 0,
)
