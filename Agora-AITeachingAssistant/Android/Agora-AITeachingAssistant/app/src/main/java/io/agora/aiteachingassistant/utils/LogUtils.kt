package io.agora.aiteachingassistant.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import io.agora.aiteachingassistant.constants.Constants
import io.agora.logging.ConsoleLogger
import io.agora.logging.FileLogger
import io.agora.logging.LogManager
import io.agora.logging.Logger

object LogUtils {
    private var isEnableLog = false
    private var saveLogFile = false
    private var logPath: String? = null
    private val LOGGERS: MutableList<Logger> = ArrayList(3)

    fun enableLog(context: Context, enableLog: Boolean, saveLogFile: Boolean) {
        this.isEnableLog = enableLog
        this.saveLogFile = saveLogFile
        if (this.isEnableLog) {
            try {
                initLog(context)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(Constants.TAG, "initLog error:" + e.message)
            }
        }
    }

    fun setLogPath(logPath: String?) {
        this.logPath = logPath
    }

    private fun initLog(context: Context) {
        if (TextUtils.isEmpty(logPath)) {
            logPath = context.getExternalFilesDir(null)!!.path
        }
        //LOGGERS.add(ConsoleLogger())
//        if (this.saveLogFile) {
//            LOGGERS.add(FileLogger(logPath, Constants.LOG_FILE_NAME, (1024 * 1024).toLong(), 2))
//        }
//        for (logger in LOGGERS) {
//            LogManager.instance().addLogger(logger)
//        }
    }

    fun destroy() {
        for (logger in LOGGERS) {
            LogManager.instance().removeLogger(logger)
        }
        LOGGERS.clear()
    }

    fun d(msg: String?) {
        if (isEnableLog) {
            LogManager.instance().debug(Constants.TAG, msg)
        }
    }

    fun d(tag: String, msg: String?) {
        if (isEnableLog) {
            LogManager.instance().debug(tag, msg)
        }
    }

    fun e(msg: String?) {
        if (isEnableLog) {
            LogManager.instance().error(Constants.TAG, msg)
        }
    }

    fun e(tag: String, msg: String?) {
        if (isEnableLog) {
            LogManager.instance().error(tag, msg)
        }
    }

    fun i(msg: String?) {
        if (isEnableLog) {
            LogManager.instance().info(Constants.TAG, msg)
        }
    }

    fun i(tag: String, msg: String?) {
        if (isEnableLog) {
            LogManager.instance().info(tag, msg)
        }
    }
}
