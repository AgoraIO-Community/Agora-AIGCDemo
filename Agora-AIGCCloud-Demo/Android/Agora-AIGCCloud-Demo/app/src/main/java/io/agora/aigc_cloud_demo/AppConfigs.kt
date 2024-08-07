package io.agora.aigc_cloud_demo

import android.content.Context
import com.google.gson.Gson
import io.agora.aigc_cloud_demo.constants.Constants
import io.agora.aigc_cloud_demo.model.RtcConfigFeature
import io.agora.aigc_cloud_demo.model.RtcConfigFeatureParams
import io.agora.aigc_cloud_demo.model.ServerConfig
import io.agora.aigc_cloud_demo.utils.KeyCenter
import io.agora.aigc_cloud_demo.utils.LogUtils
import io.agora.aigc_cloud_demo.utils.Utils

object AppConfigs {
    private val mServerConfigs = mutableListOf<ServerConfig>()
    private var mCurrentServerConfig: ServerConfig? = null
    var mCurrentSttMode = Constants.STT_MODE_QUICK
    var mCurrentTtsSelect = ""
    var mCurrentLlmSelect = ""

    private val mRtcConfigs = mutableListOf<RtcConfigFeature>()
    private var mAinsFeatureParams: RtcConfigFeatureParams? = null

    val mInLanguage = mutableListOf<String>()
    val mOutLanguage = mutableListOf<String>()
    var mCurrentAppId = KeyCenter.APP_ID

    fun initConfigs(context: Context) {
        mServerConfigs.clear()
        val gson = Gson()
        gson.fromJson(
            Utils.readContentFromAsset(context, "server_configs.json"),
            Array<ServerConfig>::class.java
        ).forEach {
            mServerConfigs.add(it)
        }

        LogUtils.d("mServerConfigs: $mServerConfigs")

        mRtcConfigs.clear()
        gson.fromJson(
            Utils.readContentFromAsset(context, "rtc_configs.json"),
            Array<RtcConfigFeature>::class.java
        ).forEach {
            mRtcConfigs.add(it)
        }
        LogUtils.d("mRtcConfigs: $mRtcConfigs")
    }

    fun getServerConfigs(): List<ServerConfig> {
        return mServerConfigs
    }

    fun getCurrentServerConfig(): ServerConfig? {
        return mCurrentServerConfig
    }

    fun setCurrentServerConfig(serverConfig: ServerConfig) {
        mCurrentServerConfig = serverConfig
    }

    fun getRtcConfigs(): List<RtcConfigFeature> {
        return mRtcConfigs
    }

    fun getAinsFeatureParams(): RtcConfigFeatureParams? {
        return mAinsFeatureParams
    }

    fun setAinsFeatureParams(ainsFeatureParams: RtcConfigFeatureParams?) {
        mAinsFeatureParams = ainsFeatureParams
    }
}