package io.agora.aigc_cloud_demo.model

data class HistoryModel(
    var startTimestamp: String,
    var endTimestamp: String,
    var sid: String,
    var title: String,
    var message: String
) {
    constructor() : this(
        "", "",
        "", "", ""
    )
}
