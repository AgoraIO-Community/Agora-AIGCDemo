package io.agora.aiteachingassistant.constants

enum class FunctionType(val value: Int) {
    CHAT_INIT(1),
    CHAT(2),
    CHAT_EXCEEDED_MAX_TOKEN(3),
    POLISH(4),
    TIPS(5),
    TRANSLATE(6),
    TRANSLATE_USER_MESSAGE(7),
    END(8)

}