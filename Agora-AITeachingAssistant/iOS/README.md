# AIGC SDK Demo

## 1 示例代码项目结构
- SDK目录存放了AGGC SDK
- ViewController.swift 是项目一级页面。
- LanguageAssistantLevelSelectedViewController.swift 是项目的二级页面，供选择不同的等级。
- LanguageAssistantViewController.swift 业务处理主页面。
- PromptGenerator.swift 是对prompt进行处理的类，用于产生不同功能的prompt。
- AIGCManager.swift 是对AIGCSDK的包装。
- RtcManager.swift 是对RTC SDK的包装。
- Config.swift 配置声网鉴权数据。
- Podfile 库依赖。
## 2 跑通示例代码
  1. podfile文件所在目录：pod install
  2. Config.swift填写app id 和 cer
  3. 打开工程文件AIGC-Examples.xcworkspace
