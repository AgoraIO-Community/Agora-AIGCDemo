package io.agora.aigc_cloud_demo.ui

import android.Manifest
import android.os.Bundle
import android.text.TextUtils
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.LoadingPopupView
import io.agora.aigc_cloud_demo.AppConfigs
import io.agora.aigc_cloud_demo.R
import io.agora.aigc_cloud_demo.RemoteAigcMessage.AigcMessage
import io.agora.aigc_cloud_demo.agora.RtcManager
import io.agora.aigc_cloud_demo.constants.Constants
import io.agora.aigc_cloud_demo.databinding.ActivityMainBinding
import io.agora.aigc_cloud_demo.model.ConversationData
import io.agora.aigc_cloud_demo.model.HistoryModel
import io.agora.aigc_cloud_demo.model.RtcConfigFeatureParams
import io.agora.aigc_cloud_demo.net.NetworkClient
import io.agora.aigc_cloud_demo.ui.adapter.HistoryListAdapter
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
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), RtcManager.RtcCallback {
    companion object {
        private const val MY_PERMISSIONS_REQUEST_CODE = 123
    }

    private lateinit var binding: ActivityMainBinding
    private var mLoadingPopup: LoadingPopupView? = null
    private var mJoinSuccess = false
    private val mCoroutineScope = CoroutineScope(Dispatchers.IO)

    private var mTaskId = ""
    private var mConversationIndex = 0;

    private var mAiHistoryListAdapter: HistoryListAdapter? = null
    private val mHistoryDataList = mutableListOf<HistoryModel>()
    private val mSdf: SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    private val mConversationDataMap: MutableMap<Int, ConversationData> = mutableMapOf()

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
        LogUtils.d("onPermissionsGranted requestCode:$requestCode perms:$perms")
    }

    fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        LogUtils.d("onPermissionsDenied requestCode:$requestCode perms:$perms")
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

        AppConfigs.initConfigs(this.applicationContext)
    }

    private fun initView() {
        handleOnBackPressed()

        val versionName = applicationContext.packageManager.getPackageInfo(
            applicationContext.packageName,
            0
        ).versionName

        binding.versionTv.text = "Demo Version: ${versionName}"

        val appIdTypes = mutableListOf(
            resources.getString(R.string.app_id_open),
            resources.getString(R.string.app_id_internal)
        )
        binding.appIdSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            appIdTypes
        )
        binding.appIdSpinner.setSelection(0)
        binding.appIdSpinner.onItemSelectedListener = object :
            android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                LogUtils.d("appIdSpinner onItemSelected config: ${AppConfigs.getServerConfigs()[position]}")
                AppConfigs.mCurrentAppId = if (position == 0) {
                    KeyCenter.APP_ID
                } else {
                    KeyCenter.APP_ID_INTERNAL
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
            }
        }

        binding.regionSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            AppConfigs.getServerConfigs().map { it.regionName }
        )
        binding.regionSpinner.setSelection(0)
        binding.regionSpinner.onItemSelectedListener = object :
            android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                LogUtils.d("regionSpinner onItemSelected config: ${AppConfigs.getServerConfigs()[position]}")
                AppConfigs.setCurrentServerConfig(AppConfigs.getServerConfigs()[position])

                updateConfigUI()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
            }
        }

        binding.joinRoomBtn.setOnClickListener {
            enableView(false)
            if (!mJoinSuccess) {
                RtcManager.setChannelId(binding.channelIdEt.text.toString())
                startCloudService()
            } else {
                stopCloudService()
            }
        }

        binding.inChineseCb.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppConfigs.mInLanguage.add("zh-CN")
            } else {
                AppConfigs.mInLanguage.remove("zh-CN")
            }
            if (AppConfigs.mInLanguage.isEmpty()) {
                ToastUtils.showShortToast(this, "input language must be not empty")
                binding.inChineseCb.isChecked = true
                AppConfigs.mInLanguage.add("zh-CN")
            }
        }

        binding.inEnglishCb.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppConfigs.mInLanguage.add("en-US")
            } else {
                AppConfigs.mInLanguage.remove("en-US")
            }
            if (AppConfigs.mInLanguage.isEmpty()) {
                ToastUtils.showShortToast(this, "input language must be not empty")
                binding.inEnglishCb.isChecked = true
                AppConfigs.mInLanguage.add("en-US")
            }
        }

        binding.outChineseCb.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppConfigs.mOutLanguage.add("zh-CN")
            } else {
                AppConfigs.mOutLanguage.remove("zh-CN")
            }
            if (AppConfigs.mOutLanguage.isEmpty()) {
                ToastUtils.showShortToast(this, "output language must be not empty")
                binding.outChineseCb.isChecked = true
                AppConfigs.mOutLanguage.add("zh-CN")
            }
        }

        binding.outEnglishCb.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppConfigs.mOutLanguage.add("en-US")
            } else {
                AppConfigs.mOutLanguage.remove("en-US")
            }
            if (AppConfigs.mOutLanguage.isEmpty()) {
                ToastUtils.showShortToast(this, "output language must be not empty")
                binding.outEnglishCb.isChecked = true
                AppConfigs.mOutLanguage.add("en-US")
            }
        }

        binding.inChineseCb.isChecked = true
        binding.outChineseCb.isChecked = true


        binding.radioQuick.isChecked = AppConfigs.mCurrentSttMode == Constants.STT_MODE_QUICK
        binding.radioNormal.isChecked = AppConfigs.mCurrentSttMode == Constants.STT_MODE_NORMAL

        binding.radioQuick.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppConfigs.mCurrentSttMode = Constants.STT_MODE_QUICK
            }
        }

        binding.radioNormal.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppConfigs.mCurrentSttMode = Constants.STT_MODE_NORMAL
            }
        }

        updateHistoryList()
    }

    private fun updateConfigUI() {
        val isAliyun =
            AppConfigs.getCurrentServerConfig()?.regionIndex == Constants.REGION_INDEX_ALIYUN
        binding.radioQuick.isEnabled =
            isAliyun
        binding.radioNormal.isEnabled =
            isAliyun

        binding.ttsModeSpinner.isEnabled = isAliyun
        binding.llmModeSpinner.isEnabled = isAliyun


        val ttsSelectList = AppConfigs.getCurrentServerConfig()?.ttsSelect
        val ttsModeNameList = mutableListOf<String>()
        var isDefaultSelectIndex = 0
        ttsSelectList?.forEach {
            if (it.isSelected) {
                isDefaultSelectIndex = ttsSelectList.indexOf(it)
            }
            ttsModeNameList.add(it.name)
        }
        binding.ttsModeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            ttsModeNameList
        )
        binding.ttsModeSpinner.setSelection(isDefaultSelectIndex)
        binding.ttsModeSpinner.onItemSelectedListener = object :
            android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                AppConfigs.mCurrentTtsSelect = ttsSelectList?.get(position)?.value ?: ""
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
            }
        }


        val llmSelectList = AppConfigs.getCurrentServerConfig()?.llmSelect
        val llmModeNameList = mutableListOf<String>()
        var isDefaultLlmSelectIndex = 0
        llmSelectList?.forEach {
            if (it.isSelected) {
                isDefaultLlmSelectIndex = llmSelectList.indexOf(it)
            }
            llmModeNameList.add(it.name)
        }

        binding.llmModeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            llmModeNameList
        )

        binding.llmModeSpinner.setSelection(isDefaultLlmSelectIndex)
        binding.llmModeSpinner.onItemSelectedListener = object :
            android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                AppConfigs.mCurrentLlmSelect = llmSelectList?.get(position)?.value ?: ""
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
            }
        }


        val ainsParams = AppConfigs.getRtcConfigs().find {
            it.feature == "ains"
        }

        val ainsPrivateParamList = mutableListOf<String>()
        ainsParams?.params?.forEach {
            ainsPrivateParamList.add(it.type)
        }
        ainsPrivateParamList.add("Disable")

        binding.ainsSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            ainsPrivateParamList
        )

        binding.ainsSpinner.setSelection(0)
        binding.ainsSpinner.onItemSelectedListener = object :
            android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                var currentAinsParams: RtcConfigFeatureParams? = null
                if (position < (ainsParams?.params?.size ?: 0)) {
                    currentAinsParams =
                        ainsParams?.params?.get(position)
                }
                LogUtils.d("ainsSpinner currentAinsParams:${currentAinsParams}")
                AppConfigs.setAinsFeatureParams(currentAinsParams)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
            }
        }
    }

    private fun updateHistoryList() {
        mHistoryDataList.clear()
        if (mAiHistoryListAdapter == null) {
            mAiHistoryListAdapter = HistoryListAdapter(applicationContext, mHistoryDataList)
            binding.aiHistoryList.adapter = mAiHistoryListAdapter
            binding.aiHistoryList.layoutManager = LinearLayoutManager(
                applicationContext
            )
            binding.aiHistoryList.addItemDecoration(HistoryListAdapter.SpacesItemDecoration(10))
        } else {
            mHistoryDataList.clear()
            mAiHistoryListAdapter?.notifyDataSetChanged()
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
        LogUtils.d("startCloudService")
        mCoroutineScope.launch {
            val url =
                "${AppConfigs.getCurrentServerConfig()?.domain}/${AppConfigs.getCurrentServerConfig()?.regionCode}/v1/projects/${AppConfigs.mCurrentAppId}/aigc-workers/local/start"

            val headers = mapOf("Content-Type" to "application/json")

            val bodyJson = JSONObject()
            bodyJson.put("channel_name", RtcManager.getChannelId())
            val userConfJsonArray = JSONArray()
            val userConfJson = JSONObject()
            userConfJson.put("speak_uid", KeyCenter.getUid())
            userConfJson.put("rtc_uid", KeyCenter.getUid() + 1)
            val inLanguagesJSONArray = JSONArray()
            for (language in AppConfigs.mInLanguage) {
                inLanguagesJSONArray.put(language)
            }
            val outLanguagesJSONArray = JSONArray()
            for (language in AppConfigs.mOutLanguage) {
                outLanguagesJSONArray.put(language)
            }
            userConfJson.put("inLanguages", inLanguagesJSONArray)
            userConfJson.put("outLanguages", outLanguagesJSONArray)
            userConfJsonArray.put(userConfJson)
            bodyJson.put("user_conf", userConfJsonArray)

            if (AppConfigs.getCurrentServerConfig()?.regionIndex == Constants.REGION_INDEX_ALIYUN) {
                bodyJson.put("aliYun_mode", AppConfigs.mCurrentSttMode)
                bodyJson.put("tts_select", AppConfigs.mCurrentTtsSelect)
                bodyJson.put("llm_select", AppConfigs.mCurrentLlmSelect)
            }

            NetworkClient.sendHttpsRequest(
                url,
                headers,
                bodyJson.toString(),
                NetworkClient.Method.POST,
                object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        LogUtils.d("startCloudService onFailure: $e")
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        val responseData = response.body?.string()
                        LogUtils.d("startCloudService onResponse: $responseData")
                        mConversationIndex++;
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
        LogUtils.d("stopCloudService mTaskId: $mTaskId")
        if (mTaskId.isEmpty()) {
            return
        }
        mCoroutineScope.launch {
            val url =
                "${AppConfigs.getCurrentServerConfig()?.domain}/${AppConfigs.getCurrentServerConfig()?.regionCode}/v1/projects/${AppConfigs.mCurrentAppId}/aigc-workers/$mTaskId/local"
            val headers = mapOf("Content-Type" to "application/json")

            NetworkClient.sendHttpsRequest(
                url,
                headers,
                "",
                NetworkClient.Method.DELETE,
                object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        LogUtils.d("startCloudService onFailure: $e")
                        mTaskId = ""
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        val responseData = response.body?.string()
                        LogUtils.d("stopCloudService onResponse: $responseData")
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
        RtcManager.initRtcEngine(this, AppConfigs.mCurrentAppId, this, false)
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
            updateHistoryList(
                0L,
                0L,
                mConversationIndex.toString() + "join",
                "Join channel($channel) success",
                "",
                false
            )
            binding.joinRoomBtn.text = resources.getString(R.string.leave)
            binding.channelIdTv.text = getString(R.string.channel_id, RtcManager.getChannelId())
            enableView(true)
        }
    }

    override fun onLeaveChannel(stats: IRtcEngineEventHandler.RtcStats) {
        mJoinSuccess = false
        runOnUiThread {
            updateHistoryList(
                0L,
                0L,
                mConversationIndex.toString() + "leave",
                "Leave channel success",
                "",
                false
            )
            binding.joinRoomBtn.text = resources.getString(R.string.join)
            binding.channelIdTv.text = ""
            enableView(true)
            RtcManager.destroy()
            resetData()
        }
    }

    override fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray?) {
        super.onStreamMessage(uid, streamId, data)
        if (data != null) {
            val aigcMessage = AigcMessage.parseFrom(data)
            LogUtils.d(
                "onStreamMessage aigcMessage: type:${aigcMessage.type} userId:${aigcMessage.userid} roundId:${aigcMessage.roundid} flag:${aigcMessage.flag} timestamp:${aigcMessage.timestamp}  content:${aigcMessage.content}"
            )
            when (aigcMessage.type) {
                Constants.AIGC_MESSAGE_TYPE_STT -> {
                    val sid =
                        mConversationIndex.toString() + aigcMessage.roundid.toString() + "stt"
                    val title = "用户[${aigcMessage.userid}]说："
                    var message = aigcMessage.content
                    if (aigcMessage.flag == 1) {
                        message += Constants.TAG_FINISH
                    }
                    var conversationData = mConversationDataMap[aigcMessage.roundid]
                    if (conversationData == null) {
                        conversationData =
                            ConversationData(aigcMessage.roundid, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                        conversationData.sttStartTimestamp = aigcMessage.timestamp
                        mConversationDataMap[aigcMessage.roundid] = conversationData
                    } else {
                        if (aigcMessage.flag == 0) {
                            if (conversationData.sttStartTimestamp == 0L) {
                                conversationData.sttStartTimestamp = aigcMessage.timestamp
                            }
                        } else if (aigcMessage.flag == 1) {
                            conversationData.sttEndTimestamp = aigcMessage.timestamp
                        }
                    }

                    if (message.isNotEmpty()) {
                        updateHistoryList(
                            conversationData.sttStartTimestamp,
                            conversationData.sttEndTimestamp,
                            sid,
                            title,
                            message,
                            false
                        )
                    }
                }

                Constants.AIGC_MESSAGE_TYPE_LLM -> {
                    val sid =
                        mConversationIndex.toString() + aigcMessage.roundid.toString() + "llm"
                    val title = "AI说："
                    var message = aigcMessage.content
                    if (aigcMessage.flag == 1) {
                        message += Constants.TAG_FINISH
                    }

                    var conversationData = mConversationDataMap[aigcMessage.roundid]
                    if (conversationData == null) {
                        conversationData =
                            ConversationData(aigcMessage.roundid, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                        conversationData.llmStartTimestamp = aigcMessage.timestamp
                        mConversationDataMap[aigcMessage.roundid] = conversationData
                    } else {
                        if (aigcMessage.flag == 0) {
                            if (conversationData.llmStartTimestamp == 0L) {
                                conversationData.llmStartTimestamp = aigcMessage.timestamp
                            }
                        } else if (aigcMessage.flag == 1) {
                            conversationData.llmEndTimestamp = aigcMessage.timestamp
                        }
                    }

                    if (message.isNotEmpty()) {
                        updateHistoryList(
                            conversationData.llmStartTimestamp,
                            conversationData.llmEndTimestamp,
                            sid,
                            title,
                            message,
                            true
                        )
                    }
                }

                Constants.AIGC_MESSAGE_TYPE_TTS -> {
                    val sid =
                        mConversationIndex.toString() + aigcMessage.roundid.toString() + "tts" + aigcMessage.flag
                    val title =
                        if (aigcMessage.flag == 0) "开始播放语音" else if (aigcMessage.flag == 1) "结束播放语音" else "播放语音中"


                    var conversationData = mConversationDataMap[aigcMessage.roundid]
                    if (conversationData == null) {
                        conversationData =
                            ConversationData(aigcMessage.roundid, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                        conversationData.ttsStartTimestamp = aigcMessage.timestamp
                        mConversationDataMap[aigcMessage.roundid] = conversationData
                    } else {
                        if (aigcMessage.flag == 0) {
                            if (conversationData.ttsStartTimestamp == 0L) {
                                conversationData.ttsStartTimestamp = aigcMessage.timestamp
                            }
                        } else if (aigcMessage.flag == 1) {
                            conversationData.ttsEndTimestamp = aigcMessage.timestamp
                        }
                    }

                    var message = ""
                    if (aigcMessage.flag == 0) {
                        message = buildString {
                            append("(")
                            append("STT耗时：")
                            append(
                                conversationData!!.sttEndTimestamp - conversationData!!.conversationStartTimestamp
                            )
                            append("ms;")
                            append("LLM第一个返回耗时：")
                            append(
                                conversationData!!.llmStartTimestamp - conversationData!!.sttEndTimestamp
                            )
                            append("ms;")
                            append("TTS耗时：")
                            append(
                                conversationData!!.ttsStartTimestamp - conversationData!!.llmStartTimestamp
                            )
                            append("ms;")
                            append("agent总体耗时：")
                            append(
                                conversationData!!.ttsStartTimestamp - conversationData!!.conversationStartTimestamp
                            )
                            append("ms;")
                            append(")")
                        }
                    }

                    if (aigcMessage.flag == 0) {
                        updateHistoryList(
                            conversationData.ttsStartTimestamp,
                            0,
                            sid,
                            title,
                            message,
                            false
                        )
                    } else if (aigcMessage.flag == 1) {
                        updateHistoryList(
                            0,
                            conversationData.ttsEndTimestamp,
                            sid,
                            title,
                            message,
                            true
                        )
                    }
                }

                Constants.AIGC_MESSAGE_TYPE_CONVERSATION -> {
                    val sid =
                        mConversationIndex.toString() + aigcMessage.roundid.toString() + "conversation" + aigcMessage.flag
                    val title =
                        if (aigcMessage.flag == 0) "会话开始" else if (aigcMessage.flag == 1) "会话结束" else "会话中"

                    var conversationData = mConversationDataMap[aigcMessage.roundid]
                    if (conversationData == null) {
                        conversationData =
                            ConversationData(aigcMessage.roundid, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                        conversationData.conversationStartTimestamp = aigcMessage.timestamp
                        mConversationDataMap[aigcMessage.roundid] = conversationData
                    } else {
                        if (aigcMessage.flag == 0) {
                            if (conversationData.conversationStartTimestamp == 0L) {
                                conversationData.conversationStartTimestamp = aigcMessage.timestamp
                            }
                        } else if (aigcMessage.flag == 1) {
                            conversationData.conversationEndTimestamp = aigcMessage.timestamp
                        }
                    }

                    if (aigcMessage.flag == 0) {
                        updateHistoryList(
                            conversationData.conversationStartTimestamp,
                            0,
                            sid,
                            title,
                            "",
                            false
                        )
                    } else {
                        updateHistoryList(
                            0,
                            conversationData.conversationEndTimestamp,
                            sid,
                            title,
                            "",
                            true
                        )

                    }
                }
            }
        }
    }

    private fun exit() {
        LogUtils.destroy()
        finishAffinity()
        finish()
        exitProcess(0)
    }


    @Synchronized
    private fun updateHistoryList(
        startTimestamp: Long,
        endTimestamp: Long,
        sid: String,
        title: String,
        message: String,
        isAppend: Boolean
    ) {
        runOnUiThread {
            var startTimestampStr = ""
            if (startTimestamp != 0L) {
                startTimestampStr = mSdf.format(startTimestamp)
            }
            var endTimestampStr = ""
            if (endTimestamp != 0L) {
                endTimestampStr = mSdf.format(endTimestamp)
            }
            var isNewLineMessage = true
            var updateIndex = -1
            if (!TextUtils.isEmpty(sid)) {
                for (historyModel in mHistoryDataList) {
                    updateIndex++
                    if (sid == historyModel.sid) {
                        if (isAppend) {
                            historyModel.message += message
                        } else {
                            historyModel.message = message
                        }
                        historyModel.title = title
                        historyModel.startTimestamp = startTimestampStr
                        historyModel.endTimestamp = endTimestampStr
                        isNewLineMessage = false
                        break
                    }
                }
            }
            if (isNewLineMessage) {
                val aiHistoryModel = HistoryModel()
                aiHistoryModel.startTimestamp = startTimestampStr
                aiHistoryModel.endTimestamp = endTimestampStr
                aiHistoryModel.title = title
                aiHistoryModel.sid = sid
                aiHistoryModel.message = message
                mHistoryDataList.add(aiHistoryModel)
                if (null != mAiHistoryListAdapter) {
                    mAiHistoryListAdapter?.notifyItemInserted(mHistoryDataList.size - 1)
                    binding.aiHistoryList.scrollToPosition(
                        mAiHistoryListAdapter?.getDataList()?.size?.minus(1) ?: 0
                    )
                }
            } else {
                if (null != mAiHistoryListAdapter) {
                    mAiHistoryListAdapter?.notifyItemChanged(updateIndex)
                    binding.aiHistoryList.scrollToPosition(
                        mAiHistoryListAdapter?.getDataList()?.size?.minus(1) ?: 0
                    )
                }
            }
        }
    }

    private fun resetData() {
        mConversationDataMap.clear()
    }
}





