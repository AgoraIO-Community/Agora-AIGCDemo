package io.agora.aiteachingassistant.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.fastjson.JSON
import com.lxj.xpopup.XPopup
import io.agora.aiteachingassistant.BuildConfig
import io.agora.aiteachingassistant.R
import io.agora.aiteachingassistant.aigc.AIGCServiceManager
import io.agora.aiteachingassistant.aigc.AIManager
import io.agora.aiteachingassistant.constants.Constants
import io.agora.aiteachingassistant.databinding.ActivityMainBinding
import io.agora.aiteachingassistant.model.TeacherLevel
import io.agora.aiteachingassistant.ui.adapter.ItemAdapter
import io.agora.aiteachingassistant.utils.EncryptUtils
import io.agora.aiteachingassistant.utils.LogUtils
import io.agora.aiteachingassistant.utils.ToastUtils
import io.agora.aiteachingassistant.utils.Utils
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private val TAG: String = Constants.TAG + "-MainActivity"
    private lateinit var binding: ActivityMainBinding
    private val MY_PERMISSIONS_REQUEST_CODE = 123
    private var mTeacherLevelList: MutableList<TeacherLevel>? = null


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
    }

    private fun checkPermissions() {
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
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
    }

    fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        // 权限被拒绝，显示一个提示信息
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            // 如果权限被永久拒绝，可以显示一个对话框引导用户去应用设置页面手动授权
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    private fun initData() {
        val teacherLevels = EncryptUtils.decryptByAes(
            Utils.readAssetJsonArray(
                this,
                Constants.ASSETS_TEACHER_LEVEL
            )
        )
        if (teacherLevels != null) {
            if (teacherLevels.isNotEmpty()) {
                mTeacherLevelList = JSON.parseArray(teacherLevels, TeacherLevel::class.java)
                LogUtils.d(TAG, "teacherLevelList : $mTeacherLevelList")
            }
        }
    }

    private fun initView() {
        handleOnBackPressed()
        binding.gpt35Tv.text = Constants.LLM_MODEL_GPT_35
        binding.gpt4Tv.text = Constants.LLM_MODEL_GPT_4

        binding.versionTv.text =
            String.format(resources.getString(R.string.version), BuildConfig.VERSION_NAME)

        var teacherLevelLabelList = arrayOf("")
        mTeacherLevelList?.let {
            teacherLevelLabelList = Array(it.size) { "" }
            for (i in it.indices) {
                teacherLevelLabelList[i] = it[i].level
            }
        }

        binding.gpt35TeacherLevelRv.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                false
            )
        val gpt35TeacherLevelAdapter = ItemAdapter(teacherLevelLabelList)
        gpt35TeacherLevelAdapter.setOnItemClickListener(object :
            ItemAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                if (!isNetworkConnected()) {
                    return
                }
                mTeacherLevelList?.get(position)?.let {
                    LogUtils.d(
                        "gpt 35 select teacher level : ${mTeacherLevelList?.get(position)}"
                    )
                    startChat(
                        it,
                        position,
                        Constants.LLM_MODEL_GPT_35
                    )
                }
            }
        })
        binding.gpt35TeacherLevelRv.adapter = gpt35TeacherLevelAdapter
//        binding.teacherLevelRv.addItemDecoration(
//            ItemAdapter.MyItemDecoration(
//                resources.getDimensionPixelSize(
//                    R.dimen.item_space
//                )
//            )
//        )

        binding.gpt4TeacherLevelRv.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                false
            )
        val gpt4TeacherLevelAdapter = ItemAdapter(teacherLevelLabelList)
        gpt4TeacherLevelAdapter.setOnItemClickListener(object :
            ItemAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                if (!isNetworkConnected()) {
                    return
                }
                mTeacherLevelList?.get(position)?.let {
                    LogUtils.d(
                        "gpt 35 select teacher level : ${mTeacherLevelList?.get(position)}"
                    )
                    startChat(
                        it,
                        position,
                        Constants.LLM_MODEL_GPT_4
                    )
                }
            }
        })
        binding.gpt4TeacherLevelRv.adapter = gpt4TeacherLevelAdapter
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
        LogUtils.destroy()
        AIGCServiceManager.destroy()
    }

    private fun startChat(
        teacherLevel: TeacherLevel,
        level: Int,
        model: String,
    ) {
        AIManager.initialize(
            teacherLevel,
            EncryptUtils.decryptByAes(Utils.readAssetJsonArray(this, Constants.ASSETS_TIP_PROMPT)),
            EncryptUtils.decryptByAes(
                Utils.readAssetJsonArray(
                    this,
                    Constants.ASSETS_TRANSLATE_PROMPT
                )
            ),
            EncryptUtils.decryptByAes(
                Utils.readAssetJsonArray(
                    this,
                    Constants.ASSETS_POLISH_PROMPT
                )
            ),
            EncryptUtils.decryptByAes(Utils.readAssetJsonArray(this, Constants.ASSETS_END_PROMPT)),
            level,
            model
        )

        startActivity(
            Intent(
                this@MainActivity,
                AITeachingAssistantActivity::class.java
            ).apply {
                //putExtra(Constants.EXTRA_KEY_TEACHER_LEVEL_PROMPT, teacherLevelPrompt)
            })

    }

    private fun isNetworkConnected(): Boolean {
        val isConnect = Utils.isNetworkConnected(this)
        if (!isConnect) {
            LogUtils.d("Network is not connected")
            ToastUtils.showLongToast(this, "请连接网络!")
        }
        return isConnect
    }

    private fun exit() {
        finishAffinity()
        finish()
        exitProcess(0)
    }
}





