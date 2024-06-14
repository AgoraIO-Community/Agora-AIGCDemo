package io.agora.aigc_cloud_demo.model

data class ItemData(
    val content: String,
    var bgResId: Int = 0,
    var success: Boolean = false,
    val nextTestCaseIndex: Int = -1

)
