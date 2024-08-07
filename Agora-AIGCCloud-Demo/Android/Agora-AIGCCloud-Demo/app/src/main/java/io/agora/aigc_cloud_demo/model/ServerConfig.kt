package io.agora.aigc_cloud_demo.model

data class ServerConfig(
    val regionIndex: Int,
    val regionName: String,
    val regionCode: String,
    val domain: String,
    val ttsSelect: MutableList<SelectItem>,
    val llmSelect: MutableList<SelectItem>
)
