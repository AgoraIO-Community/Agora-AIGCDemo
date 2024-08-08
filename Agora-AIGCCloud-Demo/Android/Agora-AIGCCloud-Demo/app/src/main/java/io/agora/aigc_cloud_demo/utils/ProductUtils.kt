package io.agora.aigc_cloud_demo.utils

import io.agora.aigc_cloud_demo.BuildConfig
import io.agora.aigc_cloud_demo.constants.Constants

object ProductUtils {
    fun isTianGongProduct(): Boolean {
        return BuildConfig.PRODUCT_FLAVORS_TYPE == Constants.PRODUCT_FLAVORS_TIANGONG
    }
}