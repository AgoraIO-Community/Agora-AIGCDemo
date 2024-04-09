# AI口语老师

## 项目介绍

本项目是AI口语老师的demo，通过调用AIGCService SDK和RTC SDK API接口，实现了一个简单的AI口语老师。

## 项目结构
    ```
    ├── assets # 项目中的资源文件，主要包括聊天、提示、润色、翻译、结束等部分的prompt
    ├── aigc
    │   ├── AIManager.kt  # 主要实现项目的相关数据源，包括prompt等信息
    │   ├── AIGCServiceManager.kt  # AIGCService的封装, 包括初始化，start，stop等
    │   ├── RtcManager.kt  # RTC SDK的封装，包括音频采集、音频播放等
    ├── constants
    │   ├── Constants.kt  # 项目中的常量
    │   └── FunctionType.kt  # 项目中的枚举类型
    ├── model # 项目中的数据模型
    ├── ui 
    │   ├── adapter # 项目中的adapter
    │   └── fragment # 项目中的fragment
    │   ├── MainActivity.kt  # 项目中的主界面
    │   └── AITeacherFragment.kt  # 项目中的AI口语老师界面
    ├── utils # 项目中的工具类
    ├── MainApplication # 项目中Application
    ```
## 运行demo

1. 下载代码
2. 打开Android Studio
3. 打开项目
4. 配置项目
    在local.properties中配置以下信息
   ```
   APP_CERTIFICATE=XXXXXXXXXXXXXXXXXXXXXXXX
   APP_ID=XXXXXXXXXXXXXXXXXXXXXXXX
   KEY=XXXXXXXXXXXXXXXX
   ```
5. 运行项目



