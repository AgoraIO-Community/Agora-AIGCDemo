package io.agora.aigc_cloud_demo.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.LoadingPopupView
import io.agora.aigc_cloud_demo.R
import io.agora.aigc_cloud_demo.agora.RtcManager
import io.agora.aigc_cloud_demo.constants.Constants
import io.agora.aigc_cloud_demo.databinding.ActivityMainBinding
import io.agora.aigc_cloud_demo.net.NetworkClient
import io.agora.aigc_cloud_demo.utils.KeyCenter
import io.agora.aigc_cloud_demo.utils.LogUtils
import io.agora.aigc_cloud_demo.utils.ToastUtils
import io.agora.aigc_cloud_demo.utils.Utils
import io.agora.rtc2.IRtcEngineEventHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), RtcManager.RtcCallback {
    private val TAG: String = Constants.TAG + "-MainActivity"
    private lateinit var binding: ActivityMainBinding
    private val MY_PERMISSIONS_REQUEST_CODE = 123
    private var mLoadingPopup: LoadingPopupView? = null
    private var mJoinSuccess = false
    private val mCoroutineScope = CoroutineScope(Dispatchers.IO)
    private var mTaskId = ""
    private val mInLanguage = mutableListOf<String>()
    private val mOutLanguage = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPermissions()
        initData()
        initView()
    }

    override fun onResume() {
        super.onResume()
        isNetworkConnected();
        initParams()
    }

    private fun checkPermissions() {
        val permissions =
            arrayOf(Manifest.permission.RECORD_AUDIO)
        if (EasyPermissions.hasPermissions(this, *permissions)) {
            // 已经获取到权限，执行相应的操作
        } else {
            EasyPermissions.requestPermissions(
                this,
                "需要录音权限",
                MY_PERMISSIONS_REQUEST_CODE,
                *permissions
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        // 权限被授予，执行相应的操作
        LogUtils.d(TAG, "onPermissionsGranted requestCode:$requestCode perms:$perms")
    }

    fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        LogUtils.d(TAG, "onPermissionsDenied requestCode:$requestCode perms:$perms")
        // 权限被拒绝，显示一个提示信息
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            // 如果权限被永久拒绝，可以显示一个对话框引导用户去应用设置页面手动授权
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    private fun initData() {
        mLoadingPopup = XPopup.Builder(this@MainActivity)
            .hasBlurBg(true)
            .asLoading("正在加载中")
    }

    private fun initView() {
        handleOnBackPressed()

        val versionName = applicationContext.packageManager.getPackageInfo(
            applicationContext.packageName,
            0
        ).versionName

        binding.versionTv.text = "Demo Version: ${versionName}"

        binding.joinRoomBtn.setOnClickListener {
            enableView(false)
            if (!mJoinSuccess) {
                startCloudService()
            } else {
                stopCloudService()
            }
        }

        binding.inChineseCb.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mInLanguage.add("zh-CN")
            } else {
                mInLanguage.remove("zh-CN")
            }
        }

        binding.inEnglishCb.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mInLanguage.add("en-US")
            } else {
                mInLanguage.remove("en-US")
            }
        }

        binding.outChineseCb.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mOutLanguage.add("zh-CN")
            } else {
                mOutLanguage.remove("zh-CN")
            }
        }

        binding.outEnglishCb.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mOutLanguage.add("en-US")
            } else {
                mOutLanguage.remove("en-US")
            }
        }

    }

    private fun enableView(enable: Boolean) {
        binding.joinRoomBtn.isEnabled = enable
    }

    private fun handleOnBackPressed() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val xPopup = XPopup.Builder(this@MainActivity)
                    .asConfirm("退出", "确认退出程序", {
                        exit()
                    }, {})
                xPopup.show()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun isNetworkConnected(): Boolean {
        val isConnect = Utils.isNetworkConnected(this)
        if (!isConnect) {
            LogUtils.d("Network is not connected")
            ToastUtils.showLongToast(this, "请连接网络!")
        }
        return isConnect
    }

    private fun initParams() {
    }


    private fun startCloudService() {
        LogUtils.d(TAG, "startCloudService")
        mCoroutineScope.launch {
            val url =
                "http://aigc-http.la3.agoralab.co/NA/v1/projects/${KeyCenter.APP_ID}/aigc-workers/local/start"

            val headers = mapOf("Content-Type" to "application/json")

            val bodyJson = JSONObject()
            bodyJson.put("channel_name", RtcManager.getChannelId())
            val userConfJsonArray = JSONArray()
            val userConfJson = JSONObject()
            userConfJson.put("speak_uid", KeyCenter.getUid())
            userConfJson.put("rtc_uid", KeyCenter.getUid() + 1)
            val inLanguagesJSONArray = JSONArray()
            for (language in mInLanguage) {
                inLanguagesJSONArray.put(language)
            }
            val outLanguagesJSONArray = JSONArray()
            for (language in mOutLanguage) {
                outLanguagesJSONArray.put(language)
            }
            userConfJson.put("inLanguages", inLanguagesJSONArray)
            userConfJson.put("outLanguages", outLanguagesJSONArray)
            userConfJsonArray.put(userConfJson)
            bodyJson.put("user_conf", userConfJsonArray)

            NetworkClient.sendHttpsRequest(
                url,
                headers,
                bodyJson.toString(),
                NetworkClient.Method.POST,
                object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        LogUtils.d(TAG, "startCloudService onFailure: $e")
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        val responseData = response.body?.string()
                        LogUtils.d(TAG, "startCloudService onResponse: $responseData")
                        val responseJson = responseData?.let { JSONObject(it) }
                        val code = responseJson?.getInt("code")
                        if (code == 0) {
                            val dataJson = responseJson.getJSONObject("data")
                            mTaskId = dataJson.getString("id") ?: ""
                            joinRoom()
                        } else {
                            runOnUiThread {
                                val message = responseJson?.getString("message")
                                    ?: "Start cloud service failed"
                                ToastUtils.showLongToast(this@MainActivity, message)
                                enableView(true)
                            }
                        }
                    }
                })
        }
    }

    private fun stopCloudService() {
        LogUtils.d(TAG, "stopCloudService mTaskId: $mTaskId")
        if (mTaskId.isEmpty()) {
            return
        }
        mCoroutineScope.launch {
            val url =
                "http://aigc-http.la3.agoralab.co/NA/v1/projects/${KeyCenter.APP_ID}/aigc-workers/$mTaskId/local"
            val headers = mapOf("Content-Type" to "application/json")

            NetworkClient.sendHttpsRequest(
                url,
                headers,
                "",
                NetworkClient.Method.DELETE,
                object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        LogUtils.d(TAG, "startCloudService onFailure: $e")
                        mTaskId = ""
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        val responseData = response.body?.string()
                        LogUtils.d(TAG, "stopCloudService onResponse: $responseData")
                        val responseJson = responseData?.let { JSONObject(it) }
                        val code = responseJson?.getInt("code")
                        if (code == 0) {
                            mTaskId = ""
                            runOnUiThread { leaveRoom() }
                        } else {
                            mTaskId = ""
                            runOnUiThread {
                                val message = responseJson?.getString("message")
                                    ?: "stop cloud service failed"
                                ToastUtils.showLongToast(this@MainActivity, message)
                                enableView(true)
                                leaveRoom()
                            }
                        }
                    }
                })
        }
    }


    private fun joinRoom() {
        RtcManager.initRtcEngine(this, this, false)
    }

    private fun leaveRoom() {
        if (mJoinSuccess) {
            RtcManager.leaveChannel()
        } else {
            RtcManager.destroy()
        }
    }


    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        mJoinSuccess = true
        runOnUiThread {
            ToastUtils.showLongToast(this, "Join channel success")
            binding.joinRoomBtn.text = resources.getString(R.string.leave)
            enableView(true)
        }
    }

    override fun onLeaveChannel(stats: IRtcEngineEventHandler.RtcStats) {
        mJoinSuccess = false
        runOnUiThread {
            ToastUtils.showLongToast(this, "Leave channel success")
            binding.joinRoomBtn.text = resources.getString(R.string.join)
            enableView(true)
            RtcManager.destroy()
        }
    }

    private fun exit() {
        LogUtils.destroy()
        finishAffinity()
        finish()
        exitProcess(0)
    }
}





