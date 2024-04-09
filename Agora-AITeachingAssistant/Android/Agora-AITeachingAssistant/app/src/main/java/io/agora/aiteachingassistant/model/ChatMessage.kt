package io.agora.aiteachingassistant.model

import com.alibaba.fastjson.annotation.JSONField

data class ChatMessage(
    val role: String = "",
    val name: String = "",
    var content: String = "",
    @JSONField(serialize = false, deserialize = false)
    val id: String = "",
    @JSONField(serialize = false, deserialize = false)
    var translateContent: String = "",
    @JSONField(serialize = false, deserialize = false)
    var requestMessage: Boolean = true
) {

}
