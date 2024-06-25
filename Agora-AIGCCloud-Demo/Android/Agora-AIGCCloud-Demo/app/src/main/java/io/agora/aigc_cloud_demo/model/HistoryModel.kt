package io.agora.aigc_cloud_demo.model

data class HistoryModel(var date: String, var sid: String, var title: String, var message: String) {
    constructor() : this("", "", "", "")
}
