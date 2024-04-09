package io.agora.aiteachingassistant.model

data class TeacherLevel(
    val level: String,
    val prompt: String,
    val preloadTipMessage: MutableList<String>,
    val userName: String,
    val teacherName: String,
    val promptToken: Int,
    val llmExtraInfoJson: String,
    val topic: String,
)
