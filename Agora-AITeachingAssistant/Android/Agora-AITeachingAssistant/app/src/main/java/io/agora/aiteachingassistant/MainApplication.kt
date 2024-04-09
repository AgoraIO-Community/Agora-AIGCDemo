package io.agora.aiteachingassistant

import android.app.Application
import android.content.Context
import android.os.Process
import io.agora.aiteachingassistant.constants.Constants
import io.agora.aiteachingassistant.utils.LogUtils

class MainApplication : Application(), Thread.UncaughtExceptionHandler {
    companion object {
        private const val TAG = Constants.TAG + "-MainApplication";
    }

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        registerUncaughtExceptionHandler()
        //ChatManager.initChatSdk(applicationContext, BuildConfig.CHAT_APP_KEY)
        LogUtils.enableLog(this, true, true)
    }

    private fun registerUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        e.printStackTrace()
        System.exit(1)
        Process.killProcess(Process.myPid())
    }

}
